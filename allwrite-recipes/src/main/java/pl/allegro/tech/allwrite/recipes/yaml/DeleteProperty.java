/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.allegro.tech.allwrite.recipes.yaml;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;
import pl.allegro.tech.allwrite.recipes.yaml.visitor.CopyDeletedPropertyCommentsVisitor;
import pl.allegro.tech.allwrite.recipes.yaml.visitor.PreOrderTraversalSnapshot;
import pl.allegro.tech.allwrite.recipes.yaml.visitor.PreOrderVisitor;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;

/*
 This is OpenRewrite's implementation (as of version 8.42.5) with a couple of changes:
 - allow deletion of properties from documents with anchors
 - preserving prefixes (comments) of deleted entries when needed
 */
public class DeleteProperty extends Recipe {

    @Option(displayName = "Property key",
        description = "The key to be deleted.",
        example = "management.metrics.binders.files.enabled")
    String propertyKey;

    @Deprecated
    @Option(displayName = "Coalesce",
        description = "(Deprecated: in a future version, this recipe will always use the `false` behavior)" +
            " Simplify nested map hierarchies into their simplest dot separated property form.",
        required = false)
    @Nullable
    Boolean coalesce;

    @Option(displayName = "Use relaxed binding",
        description =
            "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) "
                +
                "rules. Defaults to `true`. If you want to use exact matching in your search, set this to `false`.",
        required = false)
    @Nullable
    Boolean relaxedBinding;

    @Option(displayName = "File pattern",
        description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
        required = false,
        example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    public DeleteProperty(String propertyKey, @Nullable Boolean coalesce, @Nullable Boolean relaxedBinding, @Nullable String filePattern) {
        this.propertyKey = propertyKey;
        this.coalesce = coalesce;
        this.relaxedBinding = relaxedBinding;
        this.filePattern = filePattern;
    }

    public DeleteProperty() {
    }

    @Override
    public String getDisplayName() {
        return "Delete property";
    }

    @Override
    public String getDescription() {
        return "Delete a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " +
            " as Spring Boot interprets application.yml files like `a.b.c.d` or `a.b.c:d`.";
    }

    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public void setCoalesce(@Nullable Boolean coalesce) {
        this.coalesce = coalesce;
    }

    public void setRelaxedBinding(@Nullable Boolean relaxedBinding) {
        this.relaxedBinding = relaxedBinding;
    }

    public void setFilePattern(@Nullable String filePattern) {
        this.filePattern = filePattern;
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<>() {

            @Override
            public @NotNull Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.@NotNull Entry entry, @NotNull ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);

                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                    .filter(Yaml.Mapping.Entry.class::isInstance)
                    .map(Yaml.Mapping.Entry.class::cast)
                    .collect(Collectors.toCollection(ArrayDeque::new));

                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                    .map(e2 -> e2.getKey().getValue())
                    .collect(Collectors.joining("."));

                if (!Boolean.FALSE.equals(relaxedBinding) ? NameCaseConvention.equalsRelaxedBinding(prop, propertyKey) : prop.equals(propertyKey)) {
                    doAfterVisit(new DeletePropertyVisitor(entry));
                    if (Boolean.TRUE.equals(coalesce)) {
                        maybeCoalesceProperties();
                    }
                }

                return e;
            }
        });
    }

    private static class DeletePropertyVisitor extends YamlVisitor<ExecutionContext> {

        private final Yaml.Mapping.Entry scope;
        private PreOrderTraversalSnapshot traversalSnapshot;

        private DeletePropertyVisitor(Yaml.Mapping.Entry scope) {
            this.scope = scope;
        }

        @Override
        public Yaml visitDocument(Yaml.Document document, ExecutionContext p) {
            var capturer = new PreOrderVisitor();
            traversalSnapshot = capturer.traverse(document, p);

            var doc = (Yaml.Document) super.visitDocument(document, p);

            Yaml.Block rootMapping = doc.getBlock();
            if (rootMapping instanceof Yaml.Mapping && !document.isExplicit()) {
                // what if there are comments in the beginning of the doc?
                Yaml.Mapping newMapping = ((Yaml.Mapping) rootMapping).withEntries(ListUtils.mapFirst(((Yaml.Mapping) rootMapping).getEntries(), (it) -> {
                    String newPrefix = kotlin.text.StringsKt.removePrefix(it.getPrefix(), "\n");
                    return it.withPrefix(newPrefix);
                }));
                doc = doc.withBlock(newMapping);
            }

            return doc;
        }

        @Override
        public Yaml visitSequence(Yaml.Sequence sequence, ExecutionContext p) {
            sequence = (Yaml.Sequence) super.visitSequence(sequence, p);
            List<Yaml.Sequence.Entry> entries = sequence.getEntries();
            if (entries.isEmpty()) {
                return sequence;
            }

            entries = ListUtils.map(entries, entry -> ToBeRemoved.hasMarker(entry) ? null : entry);
            return entries.isEmpty() ? ToBeRemoved.withMarker(sequence) : sequence.withEntries(entries);
        }

        @Override
        public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext p) {
            entry = (Yaml.Sequence.Entry) super.visitSequenceEntry(entry, p);
            if (entry.getBlock() instanceof Yaml.Mapping) {
                Yaml.Mapping m = (Yaml.Mapping) entry.getBlock();
                if (ToBeRemoved.hasMarker(m)) {
                    doAfterVisit(new CopyDeletedPropertyCommentsVisitor(traversalSnapshot, entry, m));
                    return ToBeRemoved.withMarker(entry);
                }
            }
            return entry;
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping, ExecutionContext p) {
            Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, p);

            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();

            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (entry == scope || ToBeRemoved.hasMarker(entry.getValue())) {
                    doAfterVisit(new CopyDeletedPropertyCommentsVisitor(traversalSnapshot, entry, m));
                    changed = true;
                } else {
                    entries.add(entry);
                }
            }

            if (changed) {
                m = m.withEntries(entries);
                if (entries.isEmpty()) {
                    m = ToBeRemoved.withMarker(m);
                }

                if (getCursor().getParentOrThrow().getValue() instanceof Yaml.Document) {
                    Yaml.Document document = getCursor().getParentOrThrow().getValue();
                    if (!document.isExplicit()) {
                        m = m.withEntries(m.getEntries());
                    }
                }
            }
            return m;
        }
    }

    private static class ToBeRemoved implements Marker {

        UUID id;

        static <Y2 extends Yaml> Y2 withMarker(Y2 y) {
            return y.withMarkers(y.getMarkers().addIfAbsent(new ToBeRemoved(randomId())));
        }

        static boolean hasMarker(Yaml y) {
            return y.getMarkers().findFirst(ToBeRemoved.class).isPresent();
        }

        public ToBeRemoved(UUID id) {
            this.id = id;
        }

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public ToBeRemoved withId(UUID id) {
            return new ToBeRemoved(id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DeleteProperty that = (DeleteProperty) o;
        return Objects.equals(propertyKey, that.propertyKey) && Objects.equals(coalesce, that.coalesce) && Objects.equals(relaxedBinding, that.relaxedBinding)
            && Objects.equals(filePattern, that.filePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyKey, coalesce, relaxedBinding, filePattern);
    }
}
