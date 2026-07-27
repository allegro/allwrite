### `pl.allegro.tech.allwrite.recipes.java.ChangeType` { data-toc-label="ChangeType" }

Enhanced version of OpenRewrite's `ChangeType` with additional support for renaming variables.

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

