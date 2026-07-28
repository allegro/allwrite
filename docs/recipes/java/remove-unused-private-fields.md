### `pl.allegro.tech.allwrite.recipes.java.RemoveUnusedPrivateFields` { data-toc-label="RemoveUnusedPrivateFields" }

Enhanced version of OpenRewrite's `RemoveUnusedPrivateFields` with additional `onlyRemoveFieldsOfType` parameter.

Options:

| Name                     | Type       | Required | Description                          |
|--------------------------|------------|----------|--------------------------------------|
| `onlyRemoveFieldsOfType` | `String[]` | No       | Array of fully-qualified class names |

Before:

```java
class Example {
    private com.example.Foo unusedFoo;
    private com.example.Bar unusedBar;
}
```

After (with `onlyRemoveFieldsOfType` = `[com.example.Foo]`)

```java
class Example {
    private com.example.Bar unusedBar;
}
```

