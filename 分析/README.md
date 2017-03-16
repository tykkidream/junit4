JUnit分析说明
=========

JUnitCore
======
用JUnit4进行测试有两种方式分别是：

- 命令行方式：`java org.junit.runner.JUnitCore [java class...]`;
- 程序方式：直接调用`org.junit.runner.JUnitCore.runClass(Class<?>...clazz)`;

这两种测试的方法，相同的都是使用同一个类`JUnitCore`，`JUnitCore`是测试开始的主要入口。

主要的启动方法：

- void main(String... args)
- Result runClasses(Class<?>... classes)
- Result runClasses(Computer computer, Class<?>... classes)
- runMain(JUnitSystem system, String... args)
- Result run(Class<?>... classes)
- Result run(Computer computer, Class<?>... classes)
- Result run(Request request)
- Result run(junit.framework.Test test)
- Result run(Runner runner)

其中`Result run(Runner runner)`是真正开始测试的方法，其它方法都会交给这个方法处理。
而这个方法又会将执行交由参数runner对象的run方法处理。

所以执行器`Runner`对象会是主要负责测试的一个类。而`JUnitCore`是一个外观Facade模式，
对外提供一致的界面，同时支持运行JUnit 4或JUnit 3.8.x用例、通过命令行执行用例。

`JUnitCore`中主要涉及的类：

- Result：记录运行时的状态，比如所运行Test的数量，所忽略Test的数量，开始运行的时间。
- Runner：测试执行器，它说明了以什么样的方式执行测试用例。
- RunNotifier：运行时通知器，类似事件Bus。


Runner
======

测试执行器

主要的方法：

- run(RunNotifier notifier)

`JUnitCore`中最终调用的的就是`Runner`的`run(RunNotifier notifier)`方法。



ParentRunner
------------

BlockJUnit4ClassRunner
----------------------




