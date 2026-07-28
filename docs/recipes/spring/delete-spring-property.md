### `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringProperty` { data-toc-label="DeleteSpringProperty" }

Deletes a Spring property by key from both YAML and `.properties` files.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyKey` | `String` | Yes | The property key to delete. |

Before (with `propertyKey = "myapp.isolated-environment"`):
```yaml
myapp:
  isolated-environment:
    nested-object:
      scalar: 123
      list:
      - a
      - b
  test:
    123
```

After:
```yaml
myapp:
  test:
    123
```

