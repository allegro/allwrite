package pl.allegro.tech.allwrite.runner.application.port.outgoing

import org.openrewrite.Recipe
import java.nio.file.Path
import java.io.File

public interface InputFilesProvider {
    public fun getInputFilesFor(recipe: Recipe): List<Path>
}
