### `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyFromSpringAnnotations` { data-toc-label="DeleteSpringPropertyFromSpringAnnotations" }

Removes property entries from `@SpringBootTest(properties = ...)` and `@TestPropertySource(properties = ...)` annotations in Java source code. Supports glob matching on property names with relaxed binding.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyName` | `String` | Yes | The property key to remove. Supports glob (e.g. `management.metrics.binders.*.enabled`). |

Before (with `propertyName = "myapp.test"`):
```java
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "server.port=8080",
  "myapp.test=1",
  "myapp.best=2"
})
class Example {}
```

After:
```java
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "server.port=8080",
  "myapp.best=2"
})
class Example {}
```

