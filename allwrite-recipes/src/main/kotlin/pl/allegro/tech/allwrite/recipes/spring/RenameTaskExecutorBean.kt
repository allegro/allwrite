package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Statement
import org.openrewrite.java.tree.TypeUtils
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.spring.util.ANNOTATION_QUALIFIER
import pl.allegro.tech.allwrite.recipes.spring.util.Variable
import pl.allegro.tech.allwrite.recipes.spring.util.findArguments
import pl.allegro.tech.allwrite.recipes.spring.util.getAutowiredFields
import pl.allegro.tech.allwrite.recipes.spring.util.getAutowiringConstructor
import pl.allegro.tech.allwrite.recipes.spring.util.getBeanMethodDeclarations
import pl.allegro.tech.allwrite.recipes.spring.util.getSpringComponentAnnotation
import pl.allegro.tech.allwrite.recipes.spring.util.hasConfigurationAnnotation

public class RenameTaskExecutorBean :
    AllwriteScanningRecipe<RenameTaskExecutorBean.Context>(
        displayName = "Rename task executor bean",
        description = "Rename task executor bean, as required by [Spring Boot 3.5](https://github.com/spring-projects/spring-boot/wiki/" +
            "Spring-Boot-3.5-Release-Notes#auto-configured-taskexecutor-names).",
        visibility = RecipeVisibility.INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> =
        listOf(
            "spring-context-6",
            "spring-core-6",
            "spring-beans-6",
            "jakarta.annotation-api-2",
            "jakarta.inject-api-2",
        )

    public data class Context(
        var hasCustomTaskExecutorBean: Boolean = false,
        val variables: MutableMap<J.VariableDeclarations, List<Variable>> = HashMap(),
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(context: Context): TreeVisitor<*, ExecutionContext> = Scanner(context)

    private class Scanner(
        val context: Context,
    ) : TreeVisitor<Tree, ExecutionContext>() {
        override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
            FindCustomTaskExecutorBean(context).visit(tree, p)
            FindTaskExecutorInjectingPoints(context.variables).visit(tree, p)
            return tree
        }

        private class FindCustomTaskExecutorBean(
            val acc: Context,
        ) : JavaIsoVisitor<ExecutionContext>() {

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                // if the class is @Component
                val metadata = classDecl.getSpringComponentAnnotation()
                if (metadata?.name == TASK_EXECUTOR) {
                    acc.hasCustomTaskExecutorBean = true
                }

                // if the class is @Configuration, inspect return types of @Bean method
                if (classDecl.hasConfigurationAnnotation()) {
                    classDecl.getBeanMethodDeclarations().forEach { beanMethod ->
                        if (beanMethod.method.methodType?.isTaskExecutor() == true) {
                            acc.hasCustomTaskExecutorBean = true
                        }
                    }
                }

                return super.visitClassDeclaration(classDecl, p)
            }
        }

        // Finds VariableDeclarations of where beans of type TaskExecutor and a qualified name `taskExecutor` are injected
        private class FindTaskExecutorInjectingPoints(
            val data: MutableMap<J.VariableDeclarations, List<Variable>>,
        ) : JavaIsoVisitor<ExecutionContext>() {

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                val result = super.visitClassDeclaration(classDecl, p)
                val injectedVariables = mutableListOf<Variable>()

                // class is @Configuration, find @Bean methods which have TaskExecutor parameter
                if (classDecl.hasConfigurationAnnotation()) {
                    classDecl.getBeanMethodDeclarations().map { it.method }.forEach { beanMethod ->
                        beanMethod.getTaskExecutorParameters().forEach(injectedVariables::add)
                    }
                }

                // class is Spring @Component, find @Autowired fields and constructor arguments of TaskExecutor type
                if (classDecl.getSpringComponentAnnotation() != null) {
                    val fields = classDecl.getAutowiredFields().filter { TASK_EXECUTOR == it.name && it.variable.type.isTaskExecutor() }
                    fields.forEach(injectedVariables::add)

                    val constructor = classDecl.getAutowiringConstructor()
                    constructor?.method?.getTaskExecutorParameters()?.forEach(injectedVariables::add)
                }

                injectedVariables.groupBy { it.declaration }.forEach { (dec, vars) -> data.put(dec, vars) }
                return result
            }
        }
    }

    /**
     * Iterate over [Statement]s collected in [FindTaskExecutorInjectingPoints] and qualify the variables
     * with `applicationTaskExecutor` name.
     * If the there is a custom `taskExecutor` bean in the [context], this visitor is noop.
     *
     * TODO: this recipe disregards submodules - if at least one of the modules defines custom `taskExecutor` bean, the visitor is noop
     *   for the whole project
     */
    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> =
        object : JavaIsoVisitor<ExecutionContext>() {

            private val targetVariables = context.variables

            override fun visitCompilationUnit(cu: J.CompilationUnit, p: ExecutionContext): J.CompilationUnit {
                if (context.hasCustomTaskExecutorBean) return cu
                return super.visitCompilationUnit(cu, p)
            }

            override fun visitStatement(statement: Statement, p: ExecutionContext): Statement {
                var result = super.visitStatement(statement, p)
                val parent = cursor.parent ?: return result
                val replacement = targetVariables[statement] ?: return result

                maybeAddImport(ANNOTATION_QUALIFIER)
                replacement.forEach { v ->
                    result = QualifyVariable(v, APP_TASK_EXECUTOR).visit(result, p, parent) as Statement
                }
                return result
            }
        }

    private companion object {

        const val TASK_EXECUTOR = "taskExecutor"
        const val APP_TASK_EXECUTOR = "applicationTaskExecutor"

        fun JavaType?.isTaskExecutor() = TypeUtils.isAssignableTo("org.springframework.core.task.TaskExecutor", this)
        fun J.MethodDeclaration.getTaskExecutorParameters() = findArguments(TASK_EXECUTOR).filter { it.variable.type.isTaskExecutor() }
    }
}
