### `pl.allegro.tech.allwrite.recipes.yaml.DeleteProperty` { data-toc-label="DeleteProperty" }

Enhanced version of OpenRewrite's `DeleteProperty` that:
- allows deletion of properties from documents with anchors
- preserves comments of deleted entries when needed

Options:

| Name             | Type      | Required | Description                                                                                                                                                                                                                                                                                                         |
|------------------|-----------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `propertyKey`    | `String`  | Yes      | The key to be deleted.                                                                                                                                                                                                                                                                                              |
| `coalesce`       | `Boolean` | No       | Simplify nested map hierarchies into their simplest dot separated property form.                                                                                                                                                                                                                                    |
| `relaxedBinding` | `Boolean` | No       | Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) rules. Defaults to `true`. If you want to use exact matching in your search, set this to `false`. |
| `filePattern`    | `String`  | No       | A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.                                                                                                                                                                                                    |

Before:

```yaml
server.port: 8080 # comment should stay
management.server.port: 8081 # comment should disappear
smth-else: 1
```

After (with `propertyKey` = `management.server.port`):

```yaml
server.port: 8080 # comment should stay
smth-else: 1
```

