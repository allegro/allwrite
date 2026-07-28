### `pl.allegro.tech.allwrite.recipes.gradle.UpdateGradleDependency` { data-toc-label="UpdateGradleDependency" }

Updates dependency versions using regular expressions. Converts build files to plain text and applies regex-based replacements. Supports dependency declarations in multiple formats:

- String notation: `classpath("GROUP:ID:1.0.0")`
- Positional arguments: `classpath("GROUP", "ID", "1.0.0")`
- Groovy map notation: `classpath group: 'GROUP', name: 'ID', version: '1.0.0'`
- Kotlin named arguments: `classpath(group = "GROUP", name = "ID", version = "1.0.0")`
- **TOML version catalog entries**
- Versions declared in variables

Options:

| Name                                  | Type           | Required | Description                                                                                   |
|---------------------------------------|----------------|----------|-----------------------------------------------------------------------------------------------|
| `groupId`                             | `String`       | Yes      | Dependency group ID to match.                                                                 |
| `artifactId`                          | `String`       | Yes      | Dependency artifact ID to match.                                                              |
| `targetVersion`                       | `String`       | Yes      | The new version to set.                                                                       |
| `sourceVersionPattern`                | `String`       | No       | Regex pattern for the current version. Defaults to `\d+.\d+.\d+`.                             |
| `filePatterns`                        | `List<String>` | No       | Glob patterns for files to scan. Defaults to `*.gradle`, `*.gradle.kts`, and `gradle/*.toml`. |

Before (with `groupId = "com.example"`, `artifactId = "some-dependency"`, `targetVersion = "2.0.0"`):
```groovy
classpath("com.example:some-dependency:1.0.0")
```

After:
```groovy
classpath("com.example:some-dependency:2.0.0")
```

---


