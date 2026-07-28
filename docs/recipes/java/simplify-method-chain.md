### `pl.allegro.tech.allwrite.recipes.java.SimplifyMethodChain` { data-toc-label="SimplifyMethodChain" }

Enhanced version of OpenRewrite's `SimplifyMethodChain` with support for Kotlin.

Options:

| Name                 | Type           | Required | Description                                                                                                                                            |
|----------------------|----------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `methodPatternChain` | `List<String>` | Yes      | A list of method patterns that are called in sequence.                                                                                                 |
| `newMethodName`      | `String`       | Yes      | The method name that will replace the existing name. The new method name target is assumed to have the same arguments as the last method in the chain. |
| `matchOverrides`     | `Boolean`      | No       | When enabled, find methods that are overrides of the method pattern.                                                                                   |

Before:

```kotlin
fun getAuthorName(book: Book) = book.author.name
```

After:

```kotlin
fun getAuthorName(book: Book) = book.authorName
```

