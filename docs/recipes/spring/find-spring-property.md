### `pl.allegro.tech.allwrite.recipes.spring.FindSpringProperty` { data-toc-label="FindSpringProperty" }

Searches for a Spring property by key across `application*.properties`, `application*.yml`, and `application*.yaml` files. Supports relaxed binding and glob patterns for both key matching and profile filtering via file name suffix.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyKey` | `String` | Yes | The property key to search for. Compared using relaxed binding, supports glob. |
| `expectedValue` | `String` | Yes | The property value to match. If `null`, matches any value. |
| `fileNameSuffix` | `String` | No | Glob pattern for file name suffix, used to filter by Spring profile (e.g. `-integration`). |

