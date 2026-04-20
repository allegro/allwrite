package pl.allegro.tech.allwrite.recipes.yaml.visitor

import org.openrewrite.ExecutionContext
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import kotlin.reflect.KClass

/**
 * Visitor to traverse the whole tree and capture all subtrees of type [Y]
 */
internal open class EmittingVisitor<Y : Yaml>(
    private val type: KClass<out Y>,
) : YamlIsoVisitor<ExecutionContext>() {
    open val nodes: MutableMap<YamlPath, Y> = HashMap()

    override fun preVisit(tree: Yaml, p: ExecutionContext): Yaml? {
        if (type.isInstance(tree)) {
            nodes[cursor.toYamlPath()] = tree as Y
        }
        return super.preVisit(tree, p)
    }

    companion object {
        inline operator fun <reified Y : Yaml> invoke() = EmittingVisitor(Y::class)
    }
}
