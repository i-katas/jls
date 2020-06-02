package com.ikatas.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.ByteBuffer.wrap;

public class DirectClassLoader extends ClassLoader {
    private final boolean forceReload;
    private Map<String, Class<?>> loadedClasses = new LinkedHashMap<>();

    public DirectClassLoader(boolean forceReload) {
        this.forceReload = forceReload;
    }

    public static DirectClassLoader directClassLoader() {
        return directClassLoader(false);
    }

    public static DirectClassLoader directClassLoader(boolean forceReload) {
        return new DirectClassLoader(forceReload);
    }

    @Override
    public String getName() {
        return "direct";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!forceReload) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
        }
        if (name.startsWith("com.ikatas") || name.startsWith("jdk.")) {
            Class<?> c = defineClass(name, wrap(readClass(name)), null);
            loadedClasses.put(name, c);
            return c;
        }
        return super.loadClass(name, resolve);
    }

    private byte[] readClass(String name) throws ClassNotFoundException {
        try (InputStream in = new BufferedInputStream(findClassFile(name).openStream())) {
            return toBytes(new BufferedInputStream(in));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected URL findClassFile(String name) throws ClassNotFoundException {
        String classFile = name.replace('.', '/') + ".class";
        URL resource = getResource(classFile);
        if (resource == null) {
            throw new ClassNotFoundException(name);
        }
        return resource;
    }

    private byte[] toBytes(InputStream in) throws IOException {
        var bos = new ByteArrayOutputStream();
        write(in, bos);
        return bos.toByteArray();
    }

    private void write(InputStream in, OutputStream out) throws IOException {
        var buff = new byte[1024];
        for (int n; (n = in.read(buff)) != -1; ) {
            out.write(buff, 0, n);
        }
    }

    public boolean isClassLoaded(String name) {
        return loadedClasses.containsKey(name);
    }

    public Collection<? extends String> loadedClasses() {
        return loadedClasses.keySet();
    }
}
