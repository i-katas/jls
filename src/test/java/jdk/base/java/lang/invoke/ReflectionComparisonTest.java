package jdk.base.java.lang.invoke;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

public class ReflectionComparisonTest {

    private final Method value;

    public ReflectionComparisonTest() throws NoSuchMethodException {
        value = String.class.getDeclaredMethod("value");
    }

    @Test
    public void reflectiveMethodAccessCheckingIsPerformedEveryTimeIsInvoked() throws Throwable {
        ThrowingRunnable test = () -> value.invoke("foo");
        assertThrows("must raise exception when access an inaccessible method", IllegalAccessException.class, test);

        value.setAccessible(true);
        assertRunNormally("muse be accessible", test);

        value.setAccessible(false);
        assertThrows("must check accessibility every time", IllegalAccessException.class, test);
    }

    @Test
    public void methodHandleAccessCheckingIsPerformedWhenCreated() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        assertThrows("must check accessibility on creation", IllegalAccessException.class, () -> lookup.unreflect(value));

        value.setAccessible(true);
        MethodHandle valueHandle = lookup.unreflect(value);
        ThrowingRunnable test = () -> valueHandle.invoke("foo");
        assertRunNormally("must be accessible", test);

        value.setAccessible(false);
        assertRunNormally("must be accessible even if the accessibility of the method is changed", test);
    }

    private void assertRunNormally(String message, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            fail(message);
        }
    }
}
