package pl.allegro.tech.allwrite.runtime.util

import io.kotest.koin.KoinExtension
import io.mockk.mockkClass
import org.koin.core.component.KoinComponent
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.test.KoinTest
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.superclasses

/**
 * Creates Kotest extension for Koin with Mockk as mockProvider
 */
@Suppress("TestFunctionName")
fun KoinMockkExtension(vararg modules: Module) = KoinMockkExtension(modules.toList())

@Suppress("TestFunctionName", "ktlint:standard:function-naming")
fun KoinMockkExtension(modules: List<Module>) =
    KoinExtension(
        modules = modules.toList(),
        mockProvider = { klass -> mockkClass(klass) },
    )

/**
 * Declares [instance] as a bean and additionally binds it to all its direct superclasses (except from [Any])
 *
 * For example: [FakeRecipeSource] will be bound to [FakeRecipeSource] and [RecipeSource]
 */
inline fun <reified T> KoinTest.declareFake(instance: T) {
    getKoin().declare(
        instance = instance,
        secondaryTypes = T::class.superclasses.filter { it != Any::class },
        allowOverride = true,
    )
}

/**
 * As opposed to inject(), this delegate will resolve a bean everytime the property is accessed.
 * It is useful in tests, because every Kotest leaf test starts new Koin context,
 * so with inject() you may end up with one bean from previous context, and another from the current one.
 */
inline fun <reified T : Any> KoinComponent.injectEagerly(qualifier: Qualifier? = null, noinline parameters: ParametersDefinition? = null) =
    InjectDelegate(T::class, this, qualifier, parameters)

class InjectDelegate<T : Any>(
    private val clazz: KClass<T>,
    private val koinComponent: KoinComponent,
    private val qualifier: Qualifier?,
    private val parameters: ParametersDefinition?,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = koinComponent.getKoin().get(clazz, qualifier, parameters)
}
