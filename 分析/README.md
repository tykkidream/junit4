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

```java
    public Result run(Runner runner) {
        Result result = new Result();
        RunListener listener = result.createListener();
        notifier.addFirstListener(listener);
        try {
            notifier.fireTestRunStarted(runner.getDescription());
            runner.run(notifier);
            notifier.fireTestRunFinished(result);
        } finally {
            removeListener(listener);
        }
        return result;
    }
```

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

```java
    public abstract void run(RunNotifier notifier);
```



ParentRunner
------------

```java
    @Override
    public void run(final RunNotifier notifier) {
        EachTestNotifier testNotifier = new EachTestNotifier(notifier,
                getDescription());
        try {
            // 将被测试的类的所有的动作封装成了Statement，包括beforeClass等方法。
            // 从这里也可以看出JUnit将测试相关的操作都理解为了Statement。
            Statement statement = classBlock(notifier);
            // 执行测试类上的整体的测试操作。
            statement.evaluate();
        } catch (AssumptionViolatedException e) {
            testNotifier.addFailedAssumption(e);
        } catch (StoppedByUserException e) {
            throw e;
        } catch (Throwable e) {
            testNotifier.addFailure(e);
        }
    }
```

```java
    protected Statement classBlock(final RunNotifier notifier) {
        // Statement是装饰(Decorator)模式，一个操作包一个操作，层层包。
        // 先创建被测试方法的Statement。
        Statement statement = childrenInvoker(notifier);
        if (!areAllChildrenIgnored()) {
            // 在当前的Statement上，包上BeforeClasses的操作，得到一个新的Statement。
            statement = withBeforeClasses(statement);
            // 在当前的Statement上，包上AfterClasses的操作，得到一个新的Statement。
            statement = withAfterClasses(statement);
            // 在当前的Statement上，包上ClassRules的操作，得到一个新的Statement。
            statement = withClassRules(statement);
        }
        return statement;
    }
```


```java
    protected Statement childrenInvoker(final RunNotifier notifier) {
        return new Statement() {
            @Override
            public void evaluate() {
                runChildren(notifier);
            }
        };
    }
```

```java
    private void runChildren(final RunNotifier notifier) {
        final RunnerScheduler currentScheduler = scheduler;
        try {
            for (final T each : getFilteredChildren()) {
                currentScheduler.schedule(new Runnable() {
                    public void run() {
                        ParentRunner.this.runChild(each, notifier);
                    }
                });
            }
        } finally {
            currentScheduler.finished();
        }
    }
```

```java
    protected abstract void runChild(T child, RunNotifier notifier);
```

BlockJUnit4ClassRunner
----------------------


```java
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        if (isIgnored(method)) {
            // 被忽略的测试方法
            notifier.fireTestIgnored(description);
        } else {
            // 正常可被测试的方法
            runLeaf(methodBlock(method), description, notifier);
        }
    }
```



