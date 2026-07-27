### `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyWithValue` { data-toc-label="DeleteSpringPropertyWithValue" }

Deletes a Spring property only if it has a specific value. Works across both YAML and `.properties` files.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyKey` | `String` | Yes | The property key to delete. |
| `propertyValue` | `String` | Yes | The exact value the property must have to be deleted. |

Before (with `propertyKey = "management.endpoint.configprops"`, `propertyValue = "true"`):
```yaml
management:
  endpoint:
    configprops: true
    health:
      enabled: true
```

After:
```yaml
management:
  endpoint:
    health:
      enabled: true
```

