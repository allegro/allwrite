package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class AddNonNullableTypeBoundsToSpringRepositoriesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(AddNonNullableTypeBoundsToSpringRepositories())
            .withRecipeClasspath()
    }

    @Nested
    inner class BasicCases {

        @Test
        fun `should add Any bound to both type parameters of CrudRepository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T, ID> : CrudRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T : Any, ID : Any> : CrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not change when both type parameters already have Any bound`() {
            rewriteRun(
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T : Any, ID : Any> : CrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should add Any bound only to parameter that is missing it`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T : Any, ID> : CrudRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T : Any, ID : Any> : CrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }
    }

    @Nested
    inner class DifferentRepositoryTypes {

        @Test
        fun `should add Any bound for Repository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.Repository

                    interface Repo<T, ID> : Repository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.Repository

                    interface Repo<T : Any, ID : Any> : Repository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should add Any bound for ListCrudRepository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.ListCrudRepository

                    interface Repo<T, ID> : ListCrudRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.ListCrudRepository

                    interface Repo<T : Any, ID : Any> : ListCrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should add Any bound for PagingAndSortingRepository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.PagingAndSortingRepository

                    interface Repo<T, ID> : PagingAndSortingRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.PagingAndSortingRepository

                    interface Repo<T : Any, ID : Any> : PagingAndSortingRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should add Any bound for ListPagingAndSortingRepository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.ListPagingAndSortingRepository

                    interface Repo<T, ID> : ListPagingAndSortingRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.ListPagingAndSortingRepository

                    interface Repo<T : Any, ID : Any> : ListPagingAndSortingRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should add Any bound for ReactiveCrudRepository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.reactive.ReactiveCrudRepository

                    interface Repo<T, ID> : ReactiveCrudRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.reactive.ReactiveCrudRepository

                    interface Repo<T : Any, ID : Any> : ReactiveCrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should add Any bound for ReactiveSortingRepository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.reactive.ReactiveSortingRepository

                    interface Repo<T, ID> : ReactiveSortingRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.reactive.ReactiveSortingRepository

                    interface Repo<T : Any, ID : Any> : ReactiveSortingRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }
    }

    @Nested
    inner class ExtraTypeParameters {

        @Test
        fun `should only add Any bound to type parameters passed to repository supertype`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T, ID, Extra> : CrudRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.CrudRepository

                    interface Repo<T : Any, ID : Any, Extra> : CrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }
    }

    @Nested
    inner class ClassExtendingRepository {

        @Test
        fun `should add Any bound to class extending a repository`() {
            rewriteRun(
                kotlin(
                    before = """
                    import org.springframework.data.repository.CrudRepository

                    abstract class Repo<T, ID> : CrudRepository<T, ID>
                    """.trimIndent(),
                    after = """
                    import org.springframework.data.repository.CrudRepository

                    abstract class Repo<T : Any, ID : Any> : CrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }
    }

    @Nested
    inner class ExistingBounds {

        @Test
        fun `should not add Any bound when type parameter already has a different bound`() {
            rewriteRun(
                kotlin(
                    beforeAndAfter = """
                    import org.springframework.data.repository.CrudRepository
                    import java.io.Serializable

                    interface Repo<T : Serializable, ID : Any> : CrudRepository<T, ID>
                    """.trimIndent(),
                )
            )
        }

        @Test
        fun `should not modify interface that does not extend a repository`() {
            rewriteRun(
                kotlin(
                    beforeAndAfter = """
                    interface NotARepo<T, ID>
                    """.trimIndent(),
                )
            )
        }
    }
}
