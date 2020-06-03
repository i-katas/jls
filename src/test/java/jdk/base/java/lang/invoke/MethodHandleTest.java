package jdk.base.java.lang.invoke;

import org.hamcrest.Matcher;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.ikatas.util.DirectClassLoader.directClassLoader;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class MethodHandleTest {
    private final Lookup lookup;
    // type descriptor: (I)Ljava/lang/String;
    private final MethodHandle substring;

    public MethodHandleTest() throws IllegalAccessException, NoSuchMethodException {
        lookup = privateLookupIn(String.class, lookup()); // need opens java.base/java.lang package to current module
        substring = lookup.findVirtual(String.class, "substring", methodType(String.class, int.class));
    }

    @Test
    public void invokeMethodHandleDirectToUnderlyingMethod() throws Throwable {
        assertThat((String) substring.invokeExact("fuzzbuzz", 4), equalTo("buzz"));
    }

    @Test
    public void referenceToAnUnderlyingConstructor() throws Throwable {
        MethodHandle constructor = lookup.findConstructor(String.class, methodType(void.class, char[].class));

        assertThat((String) constructor.invokeExact(new char[]{'b', 'u', 'z', 'z'}), equalTo("buzz"));
    }

    @Test
    public void referenceToAnUnderlyingField() throws Throwable {
        MethodHandle value = lookup.findGetter(String.class, "value", byte[].class);

        assertThat((byte[]) value.invokeExact("abc"), equalTo(new byte[]{'a', 'b', 'c'}));
    }

    @Test
    public void invokeExactThrowsExceptionIfSymbolicTypeDescriptorMismatchedItsOwnTypeDescriptor() throws Throwable {
        WrongMethodTypeException exception = assertThrows(WrongMethodTypeException.class, () -> {
            // symbolic type descriptor: (I)Ljava/lang/Object;
            Object unused = substring.invokeExact("", 1);
        });

        assertThat(exception, hasMessage(equalTo("expected (String,int)String but found (String,int)Object")));
    }

    @Test
    public void returnTypeOfSymbolicTypeDescriptorWillBeVoidIfInvocationIsAStatement() throws Throwable {
        WrongMethodTypeException exception = assertThrows(WrongMethodTypeException.class, () -> {
            substring.invokeExact("", 1);
        });

        assertThat(exception, hasMessage(equalTo("expected (String,int)String but found (String,int)void")));
    }

    @Test
    public void anUncastedNullArgumentsWillBe_java_lang_Void_InSymbolicTypeDescriptor() throws Throwable {
        WrongMethodTypeException exception = assertThrows(WrongMethodTypeException.class, () -> {
            String s = (String) substring.invokeExact(null, 1);
        });

        assertThat(exception, hasMessage(equalTo("expected (String,int)String but found (Void,int)String")));
        assertThat(invocationStackOf(exception), not(hasItem(asTypeMethodInvocation())));
    }

    private Matcher<String> asTypeMethodInvocation() {
        return equalTo(MethodHandle.class.getName() + ".asType");
    }

    @Test
    public void invokeAttemptToAdjustItsTypeDescriptorToMatchSymbolicTypeDescriptorExactlyByAsType() throws Throwable {
        WrongMethodTypeException exception = assertThrows(WrongMethodTypeException.class, () -> substring.invoke("", 1L));

        assertThat(exception, hasMessage(equalTo("cannot convert MethodHandle(String,int)String to (String,long)Object")));
        assertThat(invocationStackOf(exception), hasItem(asTypeMethodInvocation()));
    }

    @Test
    public void methodHandleDoesNotCached() throws Throwable {
        MethodHandle constructor = lookup.findConstructor(String.class, methodType(void.class, char[].class));
        MethodHandle constructor2 = lookup.findConstructor(String.class, methodType(void.class, char[].class));

        assertThat(constructor2, is(not(sameInstance(constructor))));
    }

    @Test
    public void bindReceiverArgument() throws Throwable {
        MethodHandle bound = substring.bindTo("fuzzbuzz");

        assertThat(bound.invoke(4), equalTo("buzz"));
    }

    @Test
    public void convertTypeDescriptor() throws Throwable {
        // type descriptor: (Ljava/lang/CharSequence;I)Ljava/lang/CharSequence;
        MethodHandle converted = substring.asType(methodType(CharSequence.class, CharSequence.class, int.class));

        assertThat((CharSequence) converted.invokeExact((CharSequence) "fuzz", 2), equalTo("zz"));
        assertThrows(ClassCastException.class, () -> {
            CharSequence result = (CharSequence) converted.invokeExact((CharSequence) new StringBuilder("anything"), 1);
        });
    }

    @Test
    public void nonStaticMethodHasReceiverArguments() {
        Class<String> receiverClass = String.class;

        assertThat(substring.type(), equalTo(methodType(String.class, receiverClass, int.class)));
    }

    @Test
    public void invokedBySeparatedClassLoaderWithClassLoadedByDifferentClassLoaderRepeatedlyFailedDueToMethodTypeMatchingTakeIntoBothTypeNamesAndClassLoader() throws Throwable {
        Class<?> invokerClass = directClassLoader(true).loadClass(Invoker.class.getName());
        assertThat("loaded by diff class loaders", invokerClass, not(equalTo(Invoker.class)));

        Method method = invokerClass.getDeclaredMethod("invokeHandle");
        method.setAccessible(true);
        MethodHandle invoker = (MethodHandle) method.invoke(null);

        assertThrows(ClassCastException.class, () -> invoker.invoke(new Invoker()));
    }

    @Test
    public void aPrivateMethodHandleCreatedByClassThatHasToAccessThatMethodWhichCanBeUsedInAnyPlace() throws Throwable {
        String result = (String) Invoker.invokeHandle().invokeExact(new Invoker());

        assertThat(result, equalTo("success"));
    }

    @Test
    public void constructsSymbolicTypeDescriptorWhichContainsGenericTypeWillReplaceByTheirErasures() throws Throwable {
        List<List<Integer>> numbers = new ArrayList<>();
        MethodHandle addInvoker = lookup.findVirtual(List.class, "add", methodType(boolean.class, Object.class)).asType(methodType(boolean.class, List.class, List.class));

        //symbolic type descriptor: (Ljava/util/List;)Z
        boolean success = (boolean) addInvoker.invokeExact(numbers, asList(1, 2, 3));

        assertTrue(true);
        assertThat(numbers, contains(asList(1, 2, 3)));
    }

    private List<String> invocationStackOf(Exception exception) {
        return stream(exception.getStackTrace()).map(it -> it.getClassName() + "." + it.getMethodName()).collect(toList());
    }
}

class Invoker {
    private String invoke() {
        return "success";
    }

    public static MethodHandle invokeHandle() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findVirtual(Invoker.class, "invoke", methodType(String.class));
    }
}

