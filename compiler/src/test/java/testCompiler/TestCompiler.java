package testCompiler;

import org.junit.*;
import compiler.Compiler;

public class TestCompiler {

    private void run(String filename) {
        Compiler.main(new String[]{filename});
    }

    // Good files

    @Test
    public void testEmpty() {
        run("src/test/resources/empty");
    }

    @Test
    public void testAnd() {
        run("src/test/resources/and");
    }

    @Test
    public void testLatch() {
        run("src/test/resources/latch");
    }

    @Test
    public void testAssignVariable() {
        run("src/test/resources/assign_variable");
    }

    /**
     * Treat an incorrect number of args as having the rest be undefined
     */
    @Test
    public void testIncorrectArgs() {
        run("src/test/resources/incorrect_args");
    }

    // Bad files

    @Test(expected=RuntimeException.class)
    public void testDuplicateFunction() {
        run("src/test/resources/duplicate_function");
    }

    @Test(expected=RuntimeException.class)
    public void testDuplicateSymbol() {
        run("src/test/resources/duplicate_symbol");
    }

    @Test(expected=RuntimeException.class)
    public void testInvalidAssign() {
        run("src/test/resources/invalid_assign");
    }
}