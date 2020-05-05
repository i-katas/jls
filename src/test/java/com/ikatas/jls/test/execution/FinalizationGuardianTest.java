package com.ikatas.jls.test.execution;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class FinalizationGuardianTest {

    @Test
    public void forceFinalizationCalledIfSubclassDoesNotInvokeSuperFinalizationExplicitly() {
        for (int i = 0; i < 1000; i++) {
            new Subclass();
        }

        System.gc();
        assertTrue(finalized.get());
    }

    private final AtomicBoolean finalized = new AtomicBoolean(false);

    class Superclass {
        final Object finalizationGuardian = new Object() {
            @Override
            @SuppressWarnings("deprecation")
            protected void finalize() {
                setFinalizationCalled();
            }
        };

        @Override
        @SuppressWarnings("deprecation")
        protected void finalize() throws Throwable {
            setFinalizationCalled();
        }

        private void setFinalizationCalled() {
            finalized.set(true);
        }
    }

    class Subclass extends Superclass {
        byte[] large = new byte[1024 * 1024];

        @Override
        protected void finalize() throws Throwable {
            // ignore super.finalize();
        }
    }

}
