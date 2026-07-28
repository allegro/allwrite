### `pl.allegro.tech.allwrite.recipes.java.ChangeRecordField` { data-toc-label="ChangeRecordField" }

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

then has been changed to:
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

