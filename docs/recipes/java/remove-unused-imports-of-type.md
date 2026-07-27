### `pl.allegro.tech.allwrite.recipes.java.RemoveUnusedImportsOfType` { data-toc-label="RemoveUnusedImportsOfType" }

Removes unused imports, but only of given type.

Options:

| Name    | Type       | Required | Description                          |
|---------|------------|----------|--------------------------------------|
| `types` | `String[]` | No       | Array of fully-qualified class names |


Before:

```java
import com.example.Foo; // unused
import com.example.Bar; // unused
```

After (with `types` = `[com.example.Foo]`):

```java
import com.example.Bar; // unused
```

