### `pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKey` { data-toc-label="ChangeSpringPropertyKey" }

Renames a Spring property key across YAML, `.properties`, and other files (e.g. Markdown). For YAML and `.properties`, delegates to OpenRewrite's `ChangeSpringPropertyKey`. For other files, performs a regex find-and-replace supporting both `lower-hyphen` and `lowerCamel` case formats.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `oldKey` | `String` | Yes | The property key to rename. Supports glob. |
| `newKey` | `String` | Yes | The new name for the property key. |

Before (with `oldKey = "i18n.language-bundle.enabled"`, `newKey = "myapp.i18n.language-bundle.enabled"`):
```yaml
i18n:
  language-bundle:
    enabled: true
```

After:
```yaml
myapp.i18n.language-bundle.enabled: true
```
