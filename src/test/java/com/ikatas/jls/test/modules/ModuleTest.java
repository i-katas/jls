package com.ikatas.jls.test.modules;

import jdk.internal.loader.BootLoader;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ModuleTest {
    @Test
    public void readsInternalPackages() {
        assertThat("application classes", BootLoader.loadClassOrNull(ModuleTest.class.getName()), is(nullValue()));
        assertThat("builtin classes", BootLoader.loadClassOrNull(String.class.getName()), is(String.class));
    }
}
