package pl.allegro.tech.allwrite.recipes;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

import java.util.Set;

public class Java3rdPartyRecipe extends Recipe {

    @NotNull
    @Override
    public String getDisplayName() {
        return "3rd party Java recipe";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "3rd party Java recipe description.";
    }

    @NotNull
    @Override
    public Set<String> getTags() {
        return Set.of();
    }
}
