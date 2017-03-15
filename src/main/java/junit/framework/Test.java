package junit.framework;

/**
 * A <em>Test</em> can be run and collect its results.
 *
 * <p>
 *     ??????????
 * </p>
 *
 * @see TestResult
 */
public interface Test {
    /**
     * Counts the number of test cases that will be run by this test.
     *
     * <p>
     *     ????????????????
     * </p>
     */
    public abstract int countTestCases();

    /**
     * Runs a test and collects its result in a TestResult instance.
     *
     * <p>
     *     ?????
     * </p>
     */
    public abstract void run(TestResult result);
}