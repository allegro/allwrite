package pl.allegro.tech.allwrite.spi

import java.nio.file.Path

/**
 * Implement this interface in your recipe to restrict the input file set before it gets parsed.
 *
 * It may be useful to avoid costly parsing of large YAML files, etc.
 */
public interface ParsingAwareRecipe {

    /**
     * Filter the [inputFiles] list and return only those that should be parsed
     */
    public fun selectFilesToParse(inputFiles: List<Path>): List<Path>
}
