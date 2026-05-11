package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.gradle.GradleParser
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.java.JavaParser
import org.openrewrite.java.marker.JavaSourceSet
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.properties.PropertiesParser
import org.openrewrite.quark.QuarkParser
import org.openrewrite.text.PlainTextParser
import org.openrewrite.toml.TomlParser
import org.openrewrite.tree.ParseError
import org.openrewrite.yaml.YamlParser
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.ParsingAwareRecipe
import pl.allegro.tech.allwrite.runtime.util.withNestedRecipes
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Collections.enumeration
import java.util.Enumeration
import kotlin.io.path.extension

@Single
internal class SourceFilesParser {

    fun parseSourceFiles(recipe: Recipe, files: List<Path>, ctx: ExecutionContext): List<SourceFile> {
        val filesToParse = selectFilesToParse(recipe, files)

        val remainingFiles = filesToParse.toMutableList()
        val allParsedFiles = mutableListOf<SourceFile>()

        val classpath = resolveClasspath(recipe)
        val sourceSet = JavaSourceSet.build("allwrite", emptyList()) // TODO is it ok to put empty list here?
        val parsers = withExternalRecipeClassLoaders(recipe) { createParsers(classpath, ctx) }

        for (parser in parsers) {
            val acceptedFiles = remainingFiles.filter(parser::accept)

            val fileExtensions = acceptedFiles.map(Path::extension).distinct()
            logger.info { "Parsing ${acceptedFiles.size} files with ${parser.name}: $fileExtensions" }

            val parsedFiles = parser.parse(acceptedFiles, null, ctx).toList()

            remainingFiles.removeAll(parsedFiles.map { it.sourcePath })
            allParsedFiles.addAll(parsedFiles)

            if (parsedFiles.size < acceptedFiles.size) {
                val notParsedFiles = (acceptedFiles - parsedFiles.map { it.sourcePath })
                val notParsedFilesListing = notParsedFiles.joinToString(prefix = "\n", separator = "\n") { "- $it" }
                logger.warn { "Unable to parse ${notParsedFiles.size} files with ${parser.name}: $notParsedFilesListing" }
            }
        }

        checkThatNoFilesWereParsedMoreThanOnce(allParsedFiles, filesToParse)
        checkThatAllFilesWereParsed(allParsedFiles, filesToParse)
        return allParsedFiles
            .map<SourceFile, SourceFile> { it.withMarkers(it.markers.add(sourceSet)) }
            .map { handleParseError(it) }
    }

    private fun selectFilesToParse(recipe: Recipe, files: List<Path>) = (recipe as? ParsingAwareRecipe)?.selectFilesToParse(files) ?: files

    private fun resolveClasspath(recipe: Recipe): List<String> {
        val artifacts = recipe.withNestedRecipes().filterIsInstance<ClasspathAwareRecipe>()
            .flatMap { it.requireOnClasspath() }
            .distinct()
        if (artifacts.isNotEmpty()) {
            logger.info { "Resolved recipe classpath: $artifacts" }
        }
        return artifacts
    }

    private fun <T> withExternalRecipeClassLoaders(recipe: Recipe, block: () -> T): T {
        val externalRecipeClassLoaders = recipe.withNestedRecipes()
            .filterIsInstance<ClasspathAwareRecipe>()
            .map { (it as Any).javaClass.classLoader }
            .filter { it !== javaClass.classLoader }
            .distinct()

        if (externalRecipeClassLoaders.isEmpty()) return block()

        val compositeClassLoader = ExternalRecipeCompositeClassLoader(
            externalRecipeClassLoaders,
            parent = Thread.currentThread().contextClassLoader,
        )

        val previousClassLoader = Thread.currentThread().contextClassLoader
        return try {
            Thread.currentThread().contextClassLoader = compositeClassLoader
            block()
        } finally {
            Thread.currentThread().contextClassLoader = previousClassLoader
        }
    }

    private fun createParsers(classpath: List<String>, ctx: ExecutionContext): List<Parser> =
        listOf(
            GradleParser.builder().build(),
            KotlinParser.builder().classpathFromResources(ctx, *classpath.toTypedArray()).build(),
            GroovyParser.builder().classpathFromResource(ctx, *classpath.toTypedArray()).build(),
            JavaParser.fromJavaVersion().classpathFromResources(ctx, *classpath.toTypedArray()).build(),
            YamlParser.builder().build(),
            TomlParser.builder().build(),
            PropertiesParser.builder().build(),
            PlainTextParser.builder().plainTextMasks(Paths.get("."), PLAIN_TEXT_MASKS).build(),
            QuarkParser.builder().build(),
        )

    private fun checkThatNoFilesWereParsedMoreThanOnce(parsedFiles: List<SourceFile>, files: List<Path>) {
        if (parsedFiles.size > files.size) {
            val filesParsedMoreThanOnce = parsedFiles.map { it.sourcePath }
                .groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
                .keys
                .joinToString(prefix = "\n", separator = "\n") { "- $it" }
            throw IllegalStateException("ERROR: Some files were parsed by more than one parser: $filesParsedMoreThanOnce")
        }
    }

    private fun checkThatAllFilesWereParsed(parsedFiles: List<SourceFile>, files: List<Path>) {
        if (parsedFiles.size < files.size) {
            val notParsedFiles = (files - parsedFiles.map { it.sourcePath })
                .joinToString(prefix = "\n", separator = "\n") { "- $it" }
            throw IllegalStateException("ERROR: Some files were not parsed, maybe additional parser needs to be provided: $notParsedFiles")
        }
    }

    private fun handleParseError(source: SourceFile): SourceFile {
        if (source !is ParseError) return source
        if (source.erroneous !is J.CompilationUnit) return source
        val original = source.erroneous
        val result = Java22TreeAdapterVisitor().visit(original as J.CompilationUnit, InMemoryExecutionContext()) as J.CompilationUnit
        return if (result !== original) source else result
    }

    companion object {

        private val logger = KotlinLogging.logger { }

        private val PLAIN_TEXT_MASKS: List<String> = listOf(
            "**/*.scala",
            "**/*.adoc",
            "**/*.bash",
            "**/*.bat",
            "**/CODEOWNERS",
            "**/*.css",
            "**/*.config",
            "**/Dockerfile*",
            "**/.gitattributes",
            "**/.gitignore",
            "**/*.htm*",
            "**/gradlew",
            "**/.java-version",
            "**/*.jsp",
            "**/*.ksh",
            "**/lombok.config",
            "**/*.md",
            "**/*.mf",
            "**/META-INF/services/**",
            "**/META-INF/spring/**",
            "**/META-INF/spring.factories",
            "**/mvnw",
            "**/*.qute.java",
            "**/.sdkmanrc",
            "**/*.sh",
            "**/*.sql",
            "**/*.svg",
            "**/*.txt",
            "**/*.toml",
            "**/*.gradle",
        )
    }
}

private val Parser.name: String
    get() =
        if (javaClass.isAnonymousClass) {
            javaClass.superclass.simpleName
        } else {
            javaClass.simpleName
        }

private class ExternalRecipeCompositeClassLoader(
    private val externalRecipeClassLoaders: List<ClassLoader>,
    parent: ClassLoader?,
) : ClassLoader(parent) {

    override fun findClass(name: String): Class<*> {
        for (classLoader in externalRecipeClassLoaders) {
            try {
                return classLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
            }
        }
        throw ClassNotFoundException(name)
    }

    override fun findResource(name: String): URL? {
        for (classLoader in externalRecipeClassLoaders) {
            classLoader.getResource(name)?.let { return it }
        }
        return null
    }

    override fun findResources(name: String): Enumeration<URL> {
        val urls = externalRecipeClassLoaders.flatMap { it.getResources(name).toList() }
        return enumeration(urls)
    }
}
