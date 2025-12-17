# 1. 指定CMake最低版本（需和AS安装的CMake版本匹配）
cmake_minimum_required(VERSION 3.22.1)

# 2. 定义项目名称（自定义）
project("jnidemo")

# 3. 创建共享库（.so库）
# add_library(库名 类型 源码文件)
# SHARED：动态库（.so），STATIC：静态库（.a）
add_library(
        native-lib # 输出库名，对应Java中System.loadLibrary("native-lib")
        SHARED     # 动态库
        native-lib.cpp # 要编译的C/C++文件（多个文件用空格分隔）
)

# 4. 链接NDK内置库（可选，比如log库用于C/C++打印日志）
find_library(
        log-lib # 给库起别名
        log     # NDK内置的log库（android/log.h）
)

# 5. 链接依赖库（将log库链接到native-lib）
target_link_libraries(
        native-lib
        ${log-lib}
)