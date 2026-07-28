### `pl.allegro.tech.allwrite.recipes.java.ReplaceFactoryWithConstructor` { data-toc-label="ReplaceFactoryWithConstructor" }

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


