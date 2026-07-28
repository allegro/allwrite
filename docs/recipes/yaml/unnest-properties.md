### `pl.allegro.tech.allwrite.recipes.yaml.UnnestProperties` { data-toc-label="UnnestProperties" }

Removes one level of nesting from a YAML mapping at a specified path, moving child entries up to the parent level.

Options:

| Name         | Type     | Required | Description                                        |
|--------------|----------|----------|----------------------------------------------------|
| `targetPath` | `String` | Yes      | Dot-separated path to the mapping entry to unnest. |

Before:
```yaml
spring:
  groovy:
    template:
      configuration:
        auto-indent: true
        auto-new-line: true
```

After (with `targetPath = spring.groovy.template.configuration`):
```yaml
spring:
  groovy:
    template:
      auto-indent: true
      auto-new-line: true
```

