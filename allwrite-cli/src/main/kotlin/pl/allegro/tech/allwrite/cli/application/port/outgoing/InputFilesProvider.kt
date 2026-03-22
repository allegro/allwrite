package pl.allegro.tech.allwrite.cli.application.port.outgoing

import org.openrewrite.Recipe
import java.nio.file.Path

public interface InputFilesProvider {
    public fun getInputFilesFor(recipe: Recipe): List<Path>
}
