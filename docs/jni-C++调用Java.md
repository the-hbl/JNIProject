# C++ Call Java in JNI Project

JNI 不仅支持 Java 调用 C/C++，也能实现 **C/C++ 主动调用 Java 的方法/访问属性/创建对象**，核心是通过 JNIEnv 指针操作 Java 类、对象、方法和属性。以下是 C++ 调用 Java 的完整流程、核心 API 解析和实战示例，基于你之前的 JNI 项目（`com.example.jnidemo`）展开。

### 核心原理

C++ 调用 Java 的本质是：通过 JNI 提供的 API（JNIEnv 方法），在 C++ 中：

1. 找到 Java 类（jclass）；

2. 获取 Java 方法/属性的 ID（jmethodID/jfieldID）；

3. 调用方法/访问属性（静态/实例方法均支持）；

4. 处理 Java 类型与 C++ 类型的转换。

### 前置准备（复用原有项目）

基于你之前的 `JNIUtils.java` 和 `Person.java`，补充一个供 C++ 调用的 Java 类/方法：

```Java
// app/src/main/java/com/example/jnidemo/JavaCalledByCpp.java
package com.example.jnidemo;

public class JavaCalledByCpp {
    // 1. 静态属性（供C++访问）
    public static String staticMsg = "Hello from Java Static";
    // 2. 实例属性（供C++访问）
    public String instanceMsg = "Hello from Java Instance";

    // 3. 静态方法（供C++调用）
    public static String staticMethod(String input) {
        return "Static Method Response: " + input;
    }

    // 4. 实例方法（供C++调用）
    public String instanceMethod(int num) {
        return "Instance Method Response: " + num * 10;
    }

    // 5. 无参构造方法（供C++创建对象）
    public JavaCalledByCpp() {}

    // 6. 带参构造方法（供C++创建对象）
    public JavaCalledByCpp(String customMsg) {
        this.instanceMsg = customMsg;
    }
}
```

### C++ 调用 Java 的核心场景（完整示例）

在 `native-lib.cpp` 中新增一个 Native 方法 `callJavaFromCpp`，该方法内部实现 C++ 调用 Java 的所有核心场景：

```C++
#include <jni.h>
#include <string>
#include <android/log.h> // 用于打印日志（可选）

// 日志宏定义（方便调试）
#define LOG_TAG "JNI_CALL_JAVA"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// 声明：C++调用Java的核心方法
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_jnidemo_JNIUtils_callJavaFromCpp(JNIEnv *env, jobject thiz) {
    // ======================================
    // 场景1：获取并修改Java静态属性
    // ======================================
    // 1. 找到Java类（参数：类的完整路径，注意用/替换.）
    jclass javaCls = env->FindClass("com/example/jnidemo/JavaCalledByCpp");
    if (javaCls == nullptr) {
        LOGD("Find Java class failed!");
        return env->NewStringUTF("Call Java Failed: Class Not Found");
    }

    // 2. 获取静态属性ID（参数：类、属性名、属性签名）
    // 签名规则：Ljava/lang/String; 对应String，I对应int，L包名/类名; 对应自定义类
    jfieldID staticMsgFid = env->GetStaticFieldID(javaCls, "staticMsg", "Ljava/lang/String;");
    if (staticMsgFid == nullptr) {
        LOGD("Find static field failed!");
        env->DeleteLocalRef(javaCls); // 释放局部引用，避免内存泄漏
        return env->NewStringUTF("Call Java Failed: Static Field Not Found");
    }

    // 3. 获取静态属性值
    jstring staticMsgJstr = (jstring) env->GetStaticObjectField(javaCls, staticMsgFid);
    const char *staticMsgCstr = env->GetStringUTFChars(staticMsgJstr, nullptr);
    LOGD("Java Static Field: %s", staticMsgCstr); // 打印：Hello from Java Static

    // 4. 修改静态属性值
    jstring newStaticMsg = env->NewStringUTF("Modified by C++");
    env->SetStaticObjectField(javaCls, staticMsgFid, newStaticMsg);

    // 释放资源
    env->ReleaseStringUTFChars(staticMsgJstr, staticMsgCstr);
    env->DeleteLocalRef(staticMsgJstr);
    env->DeleteLocalRef(newStaticMsg);

    // ======================================
    // 场景2：调用Java静态方法
    // ======================================
    // 1. 获取静态方法ID（参数：类、方法名、方法签名）
    // 方法签名规则：(参数类型)返回类型，(Ljava/lang/String;)Ljava/lang/String; 表示入参String，返回String
    jmethodID staticMethodMid = env->GetStaticMethodID(javaCls, "staticMethod", "(Ljava/lang/String;)Ljava/lang/String;");
    if (staticMethodMid == nullptr) {
        LOGD("Find static method failed!");
        env->DeleteLocalRef(javaCls);
        return env->NewStringUTF("Call Java Failed: Static Method Not Found");
    }

    // 2. 调用静态方法（参数：类、方法ID、入参）
    jstring inputJstr = env->NewStringUTF("Hello from C++");
    jstring staticResultJstr = (jstring) env->CallStaticObjectMethod(javaCls, staticMethodMid, inputJstr);
    const char *staticResultCstr = env->GetStringUTFChars(staticResultJstr, nullptr);
    LOGD("Java Static Method Result: %s", staticResultCstr); // 打印：Static Method Response: Hello from C++

    // 释放资源
    env->ReleaseStringUTFChars(staticResultJstr, staticResultCstr);
    env->DeleteLocalRef(inputJstr);
    env->DeleteLocalRef(staticResultJstr);

    // ======================================
    // 场景3：创建Java对象 + 调用实例方法
    // ======================================
    // 方式1：调用无参构造方法创建对象
    // 1. 获取无参构造方法ID（构造方法名固定为 <init>，签名：()V 表示无参，返回void）
    jmethodID emptyCtorMid = env->GetMethodID(javaCls, "<init>", "()V");
    if (emptyCtorMid == nullptr) {
        LOGD("Find empty constructor failed!");
        env->DeleteLocalRef(javaCls);
        return env->NewStringUTF("Call Java Failed: Empty Constructor Not Found");
    }
    // 2. 创建对象（调用构造方法）
    jobject javaObj = env->NewObject(javaCls, emptyCtorMid);

    // 方式2：调用带参构造方法创建对象（可选）
    // jmethodID paramCtorMid = env->GetMethodID(javaCls, "<init>", "(Ljava/lang/String;)V");
    // jobject javaObj = env->NewObject(javaCls, paramCtorMid, env->NewStringUTF("Custom Msg from C++"));

    // 3. 获取实例属性ID并读取
    jfieldID instanceMsgFid = env->GetFieldID(javaCls, "instanceMsg", "Ljava/lang/String;");
    jstring instanceMsgJstr = (jstring) env->GetObjectField(javaObj, instanceMsgFid);
    const char *instanceMsgCstr = env->GetStringUTFChars(instanceMsgJstr, nullptr);
    LOGD("Java Instance Field: %s", instanceMsgCstr); // 打印：Hello from Java Instance

    // 4. 获取实例方法ID并调用
    jmethodID instanceMethodMid = env->GetMethodID(javaCls, "instanceMethod", "(I)Ljava/lang/String;");
    if (instanceMethodMid == nullptr) {
        LOGD("Find instance method failed!");
        env->DeleteLocalRef(javaCls);
        env->DeleteLocalRef(javaObj);
        return env->NewStringUTF("Call Java Failed: Instance Method Not Found");
    }
    // 调用实例方法（参数：对象、方法ID、入参）
    jstring instanceResultJstr = (jstring) env->CallObjectMethod(javaObj, instanceMethodMid, 100);
    const char *instanceResultCstr = env->GetStringUTFChars(instanceResultJstr, nullptr);
    LOGD("Java Instance Method Result: %s", instanceResultCstr); // 打印：Instance Method Response: 1000

    // ======================================
    // 资源释放（关键：避免JNI引用泄漏）
    // ======================================
    env->ReleaseStringUTFChars(instanceMsgJstr, instanceMsgCstr);
    env->ReleaseStringUTFChars(instanceResultJstr, instanceResultCstr);
    env->DeleteLocalRef(instanceMsgJstr);
    env->DeleteLocalRef(instanceResultJstr);
    env->DeleteLocalRef(javaObj);
    env->DeleteLocalRef(javaCls);

    // 返回最终结果给Java
    return env->NewStringUTF("C++ Call Java Success!");
}
```

### 步骤1：在Java中声明Native方法

在 `JNIUtils.java` 中新增 Native 方法，用于触发 C++ 调用 Java 的逻辑：

```Java
package com.example.jnidemo;

public class JNIUtils {
    static {
        System.loadLibrary("native-lib");
    }

    // 原有方法...
    public native String getNativeString();
    public native int add(int a, int b);
    public native String getPersonInfo(Person person);

    // 新增：触发C++调用Java的方法
    public native String callJavaFromCpp();
}
```

### 步骤2：关键API解析（C++调用Java的核心）

|API 方法|作用|
|---|---|
|`env->FindClass("包名/类名")`|查找Java类（返回jclass），包名用 `/` 替换 `.`（如 `com/example/jnidemo/JavaCalledByCpp`）|
|`env->GetStaticFieldID`|获取静态属性ID（参数：jclass、属性名、属性签名）|
|`env->GetStaticMethodID`|获取静态方法ID（参数：jclass、方法名、方法签名）|
|`env->GetFieldID`|获取实例属性ID（参数：jclass、属性名、属性签名）|
|`env->GetMethodID`|获取实例方法/构造方法ID（构造方法名固定为 `<init>`）|
|`env->GetStaticXXXField`|获取静态属性值（XXX：Object/Int/String等）|
|`env->SetStaticXXXField`|修改静态属性值|
|`env->GetXXXField`|获取实例属性值|
|`env->SetXXXField`|修改实例属性值|
|`env->CallStaticXXXMethod`|调用静态方法（XXX：Object/Int/Void等）|
|`env->CallXXXMethod`|调用实例方法|
|`env->NewObject`|创建Java对象（调用构造方法）|
### 步骤3：方法/属性签名生成（关键避坑）

JNI 方法/属性的签名必须严格匹配，手动编写易出错，可通过 `javap` 命令自动生成：

1. 编译 Java 类：

    ```Bash
    cd app/src/main/java
    javac com/example/jnidemo/JavaCalledByCpp.java
    ```

2. 生成签名：

    ```Bash
    javap -s -p com.example.jnidemo.JavaCalledByCpp
    ```

输出示例（关键看 `Descriptor` 列，即签名）：

```Plain Text
Compiled from "JavaCalledByCpp.java"
public class com.example.jnidemo.JavaCalledByCpp {
  public static java.lang.String staticMsg;
    descriptor: Ljava/lang/String;

  public java.lang.String instanceMsg;
    descriptor: Ljava/lang/String;

  public static java.lang.String staticMethod(java.lang.String);
    descriptor: (Ljava/lang/String;)Ljava/lang/String;

  public java.lang.String instanceMethod(int);
    descriptor: (I)Ljava/lang/String;

  public com.example.jnidemo.JavaCalledByCpp();
    descriptor: ()V

  public com.example.jnidemo.JavaCalledByCpp(java.lang.String);
    descriptor: (Ljava/lang/String;)V
}
```

### 步骤4：在Java中调用Native方法，触发C++调用Java

在 `MainActivity.java` 中调用新增的 `callJavaFromCpp` 方法：

```Java
package com.example.jnidemo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JNIUtils jniUtils = new JNIUtils();
        // 触发C++调用Java逻辑
        String result = jniUtils.callJavaFromCpp();
        TextView tv4 = findViewById(R.id.tv_call_java);
        tv4.setText(result); // 显示：C++ Call Java Success!

        // 验证Java静态属性是否被C++修改
        TextView tv5 = findViewById(R.id.tv_static_msg);
        tv5.setText("Modified Static Msg: " + JavaCalledByCpp.staticMsg); // 显示：Modified by C++
    }
}
```

### 关键注意事项

1. **引用释放**：

    - jclass/jobject/jstring 等属于 JNI 局部引用，必须用 `env->DeleteLocalRef` 释放，否则会导致内存泄漏；

    - `GetStringUTFChars` 获取的 C 字符串，需用 `ReleaseStringUTFChars` 释放。

2. **异常处理**：

    - C++ 调用 Java 时若出现异常（如方法不存在、空指针），需用 `env->ExceptionCheck()` 检查：

        ```C++
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe(); // 打印异常信息
            env->ExceptionClear(); // 清除异常
            return env->NewStringUTF("Call Java Failed: Exception");
        }
        ```

3. **线程安全**：

    - JNIEnv 指针仅在当前线程有效，若在 C++ 子线程调用 Java，需先通过 `JavaVM*` 获取当前线程的 JNIEnv；

    - 调用 Java 方法时，需确保在 Android 主线程（UI 操作）或子线程（普通逻辑）。

4. **权限问题**：

    - C++ 可调用 Java 的 `public` 方法/属性，若需调用 `private`，需将 Java 方法/属性改为 `public`，或通过反射（JNI 也支持反射调用私有成员，但不推荐）。

### 核心流程总结

C++ 调用 Java 的完整链路：

```Plain Text
Java调用Native方法 → C++通过JNIEnv找到Java类 → 获取方法/属性ID → 调用方法/访问属性 → 返回结果给Java
```

通过以上步骤，你可以实现 C++ 对 Java 类、对象、方法、属性的全维度操作，覆盖静态/实例、读/写、创建对象等所有核心场景。

> 