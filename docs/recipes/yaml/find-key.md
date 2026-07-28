### `pl.allegro.tech.allwrite.recipes.yaml.FindKey` { data-toc-label="FindKey" }

A lighter and faster alternative to OpenRewrite's `FindKey`. Searches for a YAML key using simple case-insensitive matching instead of JsonPath. Intended for use as a precondition that fires when a YAML document contains a specified key.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `key` | `String` | No | The YAML key to search for (case-insensitive, dot-separated path). |

