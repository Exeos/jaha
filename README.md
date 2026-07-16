# JAHA

JAHA (**JA**va **H**ooking **A**gent) is a Java library for building Java agents that hook methods at runtime while
preserving access to original method behavior.

![JAHA.png](images/JAHA.png)

## Requirements

- **Java 8** (project targets Java 8 for compatibility)
- **Zig installed** (required for cross-platform building)

---

## Example

Below is an example of hooking `Integer.toString()`.

### Agent entrypoint

```java
import java.lang.instrument.Instrumentation;

import me.exeos.jaha.Jaha;

public final class AgentMain {

    public static void premain(String args, Instrumentation inst) {
        // Register hook classes
        Jaha.register(ToStringHook.class);

        // Apply hooks
        Jaha.load(inst);
    }
}
```

### Hook class

```java
import me.exeos.jaha.Jaha;
import me.exeos.jaha.annotations.Apply;
import me.exeos.jaha.annotations.Hook;

@Hook(target = "java/lang/Integer")
public class ToStringHook {

    @Apply
    public String toString() {
        String original = (String) Jaha.callOriginalObjectMethod();
        return "This has been hooked: " + original;
    }
}

```

### Output

Let's say you attach the example to this Program:

```java
import java.util.Random;

public class Main {

    public static void main(String[] args) {
        System.out.println(new Integer(new Random().nextInt()));
    }
}
```

The result would be:

```
This has been hooked: -1604796949
```

---

## API

- `Jaha.applyHooks(Instrumentation inst)`  
  Applies all registered hooks

- `register(Class<?> hookSource)`  
  Registers a hook class

- `callOriginalVoidMethod(Object... params)`  
  Calls original method when return type is `void`.

- `callOriginalObjectMethod(Object... params)`  
  Calls original method when return type is an object. (cast the result)

- `callOriginalByteMethod(Object... params)`  
  Calls original method when return type is `byte`.

- `callOriginalShortMethod(Object... params)`  
  Calls original method when return type is `short`.

- `callOriginalIntMethod(Object... params)`  
  Calls original method when return type is `int`.

- `callOriginalLongMethod(Object... params)`  
  Calls original method when return type is `long`.

- `callOriginalFloatMethod(Object... params)`  
  Calls original method when return type is `float`.

- `callOriginalDoubleMethod(Object... params)`  
  Calls original method when return type is `double`.

- `callOriginalBooleanMethod(Object... params)`  
  Calls original method when return type is `boolean`.

- `callOriginalCharMethod(Object... params)`  
  Calls original method when return type is `char`.

---

## License

This project is [LICENSED](./LICENSE) under the **GPL-3.0**.

---
