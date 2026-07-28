### `pl.allegro.tech.allwrite.recipes.yaml.AddTopLevelLineBreaks` { data-toc-label="AddTopLevelLineBreaks" }

Ensures top-level YAML mapping entries are separated by blank lines for conventional formatting.

Before:
```yaml
server:
  port: 8080
spring:
  application:
    name: my-app
management:
  endpoints:
    enabled: true
```

After:
```yaml
server:
  port: 8080

spring:
  application:
    name: my-app

management:
  endpoints:
    enabled: true
```

