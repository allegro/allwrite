/*
 * Copyright 2022 the original author or authors.
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
package pl.allegro.tech.allwrite.recipes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import pl.allegro.tech.allwrite.recipes.yaml.DeleteProperty;

import static org.openrewrite.yaml.Assertions.yaml;

public class DeletePropertyKeyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DeleteProperty("management.metrics.binders.files.enabled", null, null, null));
    }

    @DocumentExample
    @Test
    void singleEntry() {
        rewriteRun(
            yaml("management.metrics.binders.files.enabled: true",
                ""
            )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1841")
    void firstItem() {
        rewriteRun(
            yaml(
                """
                    management.metrics.binders.files.enabled: true
                    server.port: 8080
                    """,
                """
                    server.port: 8080
                    """
            )
        );
    }

    @Test
    void expandedNotationFull() {
        rewriteRun(
            yaml(
                """
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                    server.port: 8080
                    """,
                """
                    server.port: 8080
                    """
            )
        );
    }

    @Test
    void expandedNotationPartial() {
        rewriteRun(
            yaml(
                """
                    management:
                      metrics:
                        binders:
                          smth-else:
                            enabled: true
                          files:
                            enabled: true
                    server.port: 8080
                    """,
                """
                    management:
                      metrics:
                        binders:
                          smth-else:
                            enabled: true
                    server.port: 8080
                    """
            )
        );
    }

    @Test
    void shouldRemoveCommentsBelongingToRemovedProperties() {
        rewriteRun(

            // suffix comment at the coalesced mapping
            yaml(
                """
                    server.port: 8080
                    management.metrics.binders.files.enabled: true # comment should disappear
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-coalesced")
            ),

            // suffix comment at the lowest level of the expanded mapping
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true # comment should disappear
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-lowest")
            ),

            // comment at the lowest level of the expanded mapping
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            # comment should disappear
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    smth-else: 1
                    """,
                spec -> spec.path("main-expanded-lowest")
            ),

            // suffix comment at the middle level of expanded mapping
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files: # comment should disappear
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-mid")
            ),

            // comment at the middle level of expanded mapping
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          # comment should disappear
                          files:
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    smth-else: 1
                    """,
                spec -> spec.path("main-expanded-mid")
            ),

            // suffix comment at the top level of expanded mapping
            yaml(
                """
                    server.port: 8080
                    management: # comment should disappear
                      metrics:
                        binders:
                          files:
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-top")
            ),

            // all comments in the removed hierarchy when there is no entry afterwards
            yaml(
                """
                    server.port: 8080
                    management: # suffix1
                      metrics:
                        binders:
                          files:
                            # main
                            enabled: true # suffix2
                    """,
                """
                    server.port: 8080
                    """,
                spec -> spec.path("last-entry-comments")
            ),

            // all comments in the removed hierarchy when there is no entry afterwards
            yaml(
                """
                    management: # suffix1
                      metrics: #suffix2
                        # main1
                        binders: # suffix3
                          # main2
                          files:
                            # main3
                            enabled: true # suffix4
                    """,
                "",
                spec -> spec.path("only-entry-comments")
            ),

            // all comments in the removed hierarchy when Document.End is not empty
            yaml(
                """
                    management: # suffix1
                      metrics: #suffix2
                        # main1
                        binders: # suffix3
                          # main2
                          files:
                            # main3
                            enabled: true # suffix4
                    # this comment is related to document end
                    """,
                """


                    # this comment is related to document end
                    """,
                spec -> spec.path("comments-document-end")
            )
        );
    }

    @Test
    void shouldNotLoseCommentInsideSequence() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("metrics.prometheus.percentiles", null, null, null)),

            // should keep comments before removed sequence
            yaml(
                """
                    metrics:
                      prometheus: # comment1 should stay
                        # comment2 should stay
                        percentiles: [ 0.99, 0.999 ]
                        enabled: true
                    """,
                """
                    metrics:
                      prometheus: # comment1 should stay
                        # comment2 should stay
                        enabled: true
                    """,
                spec -> spec.path("before")
            ),

            // should keep comments before removed sequence
            yaml(
                """
                    metrics:
                      prometheus:
                        enabled: true # comment should stay
                        percentiles: [ 0.99, 0.999 ]
                    """,
                """
                    metrics:
                      prometheus:
                        enabled: true # comment should stay
                    """,
                spec -> spec.path("before-post")
            ),

            // should keep comments after removed sequence
            yaml(
                """
                    metrics:
                      prometheus:
                        percentiles: [ 0.99, 0.999 ] # comment1 should disappear
                        # comment2 should stay
                        enabled: true # comment3 should stay
                    """,
                """
                    metrics:
                      prometheus:
                        # comment2 should stay
                        enabled: true # comment3 should stay
                    """,
                spec -> spec.path("after")
            ),

            // sequence in sequence
            yaml(
                """
                    metrics:
                      prometheus:
                        - name: name1
                          percentiles: [ 0.99, 0.999 ]
                          enabled: true
                        - name: name2 # comment should stay
                          percentiles: [ 0.5 ]
                    """,
                """
                    metrics:
                      prometheus:
                        - name: name1
                          enabled: true
                        - name: name2 # comment should stay
                    """,
                spec -> spec.path("seq-in-seq")
            ),

            // removed sequence entry
            yaml(
                """
                    metrics:
                      prometheus:
                        - name: name1
                          percentiles: [ 0.99, 0.999 ]
                          enabled: true # comment should stay
                        - percentiles: [ 0.5 ]
                    """,
                """
                    metrics:
                      prometheus:
                        - name: name1
                          enabled: true # comment should stay
                    """,
                spec -> spec.path("removed-seq-entry")
            )
        );
    }

    @Test
    void shouldNotLoseComment() {
        rewriteRun(

            // suffix comment to the previous entry, coalesced removed mapping
            yaml(
                """
                    server.port: 8080 # comment should stay
                    management.metrics.binders.files.enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-prev")
            ),

            // suffix comment to the previous entry, coalesced removed mapping, removed entry is the last
            // in the document
            yaml(
                """
                    server.port: 8080 # comment should stay
                    management.metrics.binders.files.enabled: true
                    """,
                """
                    server.port: 8080 # comment should stay
                    """,
                spec -> spec.path("suffix-prev-removed-last")
            ),

            // suffix comment to the previous entry, expanded removed mapping
            yaml(
                """
                    server.port: 8080 # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-prev")
            ),

            // suffix comment to the previous entry, expanded removed mapping, removed entry is the last in
            // the document
            yaml(
                """
                    server.port: 8080 # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                    """,
                """
                    server.port: 8080 # comment should stay
                    """,
                spec -> spec.path("suffix-expanded-prev-last")
            ),

            // suffix comment to the previous expanded entry, expanded removed mapping
            yaml(
                """
                    server:
                      port: # comment should stay

                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true

                    smth-else: 1
                    """,
                """
                    server:
                      port: # comment should stay


                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-prev-expanded")
            ),

            // when removed property has a sibling property, siblings' suffix comments should be preserved
            yaml(
                """
                    server.port: 8080 # comment1 should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment2 should stay
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment1 should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment2 should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-sibling")
            ),

            // when removed property has a sibling property, siblings' suffix comments should be preserved
            yaml(
                """
                    server.port: 8080 # comment1 should stay
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                            one: 1 # comment2 should stay
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment1 should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment2 should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-sibling-post")
            ),

            // when removed property has a sibling properties, siblings' suffix comments should be preserved
            yaml(
                """
                    server.port: 8080 # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment1 should stay
                            two: 2 # comment2 should stay
                            enabled: true # comment should disappear
                            three: 3 # comment3 should stay
                            four: 4 # comment4 should stay
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment1 should stay
                            two: 2 # comment2 should stay
                            three: 3 # comment3 should stay
                            four: 4 # comment4 should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-sibling-multiple")
            ),

            // when removed property has a sibling properties, its main comment should be preserved
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            # this comment may be related to the whole `files` block
                            enabled: true
                            one: 1
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            # this comment may be related to the whole `files` block
                            one: 1
                    smth-else: 1
                    """,
                spec -> spec.path("main-expanded-sibling")
            ),

            // when removed property has a sibling properties, its main comment should be preserved
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1
                            # comment should stay
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1
                            # comment should stay
                    smth-else: 1
                    """,
                spec -> spec.path("main-expanded-sibling-post")
            ),

            // when removed property's parent has a sibling property, siblings' suffix comments should be preserved
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          one: 1 # comment should stay
                          files:
                            enabled: true
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          one: 1 # comment should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-parent-sibling")
            ),

            // when removed property's parent has a sibling property, siblings' suffix comments should be preserved
            yaml(
                """
                    server.port: 8080 # comment1 should stay
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                          one: 1 # comment2 should stay
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment1 should stay
                    management:
                      metrics:
                        binders:
                          one: 1 # comment2 should stay
                    smth-else: 1
                    """,
                spec -> spec.path("suffix-expanded-parent-sibling-post")
            ),

            // when removed property's parent has a sibling properties, its main comment should be preserved
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          # this comment may be related to the whole `binders` block
                          files:
                            enabled: true
                          one: 1
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          # this comment may be related to the whole `binders` block
                          one: 1
                    smth-else: 1
                    """,
                spec -> spec.path("main-expanded-parent-sibling")
            ),

            // when removed property's parent has a sibling properties, its main comment should be preserved
            yaml(
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                          # this comment should stay
                          one: 1
                    smth-else: 1
                    """,
                """
                    server.port: 8080
                    management:
                      metrics:
                        binders:
                          # this comment should stay
                          one: 1
                    smth-else: 1
                    """,
                spec -> spec.path("main-expanded-parent-sibling-post")
            ),

            // next entry after removed entry should merge its comments with a suffix of the entry located
            // before removed entry
            yaml(
                """
                    server.port: 8080 # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment should stay
                            enabled: true





                    # note many line breaks and this comment
                    # we should still keep it
                    smth-else: 1
                    """,
                """
                    server.port: 8080 # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            one: 1 # comment should stay





                    # note many line breaks and this comment
                    # we should still keep it
                    smth-else: 1
                    """,
                spec -> spec.path("merge-prev-and-next")
            ),

            // main comment before removed entry at the root mapping
            yaml(
                """
                    # Probably this comment is related to the whole document
                    # It should not be removed
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                    server.port: 8080
                    """,
                """
                    # Probably this comment is related to the whole document
                    # It should not be removed
                    server.port: 8080
                    """,
                spec -> spec.path("main-root-mapping")
            ),

            // main comment before removed entry at the root mapping, removed entry is the last
            // in the document
            yaml(
                """
                    # Probably this comment is related to the whole document
                    # It should not be removed


                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                    """,
                """
                    # Probably this comment is related to the whole document
                    # It should not be removed



                    """,
                spec -> spec.path("main-root-mapping-last")
            ),

            // comment in the Document.End should be cleaned from removed entry suffix
            yaml(
                """
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true # this should disappear
                    # this may be some commented piece of yaml
                    # which should stay
                    """,
                """


                    # this may be some commented piece of yaml
                    # which should stay
                    """,
                spec -> spec.path("end-comment-without-suffix")
            ),

            // main comment at the root mapping level should stay, the only entry in the doc
            yaml(
                """
                    # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true
                    """,
                """
                    # comment should stay

                    """,
                spec -> spec.path("main-root-only-mapping")
            ),

            // should support document delimiters
            yaml(
                """
                    # comment should stay
                    management:
                      metrics:
                        binders:
                          files:
                            enabled: true # should disappear
                    ---
                    smth-else: 1
                    """,
                """
                    # comment should stay

                    ---
                    smth-else: 1
                    """,
                spec -> spec.path("doc-delimiters")
            )
        );
    }

    @Test
    void deleteSequenceItem() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("foo.bar.sequence.propertyA",
                null, null, null)),
            yaml(
                """
                      foo:
                        bar:
                          sequence:
                            - name: name
                            - propertyA: fieldA
                            - propertyB: fieldB
                          scalar: value
                    """,
                """
                      foo:
                        bar:
                          sequence:
                            - name: name
                            - propertyB: fieldB
                          scalar: value
                    """
            )
        );
    }

    @Test
    void deleteEntireSequence() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("foo.bar.sequence.propertyA",
                null, null, null)),
            yaml(
                """
                    foo:
                      bar:
                        sequence:
                          - propertyA: fieldA
                        # comments
                        scalar: value
                    """,
                """
                    foo:
                      bar:
                        # comments
                        scalar: value
                    """
            )
        );
    }

    @Test
    void deleteFirstItemWithComments() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("foo.bar.sequence",
                null, null, null)),
            yaml(
                """
                    foo:
                      bar:
                        sequence:
                          - name: name
                          - propertyA: fieldA
                          - propertyB: fieldB
                        # Some comments
                        scalar: value
                    """,
                """
                    foo:
                      bar:
                        # Some comments
                        scalar: value
                    """
            )
        );
    }

    @Test
    void lastItem() {
        rewriteRun(
            yaml(
                """
                    server.port: 8080
                    management.metrics.binders.files.enabled: true
                    """,
                """
                    server.port: 8080
                    """
            )
        );
    }

    @Test
    void middleItem() {
        rewriteRun(
            yaml(
                """
                    app.name: foo
                    management.metrics.binders.files.enabled: true
                    server.port: 8080
                    """,
                """
                    app.name: foo
                    server.port: 8080
                    """
            )
        );
    }

    @Test
    void downDeeper() {
        rewriteRun(
            yaml(
                """
                    management.metrics:
                      enabled: true
                      binders.files.enabled: true
                    server.port: 8080
                    """,
                """
                    management.metrics:
                      enabled: true
                    server.port: 8080
                    """
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "acme.my-project.person.first-name",
        "acme.myProject.person.firstName",
        "acme.my_project.person.first_name"
    })
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void relaxedBinding(String propertyKey) {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty(propertyKey, false, true, null)),
            yaml("acme.my-project.person.first-name: example",
                ""
            )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1168")
    void exactMatch() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("acme.my-project.person.first-name", false, false, null)),
            yaml(
                """
                    acme.myProject.person.firstName: example
                    acme.my_project.person.first_name: example
                    acme.my-project.person.first-name: example
                    """,
                """
                    acme.myProject.person.firstName: example
                    acme.my_project.person.first_name: example
                    """
            )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1769")
    @Test
    void preservesOriginalIndentStructureOfExistingHierarchy() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("my.old.key", false, null, null)),
            yaml(
                """
                      my:
                        old:
                          key:
                            color: blue
                            style: retro
                        other:
                          key: qwe
                    """,
                """
                      my:
                        other:
                          key: qwe
                    """
            )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4204")
    @Test
    void preserveEmptySequencesWithOtherKeys() {
        rewriteRun(
            spec -> spec.recipe(new DeleteProperty("my.key", false, null, null)),
            yaml(
                """
                    my.key: qwe
                    seq: []
                    """,
                """
                    seq: []
                    """
            )
        );
    }
}
