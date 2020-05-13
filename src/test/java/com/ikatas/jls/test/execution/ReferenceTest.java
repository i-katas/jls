package com.ikatas.jls.test.execution;

import org.junit.Test;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReferenceTest {
    private final ReferenceQueue<String> queue = new ReferenceQueue<>();

    @Test
    public void requestToBeNotifiedAfterGCDeterminedReferentReachabilityHasChangedByRegisterWithAQueueWhenCreated() {
        List<Reference<?>> refs = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            String s = new String(new byte[1024]);
            strings.add(s);
            refs.add(new WeakReference<>(s, queue));
        }

        System.gc();
        System.runFinalization();
        assertThat("reachable", queue.poll(), is(nullValue()));

        strings.clear();
        System.gc();
        System.runFinalization();
        Reference<? extends String> reclaimed = queue.poll();
        assertThat("enqueued", reclaimed, is(notNullValue()));
        assertThat(reclaimed.get(), is(nullValue()));
    }

    @Test
    public void clearReferentAfterReferenceEnqueued() {
        SoftReference<String> ref = new SoftReference<>("bar", queue);
        assertThat(ref.get(), equalTo("bar"));

        assertTrue("enqueue", ref.enqueue());
        assertTrue(ref.isEnqueued());
        assertThat(ref.get(), is(nullValue()));

        assertFalse(ref.enqueue());
        assertTrue(ref.isEnqueued());
    }

    @Test
    public void referenceNeverBeEnqueuedIfItselfBecomeUnreachable() {
        for (int i = 0; i < 2000; i++) {
            new PhantomReference<>(new String(new byte[1024]), queue);
        }

        System.gc();
        System.runFinalization();

        assertThat(queue.poll(), is(nullValue()));
    }
}
