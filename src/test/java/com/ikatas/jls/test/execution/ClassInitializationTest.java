package com.ikatas.jls.test.execution;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ClassInitializationTest {

    private static final Set<Class<?>> initializedClasses = new LinkedHashSet<>();

    @Before
    public void clearInitializedClasses() {
        initializedClasses.clear();
    }

    @Test
    public void classInitializedWhenDeclaredStaticFieldIsUsed() {
        valueOf(StaticFields.randomized);

        assertThat(initializedClasses, contains(StaticFields.class));
    }

    static class StaticFields {
        static final double randomized = Math.random();

        static {
            initializedWith(1);
        }
    }


    @Test
    public void classNotInitializedWhenDeclaredConstantExpressionIsUsed() {
        valueOf(ConstantExpression.value);

        assertThat(initializedClasses, is(empty()));
    }

    static class ConstantExpression {
        static final String value = "constant";

        static {
            initializedWith(1);
        }
    }


    @Test
    public void classInitializerBlockInitializedLazilyOnLoad() throws ClassNotFoundException {
        Class<?> testClass = StaticInitializer.class;
        assertThat(initializedClasses, is(empty()));

        Class.forName(StaticInitializer.class.getName());

        assertThat(initializedClasses, contains(testClass));
    }

    static class StaticInitializer {
        static {
            initializedWith(1);
        }
    }


    @Test
    public void classDeclaresStaticFieldInitializedOnly() {
        assertThat(initializedClasses, is(empty()));

        Sub.i = 2;
        assertThat(initializedClasses, contains(Sup.class));

        Sub.j = 3;
        assertThat(initializedClasses, contains(Sup.class, Sub.class));
    }

    static class Sup {
        static int i = initializedWith(1);
    }

    static class Sub extends Sup {
        static int j = initializedWith(2);
    }


    @Test
    public void superclassMustBeInitializedFirstWhenInitializingItsSubclass() {
        valueOf(Subclass.i);

        assertThat(initializedClasses, contains(Superclass.class, Subclass.class));
    }

    static class Superclass {
        static {
            initializedWith(1);
        }
    }

    static class Subclass extends Superclass {
        static final int i = initializedWith(1);
    }


    @Test
    public void superInterfacesDoesNotInitializedWhenInitializingInterface() {
        valueOf(I1.i);

        assertThat(initializedClasses, contains(I1.class));
    }

    interface I {
        int j = initializedWith(1);
    }

    interface I1 extends I {
        int i = initializedWith(1);
    }


    @Test
    public void superInterfacesDeclaresAnyDefaultMethodsInitializedWhenInitializingClass() {
        valueOf(C.i);

        assertThat(initializedClasses, contains(DefaultMethods.class, C.class));
    }

    interface DefaultMethods {
        int j = initializedWith(1);

        default void run() {/**/}
    }

    static class C implements DefaultMethods {
        static int i = initializedWith(2);
    }


    @Test
    public void interfaceDoesNotDeclaresAnyDefaultsNotBeInitializedEvenThoughFieldReferredThatInterfaceWhenInitializingStaticReferenceField() {
        valueOf(StaticRefField.ref);

        assertThat(initializedClasses, not(hasItem(Entity.class)));
        assertThat(initializedClasses, contains(AbstractRef.class, Identity.class, Ref.class, StaticRefField.class));
    }

    interface Identity {
        int i = initializedWith(2);

        default void run() {/**/}
    }

    interface Entity extends Identity {
        int i = initializedWith(1);
    }

    static class AbstractRef {
        static int i = initializedWith(2);
    }

    static class Ref extends AbstractRef implements Entity {
        static int i = initializedWith(2);
    }

    static class StaticRefField {
        static Entity ref = initializedWith(new Ref());
    }


    @Test
    public void staticNullRefClassDoesNotInitializedWhenInitializingClass() {
        valueOf(NullStaticRefField.ref);

        assertThat(initializedClasses, contains(NullStaticRefField.class));
    }

    static class NullRef {
        int i = initializedWith(1);
    }

    static class NullStaticRefField {
        static NullRef ref = initializedWith(null);
    }


    @Test
    public void invocationOfCertainReflectionsCausesClassInitialization() throws NoSuchFieldException, IllegalAccessException {
        Class<Reflective> testClass = Reflective.class;
        assertThat(initializedClasses, is(empty()));

        Field refField = testClass.getDeclaredField("ref");
        assertThat(initializedClasses, is(empty()));

        refField.getType();
        assertThat(initializedClasses, is(empty()));

        refField.get(null);
        assertThat(initializedClasses, contains(testClass));
    }

    static class Reflective {
        static Reflective ref = initializedWith(null);
    }

    private static <T> T initializedWith(T value) {
        initializedClasses.add(getCallerClass());
        return value;
    }

    private static Class<?> getCallerClass() {
        try {
            StackTraceElement[] stack = currentThread().getStackTrace();
            return Class.forName(stack[3].getClassName(), false, currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }
}
