package pl.allegro.tech.allwrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

import java.util.Set;

public class JavaPublicRecipe extends Recipe {

    @NotNull
    @Override
    public String getDisplayName() {
        return "Public Java recipe";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Public Java recipe description.";
    }

    @NotNull
    @Override
    public Set<String> getTags() {
        return Set.of("visibility:public");
    }
}
