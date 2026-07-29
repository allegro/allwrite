package pl.allegro.tech.allwrite.recipes.java;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindReferencedTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class RemoveUnusedImportsOfType extends Recipe  {
    String[] types;

    public RemoveUnusedImportsOfType() {
    }

    public RemoveUnusedImportsOfType(String[] types) {
        this.types = types;
    }

    @NotNull
    public Set<String> getTags() {
        return Set.of("visibility:internal");
    }

    @Option(displayName = "Types to remove from imports if they are not used",
        description = "An array of fully-qualified type names to remove from imports if they are not used in the code.",
        example = "[java.lang.String]")
    public void setTypes(String[] types) {
        this.types = types;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove unused imports of specific types";
    }

    @Override
    public @NotNull String getDescription() {
        return "Removal of unnecessary imports, but limited only to selected types.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            @Override
            public @org.jspecify.annotations.Nullable J postVisit(@NonNull J tree, @NotNull ExecutionContext executionContext) {
                if (!(tree instanceof JavaSourceFile sf)) {
                    return super.postVisit(tree, executionContext);
                }

                // Find references from imports and from other code separately
                var importedTypes = sf.getImports().stream()
                    .filter(i -> TypeUtils.asFullyQualified(i.getQualid().getType()) != null)
                    .map(i -> TypeUtils.asFullyQualified(i.getQualid().getType()).getFullyQualifiedName())
                    .collect(toSet());

                Stream<? extends J> startingPoint = sf.getClasses().stream();
                if (tree instanceof K.CompilationUnit kcu) {
                    startingPoint = Stream.concat(startingPoint, kcu.getStatements().stream());
                }
                var referencedTypes = startingPoint
                    .flatMap(c -> FindReferencedTypes.find(c).stream())
                    .map(JavaType.FullyQualified::getFullyQualifiedName)
                    .collect(toSet());

                var importedNotReferencedTypes = new HashSet<>(importedTypes);
                importedNotReferencedTypes.removeAll(referencedTypes);
                for (var t : types) {
                    if (importedNotReferencedTypes.contains(t)) {
                        maybeRemoveImport(t);
                    }
                }
                return super.postVisit(tree, executionContext);
            }
        };
    }
}
