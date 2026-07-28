### `pl.allegro.tech.allwrite.recipes.yaml.ExpandMappings` { data-toc-label="ExpandMappings" }

Transforms flat/collapsed YAML properties into a hierarchical structure and merges duplicate paths.

Options:

| Name       | Type           | Required | Description                                                                                                              |
|------------|----------------|----------|--------------------------------------------------------------------------------------------------------------------------|
| `prefix`   | `String`       | No       | Only transform entries matching this prefix. Must match key parts exactly (e.g. `myapp` will not match `myapplication`). |
| `excludes` | `List<String>` | No       | Do not transform entries matching these prefixes.                                                                        |

Before:
```yaml
myapp.metrics.graphite.enabled: true
myapp.metrics.graphite:
  host: localhost
  port: 2003
myapp:
  i18n:
    enabled: true
```

After:
```yaml
myapp:
  metrics:
    graphite:
      enabled: true
      host: localhost
      port: 2003
  i18n:
    enabled: true
```

