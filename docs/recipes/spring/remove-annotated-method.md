### `pl.allegro.tech.allwrite.recipes.spring.RemoveAnnotatedMethod` { data-toc-label="RemoveAnnotatedMethod" }

Open class for recipes that remove methods annotated with a specific annotation. A method is removed when all the following are true:

- It has exactly one annotation matching `annotationName`
- It has no parameters
- Its return type matches `returnType`
- Its body is not complex - i.e., it only calls methods listed in `allowedBodyCalls` (an empty set means only constructor is allowed).

When a method is removed, its return type import is also removed. The annotation import is removed only if the annotation is not used elsewhere in the file.

This recipe is marked as internal — subclasses supply the concrete configuration.

Options:

| Name               | Type          | Required | Description                                                                                                                                                                            |
|--------------------|---------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `returnType`       | `String`      | Yes      | Fully qualified return type of the method to remove. If `null`, the recipe is a no-op.                                                                                                 |
| `annotationName`   | `String`      | Yes      | Fully qualified annotation name the method must have. If `null`, the recipe is a no-op.                                                                                                |
| `allowedBodyCalls` | `Set<String>` | No       | Simple method calls permitted in the method body. Any call to a method not in this set blocks removal. Defaults to empty set - only methods with no body calls are removed by default. |

Before (Kotlin, with `annotationName = "pl.allegro.example.SomeAnnotation"`, `returnType = "pl.allegro.example.SomeClassUsedAsABean"`, `allowedBodyCalls = setOf("allowedMethodForRemoval", "println")`):

```kotlin
import pl.allegro.example.SomeAnnotation

class SomeConfig {

    @SomeAnnotation
    fun someClassUsedAsABean(): SomeClassUsedAsABean {
        println("this println call is allowed")
        return SomeClassUsedAsABean().allowedMethodForRemoval()
    }

    @SomeAnnotation
    fun someAnotherClassUsedAsABean(): SomeAnotherClassUsedAsABean = SomeAnotherClassUsedAsABean.notAllowedMethodForRemoval()
}
```

After:

```kotlin
class SomeConfig {

    @SomeAnnotation
    fun someAnotherClassUsedAsABean(): SomeAnotherClassUsedAsABean = SomeAnotherClassUsedAsABean.notAllowedMethodForRemoval()
}
```
