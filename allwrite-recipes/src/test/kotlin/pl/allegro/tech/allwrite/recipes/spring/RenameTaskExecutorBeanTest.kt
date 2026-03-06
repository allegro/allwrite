package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.java.Assertions.srcMainJava
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.common.util.withRecipeClasspath
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin

class RenameTaskExecutorBeanTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(RenameTaskExecutorBean())
            .withRecipeClasspath()
    }

    @Nested
    inner class BeanMethodTests {
        @Test
        fun `should qualify parameter when matches name and type in @Bean method in @Configuration class`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.beans.factory.annotation.Qualifier;

                    @Configuration
                    class MyConfig {

                        @Bean
                        String myString(TaskExecutor taskExecutor, @Qualifier("taskExecutor") TaskExecutor taskExecutor2) {
                            return taskExecutor.toString() + taskExecutor2.toString();
                        }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.beans.factory.annotation.Qualifier;

                    @Configuration
                    class MyConfig {

                        @Bean
                        String myString(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor, @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor2) {
                            return taskExecutor.toString() + taskExecutor2.toString();
                        }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Configuration
                    class MyConfigKotlin {
                        @Bean
                        fun myString(taskExecutor: TaskExecutor, @Qualifier("taskExecutor") taskExecutor2: TaskExecutor) = taskExecutor.toString() + taskExecutor2.toString();
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Configuration
                    class MyConfigKotlin {
                        @Bean
                        fun myString(@Qualifier("applicationTaskExecutor") taskExecutor: TaskExecutor, @Qualifier("applicationTaskExecutor") taskExecutor2: TaskExecutor) = taskExecutor.toString() + taskExecutor2.toString();
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should qualify parameter when matches and type is a subtype in @Bean method in @Configuration class`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;

                    @Configuration
                    class MyConfig {
                        @Bean
                        String myString(ConcurrentTaskExecutor taskExecutor) { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.context.annotation.Bean;

                    @Configuration
                    class MyConfig {
                        @Bean
                        String myString(@Qualifier("applicationTaskExecutor") ConcurrentTaskExecutor taskExecutor) { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin {
                        @Bean
                        fun myString(taskExecutor : ConcurrentTaskExecutor) = taskExecutor.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin {
                        @Bean
                        fun myString(@Qualifier("applicationTaskExecutor") taskExecutor : ConcurrentTaskExecutor) = taskExecutor.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should qualify taskExecutor in a nested Configuration`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;

                    @Configuration
                    class MyConfig {

                        @Configuration
                        class NestedConfig {
                            @Bean
                            String myString(ConcurrentTaskExecutor taskExecutor) { return taskExecutor.toString(); }
                        }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.context.annotation.Bean;

                    @Configuration
                    class MyConfig {

                        @Configuration
                        class NestedConfig {
                            @Bean
                            String myString(@Qualifier("applicationTaskExecutor") ConcurrentTaskExecutor taskExecutor) { return taskExecutor.toString(); }
                        }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin {
                      @Configuration
                      class NestedConfig {
                        @Bean
                        fun myString(taskExecutor: TaskExecutor) = taskExecutor.toString()
                      }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin {
                      @Configuration
                      class NestedConfig {
                        @Bean
                        fun myString(@Qualifier("applicationTaskExecutor") taskExecutor: TaskExecutor) = taskExecutor.toString()
                      }
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not modify parameters outside of @Configuration class`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Bean;

                    class MyConfig {
                      @Bean
                      String myString(TaskExecutor taskExecutor) { return taskExecutor.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Bean

                    class MyConfigKotlin {
                      @Bean
                      fun myString(taskExecutor: TaskExecutor) = taskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not modify parameters outside of @Bean method`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;

                    @Configuration
                    class MyConfig {
                      String myString(TaskExecutor taskExecutor) { return taskExecutor.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.beans.factory.BeanFactory

                    @Configuration
                    class MyConfigKotlin {
                      fun myString(taskExecutor: TaskExecutor) = taskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not modify parameters with a different qualified name`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;
                    import jakarta.inject.Named;
                    import org.springframework.beans.factory.annotation.Qualifier;

                    @Configuration
                    class MyConfig {
                      @Bean
                      String myString1(@Qualifier("custom") TaskExecutor taskExecutor) { return taskExecutor.toString(); }
                      @Bean
                      String myString2(@Named("custom") TaskExecutor taskExecutor) { return taskExecutor.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean
                    import jakarta.inject.Named
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Configuration
                    class MyConfigKotlin {
                      fun myString(@Qualifier("custom") taskExecutor: TaskExecutor) = taskExecutor.toString()
                      fun myString2(@Named("custom") taskExecutor: TaskExecutor) = taskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not modify parameters with a different name`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;

                    @Configuration
                    class MyConfig {
                      @Bean
                      String myString(TaskExecutor customTaskExecutor) { return customTaskExecutor.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin {
                      @Bean
                      fun myString(customTaskExecutor: TaskExecutor) = customTaskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not modify parameters with a different type`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;

                    @Configuration
                    class MyConfig {
                      @Bean
                      String myString(Object taskExecutor) { return taskExecutor.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin {
                      @Bean
                      fun myString(taskExecutor: Any) = customTaskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not rename identifiers outside of @Bean method`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.core.task.SimpleAsyncTaskExecutor;

                    @Configuration
                    class MyConfig {
                      private TaskExecutor taskExecutor;
                      @Bean
                      String myString(Object o) {
                        var taskExecutor = new SimpleAsyncTaskExecutor();
                        this.taskExecutor = taskExecutor;
                        return o.toString();
                      }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.context.annotation.Bean
                    import org.springframework.core.task.SimpleAsyncTaskExecutor

                    @Configuration
                    class MyConfigKotlin {

                      private lateinit var taskExecutor: SimpleAsyncTaskExecutor
                      @Bean
                      fun myString(a: Any) {
                        val taskExecutor = SimpleAsyncTaskExecutor()
                        this.taskExecutor = taskExecutor
                        return a.toString()
                      }
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Nested
    inner class AutowiredFieldsTests {
        @Test
        fun `should qualify autowired field when it matches name and type`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                        @Autowired
                        private TaskExecutor taskExecutor;
                        public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                        @Autowired
                        @Qualifier("applicationTaskExecutor")
                        private TaskExecutor taskExecutor;
                        public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent {
                        @Autowired
                        private lateinit var taskExecutor: TaskExecutor
                        public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent {
                        @Autowired
                        @Qualifier("applicationTaskExecutor")
                        private lateinit var taskExecutor: TaskExecutor
                        public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                ),
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import jakarta.inject.Inject;

                    @Component
                    class MyComponentInject {
                        @Inject
                        private TaskExecutor taskExecutor;
                        public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import jakarta.inject.Inject;

                    @Component
                    class MyComponentInject {
                        @Inject
                        @Qualifier("applicationTaskExecutor")
                        private TaskExecutor taskExecutor;
                        public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import jakarta.inject.Inject

                    @Component
                    class MyComponentInject {
                        @Inject
                        private lateinit var taskExecutor: TaskExecutor
                        public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import jakarta.inject.Inject

                    @Component
                    class MyComponentInject {
                        @Inject
                        @Qualifier("applicationTaskExecutor")
                        private lateinit var taskExecutor: TaskExecutor
                        public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                ),
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import jakarta.annotation.Resource;

                    @Component
                    class MyComponentResource {
                        @Resource
                        private TaskExecutor taskExecutor;
                        public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import jakarta.annotation.Resource;

                    @Component
                    class MyComponentResource {
                        @Resource(name = "applicationTaskExecutor")
                        private TaskExecutor taskExecutor;
                        public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import jakarta.annotation.Resource

                    @Component
                    class MyComponentResource {
                        @Resource
                        private lateinit var taskExecutor: TaskExecutor
                        public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import jakarta.annotation.Resource

                    @Component
                    class MyComponentResource {
                        @Resource(name = "applicationTaskExecutor")
                        private lateinit var taskExecutor: TaskExecutor
                        public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should qualify qualifier of the field when it matches qualified name and type`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import jakarta.inject.Named;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.annotation.Resource;

                    @Component
                    class MyComponent {
                      @Autowired @Named("taskExecutor")
                      private TaskExecutor taskExecutor1;

                      @Autowired @Qualifier("taskExecutor")
                      private TaskExecutor taskExecutor2;

                      @Resource(name = "taskExecutor")
                      private TaskExecutor taskExecutor3;

                      public String test() { return taskExecutor1.toString() + taskExecutor2.toString() + taskExecutor3.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import jakarta.inject.Named;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.annotation.Resource;

                    @Component
                    class MyComponent {
                      @Autowired @Named("applicationTaskExecutor")
                      private TaskExecutor taskExecutor1;

                      @Autowired @Qualifier("applicationTaskExecutor")
                      private TaskExecutor taskExecutor2;

                      @Resource(name = "applicationTaskExecutor")
                      private TaskExecutor taskExecutor3;

                      public String test() { return taskExecutor1.toString() + taskExecutor2.toString() + taskExecutor3.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired
                    import jakarta.inject.Named
                    import org.springframework.beans.factory.annotation.Qualifier
                    import jakarta.annotation.Resource

                    @Component
                    class MyComponent {
                      @Autowired @Named("taskExecutor")
                      private lateinit var taskExecutor1 : TaskExecutor

                      @Autowired @Qualifier("taskExecutor")
                      private lateinit var taskExecutor2 : TaskExecutor

                      @Resource(name = "taskExecutor")
                      private lateinit var taskExecutor3 : TaskExecutor

                      public fun test(): String = taskExecutor1.toString() + taskExecutor2.toString() + taskExecutor3.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired
                    import jakarta.inject.Named
                    import org.springframework.beans.factory.annotation.Qualifier
                    import jakarta.annotation.Resource

                    @Component
                    class MyComponent {
                      @Autowired @Named("applicationTaskExecutor")
                      private lateinit var taskExecutor1 : TaskExecutor

                      @Autowired @Qualifier("applicationTaskExecutor")
                      private lateinit var taskExecutor2 : TaskExecutor

                      @Resource(name = "applicationTaskExecutor")
                      private lateinit var taskExecutor3 : TaskExecutor

                      public fun test(): String = taskExecutor1.toString() + taskExecutor2.toString() + taskExecutor3.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should qualify autowired field when it matches name and type is a subtype`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import jakarta.annotation.Resource;

                    @Component
                    class MyComponent {
                        @Autowired
                        private ConcurrentTaskExecutor taskExecutor;
                        @Resource(name = "taskExecutor")
                        private ConcurrentTaskExecutor resource;
                        public String test() { return taskExecutor.toString() + resource.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import jakarta.annotation.Resource;

                    @Component
                    class MyComponent {
                        @Autowired
                        @Qualifier("applicationTaskExecutor")
                        private ConcurrentTaskExecutor taskExecutor;
                        @Resource(name = "applicationTaskExecutor")
                        private ConcurrentTaskExecutor resource;
                        public String test() { return taskExecutor.toString() + resource.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired
                    import jakarta.annotation.Resource

                    @Component
                    class MyComponent {
                        @Autowired
                        private lateinit var taskExecutor: ConcurrentTaskExecutor
                        @Resource(name = "taskExecutor")
                        private lateinit var resource: ConcurrentTaskExecutor
                        public fun test(): String = taskExecutor.toString() + resource.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired
                    import jakarta.annotation.Resource

                    @Component
                    class MyComponent {
                        @Autowired
                        @Qualifier("applicationTaskExecutor")
                        private lateinit var taskExecutor: ConcurrentTaskExecutor
                        @Resource(name = "applicationTaskExecutor")
                        private lateinit var resource: ConcurrentTaskExecutor
                        public fun test(): String = taskExecutor.toString() + resource.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not rename autowired field when it matches name and type, but has an explicit qualifier`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import jakarta.annotation.Resource;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                      @Autowired
                      @Qualifier("customTaskExecutor")
                      private TaskExecutor taskExecutor1;

                      @Autowired
                      @Resource(name = "resourceTaskExecutor")
                      private TaskExecutor taskExecutor2;

                      public String test() { return taskExecutor1.toString() + taskExecutor2.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import jakarta.annotation.Resource
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent {
                      @Autowired @Qualifier("customTaskExecutor1")
                      private lateinit var taskExecutor1 : TaskExecutor

                      @Autowired @Resource(name = "customTaskExecutor2")
                      private lateinit var taskExecutor2 : TaskExecutor

                      public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not rename autowired field when it matches type but has a different name`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                      @Autowired
                      private TaskExecutor customTaskExecutor;

                      public String test() { return customTaskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent {
                      @Autowired
                      private lateinit var customTaskExecutor : TaskExecutor

                      public fun test(): String = customTaskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not rename autowired field when it matches name but has a different type`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                      @Autowired
                      private Object taskExecutor;

                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent {
                      @Autowired
                      private lateinit var customTaskExecutor : Any

                      public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }

        @Test
        fun `should not rename autowired field when it matches name and type but has no Autowired annotation`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;

                    @Component
                    class MyComponent {
                      private TaskExecutor taskExecutor;

                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component

                    @Component
                    class MyComponent {
                      private lateinit var customTaskExecutor : TaskExecutor

                      public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent()
                )
            )
        }
    }

    @Nested
    inner class ConstructorArgumentsTests {
        @Test
        fun `should qualify constructor arguments when it matches name and type`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      public MyComponent(TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      public MyComponent(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent1(val taskExecutor : TaskExecutor) {
                      private val inner = taskExecutor
                      public fun test(): String = taskExecutor.toString()
                    }
                    @Component
                    class MyComponent2(taskExecutor : TaskExecutor) {
                      private val inner = taskExecutor
                      public fun test(): String = inner.toString()
                    }
                    @Component
                    class MyComponent3(val taskExecutorString : String) {
                      @Autowired constructor(taskExecutor : TaskExecutor) : this(taskExecutor.toString())
                      public fun test(): String = taskExecutorString
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent1(@Qualifier("applicationTaskExecutor") val taskExecutor : TaskExecutor) {
                      private val inner = taskExecutor
                      public fun test(): String = taskExecutor.toString()
                    }
                    @Component
                    class MyComponent2(@Qualifier("applicationTaskExecutor") taskExecutor : TaskExecutor) {
                      private val inner = taskExecutor
                      public fun test(): String = inner.toString()
                    }
                    @Component
                    class MyComponent3(val taskExecutorString : String) {
                      @Autowired constructor(@Qualifier("applicationTaskExecutor") taskExecutor : TaskExecutor) : this(taskExecutor.toString())
                      public fun test(): String = taskExecutorString
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should rename constructor arguments when it matches type and qualified name`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      public MyComponent(@Qualifier("taskExecutor") TaskExecutor customName1, @Named("taskExecutor") TaskExecutor customName2) {
                        this.taskExecutor = customName1;
                        this.taskExecutor = customName2;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      public MyComponent(@Qualifier("applicationTaskExecutor") TaskExecutor customName1, @Named("applicationTaskExecutor") TaskExecutor customName2) {
                        this.taskExecutor = customName1;
                        this.taskExecutor = customName2;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Qualifier
                    import jakarta.inject.Named

                    @Component
                    class MyComponent(@Qualifier("taskExecutor") val taskExecutor1 : TaskExecutor, @Named("taskExecutor") val taskExecutor2 : TaskExecutor) {
                      public fun test(): String = taskExecutor1.toString() + taskExecutor2.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Qualifier
                    import jakarta.inject.Named

                    @Component
                    class MyComponent(@Qualifier("applicationTaskExecutor") val taskExecutor1 : TaskExecutor, @Named("applicationTaskExecutor") val taskExecutor2 : TaskExecutor) {
                      public fun test(): String = taskExecutor1.toString() + taskExecutor2.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should qualify constructor arguments when it matches name and type is a subtype of TaskExecutor`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final ConcurrentTaskExecutor taskExecutor;
                      public MyComponent(@Qualifier("taskExecutor") ConcurrentTaskExecutor customName, ConcurrentTaskExecutor taskExecutor) {
                        this.taskExecutor = customName;
                        this.taskExecutor = taskExecutor;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final ConcurrentTaskExecutor taskExecutor;
                      public MyComponent(@Qualifier("applicationTaskExecutor") ConcurrentTaskExecutor customName, @Qualifier("applicationTaskExecutor") ConcurrentTaskExecutor taskExecutor) {
                        this.taskExecutor = customName;
                        this.taskExecutor = taskExecutor;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Component
                    class MyComponent(@Qualifier("taskExecutor") val taskExecutor1 : ConcurrentTaskExecutor, val taskExecutor : ConcurrentTaskExecutor) {
                      public fun test(): String = taskExecutor1.toString() + taskExecutor.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Component
                    class MyComponent(@Qualifier("applicationTaskExecutor") val taskExecutor1 : ConcurrentTaskExecutor, @Qualifier("applicationTaskExecutor") val taskExecutor : ConcurrentTaskExecutor) {
                      public fun test(): String = taskExecutor1.toString() + taskExecutor.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should pick the constructor with @Autowired when more than one is defined`() {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      private final String s;

                      public MyComponent(TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                        this.s = taskExecutor.toString();
                      }

                      @Autowired
                      public MyComponent(TaskExecutor taskExecutor, String s) {
                        this.taskExecutor = taskExecutor;
                        this.s = s;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      private final String s;

                      public MyComponent(TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                        this.s = taskExecutor.toString();
                      }

                      @Autowired
                      public MyComponent(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor, String s) {
                        this.taskExecutor = taskExecutor;
                        this.s = s;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent(val taskExecutor: TaskExecutor, val s: String) {
                      @Autowired constructor(taskExecutor: TaskExecutor) : this(taskExecutor, taskExecutor.toString())
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent(val taskExecutor: TaskExecutor, val s: String) {
                      @Autowired constructor(@Qualifier("applicationTaskExecutor") taskExecutor: TaskExecutor) : this(taskExecutor, taskExecutor.toString())
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not qualify identifiers outside the autowired constructor scope`() {
            rewriteRun(
                // should not rename kotlin property and parameter name in constructor invocation
                kotlin(
                    before = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent(val taskExecutor: TaskExecutor, val s: String) {
                      @Autowired constructor(taskExecutor: TaskExecutor) : this(taskExecutor = taskExecutor, s = taskExecutor.toString())
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent(val taskExecutor: TaskExecutor, val s: String) {
                      @Autowired constructor(@Qualifier("applicationTaskExecutor") taskExecutor: TaskExecutor) : this(taskExecutor = taskExecutor, s = taskExecutor.toString())
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not change formatting`() {
            rewriteRun(
                // indents and spaces in kotlin
                kotlin(
                    before = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent(val taskExecutor : TaskExecutor,
                                      val s   : String) {
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired

                    @Component
                    class MyComponent(@Qualifier("applicationTaskExecutor") val taskExecutor : TaskExecutor,
                                      val s   : String) {
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                ),
                // in java we are adding a line break in case of multiple annotations, unfortunately
                java(
                    before = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                        @Autowired private TaskExecutor taskExecutor;
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @Component
                    class MyComponent {
                        @Autowired
                        @Qualifier("applicationTaskExecutor") private TaskExecutor taskExecutor;
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not change arguments when multiple constructors are defined and none of them has @Autowired`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor;
                      private final String s;

                      public MyComponent(TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                        this.s = taskExecutor.toString();
                      }

                      public MyComponent(TaskExecutor taskExecutor, String s) {
                        this.taskExecutor = taskExecutor;
                        this.s = s;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import jakarta.inject.Named

                    @Component
                    class MyComponent(val taskExecutor: TaskExecutor, val s: String) {
                      constructor(taskExecutor: TaskExecutor) : this(taskExecutor, taskExecutor.toString())
                      public fun test(): String = taskExecutor.toString() + s
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not change argument when its qualified name is not taskExecutor`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final TaskExecutor taskExecutor1;
                      private final TaskExecutor taskExecutor2;
                      private final TaskExecutor taskExecutor3;
                      private final String s;

                      @Autowired
                      public MyComponent(TaskExecutor taskExecutor1, @Qualifier("two") TaskExecutor taskExecutor, @Named("three") TaskExecutor taskExecutor3) {
                        this.taskExecutor1 = taskExecutor1;
                        this.taskExecutor2 = taskExecutor;
                        this.taskExecutor3 = taskExecutor3;
                      }
                      public String test() { return taskExecutor1.toString() + taskExecutor2.toString() + taskExecutor3.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired
                    import org.springframework.beans.factory.annotation.Qualifier
                    import jakarta.inject.Named

                    @Component
                    class MyComponent(val taskExecutor1: TaskExecutor, @Qualifier("two") val taskExecutor2: TaskExecutor, @Named("three") val taskExecutor: TaskExecutor) {
                      public fun test(): String = taskExecutor1.toString() + taskExecutor2.toString() + taskExecutor.toString()
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not change argument when it is of different type`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.stereotype.Component;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import jakarta.inject.Named;

                    @Component
                    class MyComponent {
                      private final Object o;
                      private final String s;

                      @Autowired
                      public MyComponent(String taskExecutor, @Named("taskExecutor") Object obj) {
                        this.s = taskExecutor;
                        this.o = obj;
                      }

                      public String test() { return s + o.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.stereotype.Component
                    import org.springframework.beans.factory.annotation.Autowired
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Component
                    class MyComponent(val taskExecutor: String, @Qualifier("taskExecutor") val a: Any) {
                      public fun test(): String = taskExecutor + a.toString();
                    }
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not change argument when if declaring class is not @Component`() {
            rewriteRun(
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.beans.factory.annotation.Autowired;

                    class MyComponent {
                      private final TaskExecutor t;

                      @Autowired
                      public MyComponent(TaskExecutor taskExecutor) {
                        this.t = taskExecutor;
                      }

                      public String test() { return t.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.beans.factory.annotation.Autowired

                    class MyComponent @Autowired constructor(val taskExecutor: TaskExecutor) {
                      public fun test(): String = taskExecutor.toString();
                    }
                    """.trimIndent(),
                ),
                java(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.beans.factory.annotation.Autowired;

                    @pl.allegro.tech.autoupgrades.recipes.andamio9.Component
                    class MyComponent2 {
                      private final TaskExecutor t;

                      @Autowired
                      public MyComponent2(TaskExecutor taskExecutor) {
                        this.t = taskExecutor;
                      }

                      public String test() { return t.toString(); }
                    }
                    """.trimIndent()
                ),
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.beans.factory.annotation.Autowired

                    @pl.allegro.tech.autoupgrades.recipes.andamio9.Component
                    class MyComponent2 @Autowired constructor(val taskExecutor: TaskExecutor) {
                      public fun test(): String = taskExecutor.toString();
                    }
                    """.trimIndent(),
                )
            )
        }

        @ParameterizedTest(name = "should change the argument in @{0}")
        @ValueSource(strings = [
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Controller",
            "org.springframework.stereotype.Repository",
            "org.springframework.stereotype.Service",
        ])
        fun `should treat Spring stereotype annotations as component`(annotation: String) {
            rewriteRun(
                java(
                    before = """
                    import org.springframework.core.task.TaskExecutor;

                    @$annotation
                    class MyComponent {
                      private final TaskExecutor taskExecutor;

                      public MyComponent(TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier;
                    import org.springframework.core.task.TaskExecutor;

                    @$annotation
                    class MyComponent {
                      private final TaskExecutor taskExecutor;

                      public MyComponent(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
                        this.taskExecutor = taskExecutor;
                      }
                      public String test() { return taskExecutor.toString(); }
                    }
                    """.trimIndent(),
                ),
                kotlin(
                    before = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor

                    @$annotation
                    class MyComponent(val taskExecutor: TaskExecutor) {
                      public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                    after = """
                    import org.springframework.beans.factory.annotation.Qualifier
                    import org.springframework.core.task.TaskExecutor

                    @$annotation
                    class MyComponent(@Qualifier("applicationTaskExecutor") val taskExecutor: TaskExecutor) {
                      public fun test(): String = taskExecutor.toString()
                    }
                    """.trimIndent(),
                )
            )
        }
    }

    @Test
    @Disabled("no such use cases in our org")
    fun `should rename argument in FactoryBean#getBean method`() {
        rewriteRun(
            java(
                before = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.ApplicationContext;

                    class MyConfig {
                      public void test(ApplicationContext context) {
                        context.getBean("taskExecutor");
                      }
                    }
                    """.trimIndent(),
                after = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.ApplicationContext;

                    class MyConfig {
                      public void test(ApplicationContext context) {
                        context.getBean("applicationTaskExecutor");
                      }
                    }
                    """.trimIndent(),
            )
        )
    }

    @Test
    fun `should not change anything if there is a custom @Component with name taskExecutor defined, which extends Spring's TaskExecutor`() {
        rewriteRun(
            srcMainJava(java(
                beforeAndAfter = """
                    package com.example;

                    import org.springframework.stereotype.Component;

                    @Component
                    public class TaskExecutor extends org.springframework.core.task.TaskExecutor {
                      public String toString() {
                        return "123";
                      }
                    }
                    """.trimIndent(),
                spec = {path("src/main/java/com/example/TaskExecutor.java")}
            )),
            srcMainJava(java(
                beforeAndAfter = """
                    package com.example;

                    import org.springframework.context.annotation.Bean;
                    import org.springframework.core.task.SimpleAsyncTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.beans.factory.annotation.Qualifier;

                    @Configuration
                    class StringConfig {

                      @Autowired
                      @Qualifier("taskExecutor")
                      private TaskExecutor fallbackTaskExecutor;

                      @Bean
                      public String myString(TaskExecutor taskExecutor) {
                         return taskExecutor.toString() + fallbackTaskExecutor.toString();
                      }
                    }
                    """.trimIndent(),
                spec = {path("src/main/java/com/example/StringConfig.java")}
            )),
        )
    }

    @Test
    fun `should not change anything if there is a custom taskExecutor @Bean defined`() {
        rewriteRun(
            java(
                beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.core.task.SimpleAsyncTaskExecutor;
                    import org.springframework.context.annotation.Configuration;

                    @Configuration
                    class Config {

                      @Bean
                      public TaskExecutor taskExecutor() {
                         return new SimpleAsyncTaskExecutor();
                      }
                    }
                    """.trimIndent(),
                spec = { path("src/main/resources/com/example/Config.java") }
            ),
            java(
                beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.core.task.SimpleAsyncTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.beans.factory.annotation.Qualifier;

                    @Configuration
                    class StringConfig {

                      @Autowired
                      @Qualifier("taskExecutor")
                      private TaskExecutor fallbackTaskExecutor;

                      @Bean
                      public String myString(TaskExecutor taskExecutor) {
                         return taskExecutor.toString() + fallbackTaskExecutor.toString();
                      }
                    }
                    """.trimIndent(),
                spec = { path("src/main/resources/com/example/StringConfig.java") }
            ),
        )
    }

    @Test
    fun `should not change anything if there is a custom taskExecutor @Bean with explicit name defined`() {
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.core.task.SimpleAsyncTaskExecutor;
                    import org.springframework.context.annotation.Configuration;

                    @Configuration
                    class Config {
                      @Bean(name = "taskExecutor")
                      fun custom() = SimpleAsyncTaskExecutor()
                    }
                    """.trimIndent(),
                spec = { path("src/main/resources/com/example/Config.java") }
            ),
            java(
                beforeAndAfter = """
                    import org.springframework.core.task.TaskExecutor;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.core.task.SimpleAsyncTaskExecutor;
                    import org.springframework.context.annotation.Configuration;
                    import org.springframework.beans.factory.annotation.Autowired;
                    import org.springframework.beans.factory.annotation.Qualifier;

                    @Configuration
                    class StringConfig {

                      @Autowired
                      @Qualifier("taskExecutor")
                      private TaskExecutor fallbackTaskExecutor;

                      @Bean
                      public String myString(TaskExecutor taskExecutor) {
                         return taskExecutor.toString() + fallbackTaskExecutor.toString();
                      }
                    }
                    """.trimIndent(),
                spec = { path("src/main/resources/com/example/StringConfig.java") }
            ),
        )
    }

    @Test
    @Disabled("OpenRewrite's KotlinTemplate does not support K.ClassDeclarations")
    fun `should support Kotlin ClassDeclaration`() {
        rewriteRun(
            kotlin(
                before = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.beans.factory.BeanFactory
                    import org.springframework.beans.factory.annotation.Autowired
                    import org.springframework.context.annotation.Bean

                    @Configuration
                    class MyConfigKotlin<F>(val taskExecutor: TaskExecutor) where F: BeanFactory {
                      @Autowired
                      private lateinit var innerTaskExecutor: TaskExecutor

                      @Bean
                      fun myString(taskExecutor: TaskExecutor) = taskExecutor.toString()

                      @Bean
                      fun myString2() = taskExecutor.toString()
                    }
                    """.trimIndent(),
                after = """
                    import org.springframework.core.task.TaskExecutor
                    import org.springframework.context.annotation.Configuration
                    import org.springframework.beans.factory.BeanFactory
                    import org.springframework.beans.factory.annotation.Autowired
                    import org.springframework.context.annotation.Bean
                    import org.springframework.beans.factory.annotation.Qualifier

                    @Configuration
                    class MyConfigKotlin<F>(@Qualifier("applicationTaskExecutor") val taskExecutor: TaskExecutor) where F: BeanFactory {
                      @Autowired
                      @Qualifier("applicationTaskExecutor")
                      private lateinit var innerTaskExecutor: TaskExecutor

                      @Bean
                      fun myString(@Qualifier("applicationTaskExecutor") taskExecutor: TaskExecutor) = taskExecutor.toString()

                      @Bean
                      fun myString2() = taskExecutor.toString()
                    }
                    """.trimIndent()
            )
        )
    }
}
