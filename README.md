jretty-server
=====================

run Java war/jar on Android system！ ！！！  

一个Android版本的Java Container（类似于Tomcat），能够**让原生Android系统直接运行jar/war项目**！！！！  
> 该项目一直在维护，it is a long-term maintenance project, and tested from Android 4. x to the latest Android 13.

### 核心技术原理（一）
- 1、将classes编译为 Android JVM（Dalvik/ART）的字节码文件（Java Source->.class->.dex）   
- 2、实现基于Android JVM的classloader（类似于通过tomcat的Catalina ClassLoader）   

这样就能够运行简单Java程序了，但是要运行项目工程，还远远不够，因为类属于Spring/Struts/Mybatis等等框架的功能，没有考虑过在Android JVM上运行，所以得基于框架层面改造。  

但是，要改造Spring这样复杂的框架，工作量太大了，所以，要在原生Android系统上运行Spring只是个理想、并不现实。  

### 核心技术原理（二） 
- 1、基于<a href="https://github.com/zollty-org/zollty-mvc" target="_blank">ZolltyMVC</a>运行中小型Java工程项目，ZolltyMVC是一个轻量级MVC框架，麻雀虽小、五脏俱全，其1.7版本已经支持运行在jretty-server上！
