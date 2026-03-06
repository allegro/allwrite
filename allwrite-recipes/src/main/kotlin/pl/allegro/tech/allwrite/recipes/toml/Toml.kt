package pl.allegro.tech.allwrite.recipes.toml

import org.openrewrite.Tree
import org.openrewrite.marker.Markers
import org.openrewrite.toml.marker.InlineTable
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import org.openrewrite.toml.tree.TomlKey
import org.openrewrite.toml.tree.TomlRightPadded
import org.openrewrite.toml.tree.TomlType
import pl.allegro.tech.allwrite.recipes.util.mapLast
import java.util.UUID

internal fun Toml.KeyValue.stringKey(): String? = (this.key as? Toml.Identifier)?.name
internal fun Toml.KeyValue.stringValue(): String? = (this.value as? Toml.Literal)?.asString()
internal fun Toml.Table.name() = this.name?.name
internal fun Toml.Table.findLiteralValue(key: String): Toml.Literal? = values.filterIsInstance<Toml.KeyValue>().firstOrNull { it.stringKey() == key }?.value as? Toml.Literal

internal fun Toml.Literal.asString() = if (type == TomlType.Primitive.String) value as String else null


internal object Builders {
    fun KeyValue(
        id: UUID = Tree.randomId(),
        prefix: Space = Space.EMPTY,
        markers: Markers = Markers.EMPTY,
        key: TomlRightPadded<out TomlKey>,
        value: Toml
    ): Toml.KeyValue {
        return Toml.KeyValue(id, prefix, markers, key as TomlRightPadded<TomlKey>, value)
    }

    fun id(name: String) = Toml.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, name, name)

    fun <E : Toml> rightPad(nested: E) = TomlRightPadded<E>(nested, Space.EMPTY, Markers.EMPTY)

    fun literal(value: String) = Toml.Literal(Tree.randomId(), Space.EMPTY, Markers.EMPTY, TomlType.Primitive.String, "\"${value}\"", value)

    fun kv(name: String, value: String) = KeyValue(
        key = rightPad(id(name)).withAfter(Space.SINGLE_SPACE),
        value = literal(value).withPrefix(Space.SINGLE_SPACE)
    )

    fun kv(name: String, value: Map<String, String>) = KeyValue(
        key = rightPad(id(name)).withAfter(Space.SINGLE_SPACE),
        value = value
            .map { (k, v) -> rightPad(kv(k, v).withPrefix(Space.SINGLE_SPACE)) }
            .toList()
            .mapLast { it.withAfter(Space.SINGLE_SPACE) }
            .let { inlineTable(it) }
            .withPrefix(Space.SINGLE_SPACE)
    )

    fun inlineTable(values: List<TomlRightPadded<Toml.KeyValue>>) =
        Toml.Table(Tree.randomId(), Space.EMPTY, Markers.build(mutableListOf(InlineTable(Tree.randomId()))), null, values as List<TomlRightPadded<Toml>>)

    fun emptyTable() = Toml.Table(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, mutableListOf())
}
