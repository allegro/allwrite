# Recipes

The following list covers all custom recipes provided by `allwrite`.

## YAML

### `pl.allegro.tech.allwrite.recipes.yaml.ExpandMappings`

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

### `pl.allegro.tech.allwrite.recipes.yaml.UnnestProperties`

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

### `pl.allegro.tech.allwrite.recipes.yaml.AddTopLevelLineBreaks`

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

### `pl.allegro.tech.allwrite.recipes.yaml.DeleteProperty`

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

### `pl.allegro.tech.allwrite.recipes.yaml.FindKey`

A lighter and faster alternative to OpenRewrite's `FindKey`. Searches for a YAML key using simple case-insensitive matching instead of JsonPath. Intended for use as a precondition that fires when a YAML document contains a specified key.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `key` | `String` | No | The YAML key to search for (case-insensitive, dot-separated path). |

### `pl.allegro.tech.allwrite.recipes.yaml.YamlEntryHasValue`

Precondition recipe that matches YAML entries at a given JSON path having a specific value.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `key` | `String` | Yes | JSON path expression to locate the entry. |
| `expectedValue` | `String` | Yes | The value the entry must have to match. |

---

## Gradle

### `pl.allegro.tech.allwrite.recipes.gradle.AddGradleDependency`

A two-pass scanning recipe that adds a dependency to a Gradle project. In the scan phase, it parses the TOML version catalog (`gradle/libs.versions.toml`) and discovers module roots. In the transform phase, it adds the library entry to the version catalog and a dependency reference in `build.gradle(.kts)`.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `configuration` | `String` | Yes | Gradle configuration (e.g. `implementation`, `testImplementation`). |
| `groupId` | `String` | Yes | Dependency group ID. |
| `artifactId` | `String` | Yes | Dependency artifact ID. |
| `version` | `String` | No | Dependency version. Used when no version catalog is present. |
| `versionCatalogName` | `String` | No | Alias for the dependency in the version catalog. Auto-generated from coordinates if not specified. |

Before (with `configuration = "testRuntimeOnly"`, `groupId = "org.junit.platform"`, `artifactId = "junit-platform-launcher"`):

`gradle/libs.versions.toml`:
```toml
[libraries]
mylib-starter = { group = "com.example.lib", name = "mylib-starter" }
```

`build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.test)
}
```

After:

`gradle/libs.versions.toml`:
```toml
[libraries]
mylib-starter = { group = "com.example.lib", name = "mylib-starter" }
junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
```

`build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

### `pl.allegro.tech.allwrite.recipes.gradle.UpdateGradleDependency `

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

## Java / Kotlin

### `pl.allegro.tech.allwrite.recipes.java.ChangeType`

Enhanced version of the OpenRewrites `ChangeType` with additional support for renaming variables.

Options:

| Name                        | Type      | Required | Description                                                                                                                                                                             |
|-----------------------------|-----------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `oldFullyQualifiedTypeName` | `String`  | Yes      | Fully-qualified class name of the original type.                                                                                                                                        |
| `newFullyQualifiedTypeName` | `String`  | Yes      | Fully-qualified class name of the replacement type, or the name of a primitive such as "int". The `OuterClassName$NestedClassName` naming convention should be used for nested classes. |
| `ignoreDefinition`          | `Boolean` | No       | When set to `true` the definition of the old type will be left untouched. This is useful when you're replacing usage of a class but don't want to rename it.                            |


Before:
```java
public class OldType {}

public class Main {
    static void main() {
        OldType oldType = new OldType();
    }
}
```

After:
```java
public class NewType {}

public class Main {
    static void main() {
        // the vanilla OpenRewrite would keep 'oldType' variable name
        NewType newType = new NewType();
    }
}
```

### `pl.allegro.tech.allwrite.recipes.java.ChangeRecordField`

Renames a field on a Java record type across all usages, including field access expressions and accessor method invocations.

Options:

| Name               | Type     | Required | Description                              |
|--------------------|----------|----------|------------------------------------------|
| `declaringTypeFqn` | `String` | Yes      | Fully qualified name of the record type. |
| `oldFieldName`     | `String` | Yes      | Current field name.                      |
| `newFieldName`     | `String` | Yes      | New field name.                          |

Given third-party class:
```java
public record SomeRecord(String oldName) { }
```

than has been changed to:
```java
public record SomeRecord(String newName) { }
```

#### In Java

Before (with `declaringTypeFqn = "com.example.SomeRecord"`, `oldFieldName = "oldName"`, `newFieldName = "newName"`):

```java
class Foo {
    String foo(SomeRecord someRecord) {
        return someRecord.oldName();
    }
}
```

After:
```java
class Foo {
    String foo(SomeRecord someRecord) {
        return someRecord.newName();
    }
}
```

#### In Kotlin

Before (with `declaringTypeFqn = "com.example.SomeRecord"`, `oldFieldName = "oldName"`, `newFieldName = "newName"`):

```kotlin
fun foo(someRecord: SomeRecord): String {
    return someRecord.oldName
}
```

After:
```kotlin
fun foo(someRecord: SomeRecord): String {
    return someRecord.newName
}
```

### `pl.allegro.tech.allwrite.recipes.java.SimplifyMethodChain`

Enhanced version of OpenRewrite's `SimplifyMethodChain` with support for Kotlin.

Options:

| Name                 | Type           | Required | Description                                                                                                                                            |
|----------------------|----------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `methodPatternChain` | `List<String>` | Yes      | A list of method patterns that are called in sequence.                                                                                                 |
| `newMethodName`      | `String`       | Yes      | The method name that will replace the existing name. The new method name target is assumed to have the same arguments as the last method in the chain. |
| `matchOverrides`     | `Boolean`      | No       | When enabled, find methods that are overrides of the method pattern.                                                                                   |

Before:

```kotlin
fun getAuthorName(book: Book) = book.author.name
```

After:

```kotlin
fun getAuthorName(book: Book) = book.authorName
```

### `pl.allegro.tech.allwrite.recipes.java.RemoveUnusedPrivateFields`

Enhanced version of OpenRewrite's `RemoveUnusedPrivateFields` with additional `onlyRemoveFieldsOfType` parameter.

Options:

| Name                     | Type       | Required | Description                          |
|--------------------------|------------|----------|--------------------------------------|
| `onlyRemoveFieldsOfType` | `String[]` | No       | Array of fully-qualified class names |

Before:

```java
class Example {
    private com.example.Foo unusedFoo;
    private com.example.Bar unusedBar;
}
```

After (with `onlyRemoveFieldsOfType` = `[com.example.Foo]`)

```java
class Example {
    private com.example.Bar unusedBar;
}
```

### `pl.allegro.tech.allwrite.recipes.java.RemoveUnusedImportsOfType`

Removes unused imports, but only of given type.

Options:

| Name    | Type       | Required | Description                          |
|---------|------------|----------|--------------------------------------|
| `types` | `String[]` | No       | Array of fully-qualified class names |


Before:

```java
import com.example.Foo; // unused
import com.example.Bar; // unused
```

After (with `types` = `[com.example.Foor]`):

```java
import com.example.Bar; // unused
```

### `pl.allegro.tech.allwrite.recipes.java.ReplaceFactoryWithConstructor`

Replaces factory method invocations with direct constructor calls. Handles both `new Factory().create(args)` and identifier-based factory invocations. Automatically manages import changes.

Options:

| Name                      | Type     | Required | Description                                           |
|---------------------------|----------|----------|-------------------------------------------------------|
| `fullyQualifiedTypeName`  | `String` | Yes      | Fully qualified name of the target type to construct. |
| `factoryClassNamePattern` | `String` | Yes      | Regex pattern matching the factory class name.        |

Before (with `fullyQualifiedTypeName = "com.example.MyClass"`, `factoryClassNamePattern = "MyClassFactory"`):
```java
import com.example.MyClassFactory;

class Foo {
    void bar() {
        MyClass obj = new MyClassFactory().create(arg1, arg2);
    }
}
```

After:
```java
import com.example.MyClass;

class Foo {
    void bar() {
        MyClass obj = new MyClass(arg1, arg2);
    }
}
```

---

## Spring

### `pl.allegro.tech.allwrite.recipes.spring.FindSpringProperty`

Searches for a Spring property by key across `application*.properties`, `application*.yml`, and `application*.yaml` files. Supports relaxed binding and glob patterns for both key matching and profile filtering via file name suffix.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyKey` | `String` | Yes | The property key to search for. Compared using relaxed binding, supports glob. |
| `expectedValue` | `String` | Yes | The property value to match. If `null`, matches any value. |
| `fileNameSuffix` | `String` | No | Glob pattern for file name suffix, used to filter by Spring profile (e.g. `-integration`). |

### `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringProperty`

Deletes a Spring property by key from both YAML and `.properties` files.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyKey` | `String` | Yes | The property key to delete. |

Before (with `propertyKey = "myapp.isolated-environment"`):
```yaml
myapp:
  isolated-environment:
    nested-object:
      scalar: 123
      list:
      - a
      - b
  test:
    123
```

After:
```yaml
myapp:
  test:
    123
```

### `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyWithValue`

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

### `pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyFromSpringAnnotations`

Removes property entries from `@SpringBootTest(properties = ...)` and `@TestPropertySource(properties = ...)` annotations in Java source code. Supports glob matching on property names with relaxed binding.

Options:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `propertyName` | `String` | Yes | The property key to remove. Supports glob (e.g. `management.metrics.binders.*.enabled`). |

Before (with `propertyName = "myapp.test"`):
```java
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "server.port=8080",
  "myapp.test=1",
  "myapp.best=2"
})
class Example {}
```

After:
```java
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "server.port=8080",
  "myapp.best=2"
})
class Example {}
```

### `pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKey`

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

### `pl.allegro.tech.allwrite.recipes.spring.RenameTaskExecutorBean`

Adds `@Qualifier("applicationTaskExecutor")` to `TaskExecutor` injection points (constructor parameters, `@Bean` method parameters, and `@Autowired` fields) to align with the [Spring Boot 3.5](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.5-Release-Notes#auto-configured-taskexecutor-names) bean naming change. Skips projects that define their own custom `taskExecutor` bean.

Before:
```java
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MyService {
    public MyService(TaskExecutor taskExecutor) {}
}
```

After:
```java
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class MyService {
    public MyService(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {}
}
```
