package org.junit.runners.model;


/**
 * Represents one or more actions to be taken at runtime in the course
 * of running a JUnit test suite.
 *
 * <p>
 *     在运行期时，执行test case前可以插入一些用户动作，它就是描述这些动作的一个类。
 * </p>
 * <p>
 *     是个装饰(Decorator)模式，只有一个方法evaluate() 代表一个操作，继承此类，
 *     可以添加一些自定义的动作。
 * </p>
 * <p>
 *     譬如“@Before”是一个Statement，“@BeforeClass”也是一个Statement，
 *     “@Test”里面定义的expected和timeout都是Statement，
 *     当然被测试的方法本身也是一个Statement。
 * </p>
 *
 * @since 4.5
 */
public abstract class Statement {
    /**
     * Run the action, throwing a {@code Throwable} if anything goes wrong.
     */
    public abstract void evaluate() throws Throwable;
}