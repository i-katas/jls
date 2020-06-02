package com.ikatas.jls.test.execution;

import com.ikatas.util.DirectClassLoader;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.ikatas.util.DirectClassLoader.directClassLoader;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.util.Collections.reverse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class ClassLoaderTest {

    private final String testClassName = getClass().getName();

    @Test
    public void classLoaderChain() {
        assertThat(classLoaders(), contains("platform", "app"));
    }

    @Test
    public void appClassLoader() {
        ClassLoader appClassLoader = getClass().getClassLoader();
        ClassLoader bootstrapClassLoader = appClassLoader.getClass().getClassLoader();

        assertThat(appClassLoader.getName(), equalTo("app"));
        assertThat(bootstrapClassLoader, is(nullValue()));
    }

    @Test
    public void builtinClassesLoadedByBootstrapClassLoader() {
        assertThat(Object.class.getClassLoader(), is(nullValue()));
    }

    @Test
    public void identicalClassLoadByDifferentClassLoadersAreNotEqual() throws ClassNotFoundException {
        Class<?> testClass = getClass();

        assertThat(Class.forName(testClass.getName(), true, directClassLoader()), not(equalTo(testClass)));
    }

    @Test
    public void failsOnCastInstanceBetweenDistinctIdenticalClasses() throws Exception {
        Class<?> testClass = getClass();

        Class<?> distinctClass = Class.forName(testClass.getName(), true, directClassLoader());

        assertThrows(ClassCastException.class, () -> testClass.cast(distinctClass.getDeclaredConstructor().newInstance()));
    }

    @Test
    public void jvmWillRecordLoadedClassesForEachClassLoader() throws ClassNotFoundException {
        ClassLoader testLoader = directClassLoader();

        Class<?> firstLoaded = testLoader.loadClass(testClassName);

        assertThat(testLoader.loadClass(testClassName), sameInstance(firstLoaded));
        assertThat(directClassLoader().loadClass(testClassName), not(equalTo(firstLoaded)));
    }

    @Test
    public void raisesErrorIfLoadDuplicatedClasses() throws ClassNotFoundException {
        ClassLoader testLoader = directClassLoader(true);

        testLoader.loadClass(testClassName);

        LinkageError error = assertThrows(LinkageError.class, () -> testLoader.loadClass(testClassName));
        assertThat(error, hasMessage(containsString(format("loader 'direct' @%s attempted duplicate class definition for %s",
                toHexString(testLoader.hashCode()), testClassName))));
    }

    @Test
    public void loadLinkedClassesLazilyWithSameLoaderOfHostClass() throws ClassNotFoundException, NoSuchFieldException {
        DirectClassLoader testLoader = directClassLoader();

        Class<?> testClass = testLoader.loadClass(testClassName);
        assertTrue(testLoader.isClassLoaded(testClassName));
        assertFalse(testLoader.isClassLoaded(Sub.class.getName()));

        Field field = testClass.getDeclaredField("sub");
        assertTrue(testLoader.isClassLoaded(Sub.class.getName()));
        assertThat(field.getType(), not(equalTo(Sub.class)));
        assertThat(field.getType().getName(), equalTo(Sub.class.getName()));
    }

    @Test
    public void loadParentClassFirstWhenLoadingSubclass() throws ClassNotFoundException {
        String testClassName = Sub.class.getName();
        DirectClassLoader testLoader = directClassLoader();
        assertFalse(testLoader.isClassLoaded(testClassName));

        testLoader.loadClass(testClassName);

        assertThat(testLoader.loadedClasses(), containsInRelativeOrder(Sub.class.getSuperclass().getName(), testClassName));
    }

    @Test
    public void loadSuperInterfacesBeforeAllClassesWhenLoadingSubclass() throws ClassNotFoundException {
        String testClassName = Sub.class.getName();
        DirectClassLoader testLoader = directClassLoader();
        assertFalse(testLoader.isClassLoaded(testClassName));

        testLoader.loadClass(testClassName);

        assertThat(testLoader.loadedClasses(), containsInRelativeOrder(Any.class.getName(), Markable.class.getName(),
                Sup.class.getName(), Sub.class.getName()));
    }

    private Sub sub;

    static class Sup implements Markable {
    }

    static final class Sub extends Sup {
    }

    interface Any {
    }

    interface Markable extends Any {
    }

    private List<String> classLoaders() {
        List<String> names = new ArrayList<>();
        for (ClassLoader current = getSystemClassLoader(); current != null; ) {
            names.add(current.getName());
            current = current.getParent();
        }
        reverse(names);
        return names;
    }

}
