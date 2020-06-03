package jdk.base.java.lang.invoke;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.invoke.MethodType.methodType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

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

    @Test
    public void throwsUnsupportedOperationExceptionWhenInvokeMethodHandleReflectively() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        MethodHandle valueHandle = MethodHandles.lookup().findStatic(getClass(), "value", methodType(int.class));
        Method invoker = MethodHandle.class.getDeclaredMethod("invokeExact", Object[].class);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> invoker.invoke(valueHandle, new Object[1]));

        assertThat(exception.getCause(), hasMessage(containsString("MethodHandle.invokeExact cannot be invoked reflectively")));
    }

    @Test
    public void invokeMethodHandleByExactInvoker() throws Throwable {
        MethodHandle valueHandle = MethodHandles.lookup().findStatic(getClass(), "value", methodType(int.class));
        MethodHandle invoker = MethodHandles.exactInvoker(methodType(int.class));

        int value = (int) invoker.invokeExact(valueHandle);

        assertThat(value, equalTo(1));
    }

    private static int value() {
        return 1;
    }

    private void assertRunNormally(String message, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            fail(message);
        }
    }
}
