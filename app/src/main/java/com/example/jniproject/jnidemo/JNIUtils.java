package com.example.jniproject.jnidemo;

/**
 * ClassName: JNIUtils
 * Create by: huangbenlu
 * Create on: 2025/12/16 19:29
 * Description:
 */
public class JNIUtils {
    static {
        System.loadLibrary("native-lib");
    }

    //返回字符串
    public native String getNativeString();

    //加法计算
    public native int add(int a, int b);

    //传递对象
    public native String getPersonInfo(Person person);

    class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // 必须提供getter，C/C++通过JNI调用
        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }


}
