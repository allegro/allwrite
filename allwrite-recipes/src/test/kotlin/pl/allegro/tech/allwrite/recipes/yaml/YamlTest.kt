package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.Cursor
import org.openrewrite.yaml.YamlParser
import org.openrewrite.yaml.tree.Yaml
import kotlin.jvm.optionals.getOrNull

interface YamlTest {

    fun parse(yaml: String): Yaml.Documents = parser.parse(yaml).findFirst().getOrNull() as Yaml.Documents

    fun parse(original: String, beforeVisit: String): Pair<Yaml.Documents, Yaml.Documents> {
        val originalDocs = parser.parse(original).findFirst().getOrNull() as Yaml.Documents
        val beforeVisitDocs = parser.parse(beforeVisit).findFirst().getOrNull() as Yaml.Documents
        val docsWithSameIds = beforeVisitDocs.documents.mapIndexed { index, doc ->
            doc.withId(originalDocs.documents[index].id)
        }
        return Pair(originalDocs, beforeVisitDocs.withDocuments(docsWithSameIds))
    }

    fun Yaml.Documents.toYamlString() = this.print(Cursor(null, Cursor.ROOT_VALUE)).trim()

    companion object {
        private val parser = YamlParser()
    }
}
