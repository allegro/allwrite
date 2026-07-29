### Spring Boot upgrades { data-toc-label="Spring Boot upgrades" }

The Spring Boot upgrade recipes migrate between consecutive supported Spring Boot versions. Each recipe applies OpenRewrite's migration for its target version without rerunning earlier upgrade recipes.

| From | To |
|------|----|
| 1.0 | 2.0 |
| 2.0 | 2.1 |
| 2.1 | 2.2 |
| 2.2 | 2.3 |
| 2.3 | 2.4 |
| 2.4 | 2.5 |
| 2.5 | 2.6 |
| 2.6 | 2.7 |
| 2.7 | 3.0 |
| 3.0 | 3.1 |
| 3.1 | 3.2 |
| 3.2 | 3.3 |
| 3.3 | 3.4 |
| 3.4 | 3.5 |
| 3.5 | 4.0 |

Run an upgrade by specifying its source and target versions:

```bash
allwrite run springBoot/upgrade 3.5 4.0
```

The Spring Boot 4.0 upgrade additionally applies allwrite-specific migrations for MongoDB properties, test client types and dependencies, Groovy, Spock, and Testcontainers.
