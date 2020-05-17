package com.ikatas.jls.test.modules;

import jdk.internal.loader.BootLoader;
import org.junit.Test;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModuleTest {
    @Test
    public void addExportsToModuleInternalPackagesForReadability() {
        assertThat("application classes", BootLoader.loadClassOrNull(ModuleTest.class.getName()), is(nullValue()));
        assertThat("builtin classes", BootLoader.loadClassOrNull(String.class.getName()), is(String.class));
    }

    @Test
    public void addOpensToModulePackagesForReflections() throws NoSuchFieldException, IllegalAccessException {
        Field field = DriverManager.class.getDeclaredField("JDBC_DRIVERS_PROPERTY");

        field.setAccessible(true);

        assertThat(field.get(null), equalTo("jdbc.drivers"));
    }

    @Test
    public void testUnnamedModuleHasNoAnyModuleDescriptors() {
        Module unnamed = getClass().getModule();

        assertFalse("unnamed", unnamed.isNamed());
        assertThat(unnamed.getName(), is(nullValue()));
        assertThat(unnamed.getDescriptor(), is(nullValue()));
    }

    @Test
    public void automaticModule() {
        Path[] modulePaths = Arrays.stream(System.getProperty("java.class.path").split("[:;]")).map(Paths::get).toArray(Path[]::new);

        ModuleDescriptor module = ModuleFinder.of(modulePaths).find("junit").get().descriptor();

        assertTrue("automatic", module.isAutomatic());
        assertFalse("open", module.isOpen());
        Stream<ModuleDescriptor.Requires> stream = module.requires().stream();
        assertThat("java.base mandated", stream.map(ModuleDescriptor.Requires::name).collect(toList()), contains("java.base"));
        assertThat(module.exports(), is(empty()));
        assertThat(module.opens(), is(empty()));
    }
}
