package com.ikatas.jls.test.threading;

import com.ikatas.jls.test.execution.Unstable;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.State.*;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThreadTest {

    @Test
    public void terminatedThread() throws InterruptedException {
        Thread thread = new Thread();
        thread.start();

        thread.join(); // wait until thread has terminated

        assertFalse(thread.isAlive());
        assertThat(thread, inState(TERMINATED));
    }

    @Test
    public void interruptNotAliveThreadNeedNotHaveAnyEffects() {
        Thread thread = new Thread();

        thread.interrupt();

        assertFalse(thread.isAlive());
        assertFalse("interrupt should has no effect if thread is not alive", thread.isInterrupted());
    }

    @Test
    public void threadInterruptStatusWillBeClearedOnWaitAfterReceivingAnInterruptedException() throws InterruptedException {
        AtomicReference<Throwable> capturedException = new AtomicReference<>();
        Thread thread = startThread(() -> {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    capturedException.set(e);
                }
            }
        });

        thread.interrupt();

        thread.join();
        assertThat(capturedException.get(), isA(InterruptedException.class));
        assertFalse("interrupt status is reset", thread.isInterrupted());
    }

    @Test
    @Unstable
    public void removeThreadFromWaitSetAndThrowsInterruptedExceptionInThreadExecutionIfInterrupted() throws InterruptedException {
        CountDownLatch startLock = new CountDownLatch(2), entryLock = new CountDownLatch(1), exitLock = new CountDownLatch(2);
        List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();
        Runnable action = () -> {
            startLock.countDown();
            synchronized (this) {
                entryLock.countDown();
                try {
                    wait(); // add the current to wait set of this object
                } catch (InterruptedException e) {
                    capturedExceptions.add(e);
                }
            }
            exitLock.countDown();
        };
        Thread first = startThread(action);
        entryLock.await();
        Thread next = startThread(action);
        startLock.await();

        first.interrupt();

        first.join();
        assertThat(capturedExceptions, contains(isA(InterruptedException.class))); // only the interrupted thread raise InterruptedException on wait
        capturedExceptions.clear();
        assertThat("interrupt other threads should not affect current thread", next, anyOf(inState(RUNNABLE), inState(BLOCKED), inState(WAITING)));

        synchronized (this) {
            notify(); // remove the next thread from this wait set and then wakeup it
        }

        assertTrue("next thread has not been wakeup", exitLock.await(500, MILLISECONDS));
        assertThat(capturedExceptions, is(empty()));
    }

    @Test
    public void onlyOneThreadHoldsLockOnMonitorAtATimeAndOthersAttemptingToLockWillBeBlockedUntilCanObtainTheLock() throws InterruptedException {
        CountDownLatch startLock = new CountDownLatch(2), entryLock = new CountDownLatch(1),
                waitLock = new CountDownLatch(1), exitLock = new CountDownLatch(2);
        ThrowableRunnable action = () -> {
            startLock.countDown();
            synchronized (this) {
                entryLock.countDown();
                waitLock.await();
            }
            exitLock.countDown();
        };

        Thread first = startThread(action);
        entryLock.await(); // ensure the first thread already holds the lock

        Thread next = startThread(action);
        startLock.await(); // ensure all threads started

        assertThat(first, inState(WAITING));
        assertThat(next, either(inState(RUNNABLE)).or(inState(BLOCKED)));

        waitLock.countDown(); // release the lock of the first thread
        exitLock.await(); // wait until the first thread exit the synchronization and the next thread to obtain the lock
        assertThat(next, either(inState(RUNNABLE)).or(inState(TERMINATED)));
    }

    @Test
    public void allThreadsInWaitSetWillBeClearedAtomicallyByNotifyAll() throws InterruptedException {
        CountDownLatch startLock = new CountDownLatch(2), entryLock = new CountDownLatch(1), exitLock = new CountDownLatch(2);
        AtomicBoolean toWait = new AtomicBoolean(true);
        ThrowableRunnable waitUntilNotified = () -> {
            startLock.countDown();
            synchronized (this) {
                entryLock.countDown();
                while (toWait.get()) { // spurious wakeup avoidance
                    wait();
                }
            }
            exitLock.countDown();
        };
        startThread(waitUntilNotified);
        startThread(waitUntilNotified);
        startLock.await(); // ensure all threads are alive
        entryLock.await(); // ensure one thread has hold the lock

        synchronized (this) {
            notifyAll(); // clear all threads in wait set
            toWait.set(false);
        } // resume all threads after exit the synchronization

        assertTrue("not all threads in wait set were cleared", exitLock.await(500, MILLISECONDS));
    }

    @Test
    public void notificationMustNotBeLostDueToInterrupt() throws InterruptedException {
        CountDownLatch exitLock;
        while (true) {
            exitLock = new CountDownLatch(2);
            CountDownLatch startLock = new CountDownLatch(2), entryLock = new CountDownLatch(1), finalExitLock = exitLock;
            AtomicReference<Throwable> capturedException = new AtomicReference<>();
            AtomicReference<Thread> interrupted = new AtomicReference<>();
            ThrowableRunnable action = () -> {
                startLock.countDown();
                synchronized (this) {
                    entryLock.countDown();
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted.set(currentThread());
                        capturedException.set(e);
                    }
                }
                finalExitLock.countDown();
            };
            Thread first = startThread(action);
            entryLock.await();
            Thread next = startThread(action);
            startLock.await();

            first.interrupt();
            synchronized (this) {
                notify();
            }
            if (capturedException.get() != null) {
                assertThat(interrupted.get(), equalTo(first));
                break;
            }
            next.interrupt(); // remove it from the wait set
        }

        assertTrue("notification is lost", exitLock.await(500, MILLISECONDS));
    }

    @Test
    public void threadMayReturnNormallyFromWaitIfBothInterruptedAndNotified() throws InterruptedException {
        while (true) {
            CountDownLatch entryLock = new CountDownLatch(1), exitLock = new CountDownLatch(1);
            ThrowableRunnable action = () -> {
                synchronized (this) {
                    entryLock.countDown();
                    try {
                        wait();
                    } catch (InterruptedException ignored) {/**/}
                }
                if (Thread.interrupted()) {
                    exitLock.countDown();
                }
                while (!Thread.interrupted()) currentThread().join();
            };
            Thread thread = startThread(action, (t, e) -> {/**/});
            entryLock.await();

            thread.interrupt(); // remove thread from the wait set and interrupt it
            synchronized (this) {
                notify(); // remove thread from the wait set
            }

            if (exitLock.await(100, MILLISECONDS)) {
                assertFalse("thread interrupt status is reset by Thread.interrupted()", thread.isInterrupted());
                assertTrue(thread.isAlive());
                thread.interrupt();
                return;
            }
            thread.interrupt(); // interrupt while loop
            thread.join();
        }
    }

    @Test
    public void sleepDoesNotLoseTheOwnershipOfAnyMonitors() throws InterruptedException {
        CountDownLatch startLock = new CountDownLatch(2), entryLock = new CountDownLatch(1), exitLock = new CountDownLatch(2);
        Thread sleeping = startThread(() -> {
            startLock.countDown();
            synchronized (this) {
                entryLock.countDown();
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException ignored) {/**/}
                exitLock.countDown();
            }
        });
        entryLock.await();
        Thread blocked = startThread(() -> {
            startLock.countDown();
            synchronized (this) {
                exitLock.countDown();
            }
        });
        startLock.await();

        assertFalse("sleep thread should not lose the ownership of any monitors", exitLock.await(500, MILLISECONDS));
        assertThat(blocked, inState(BLOCKED));

        sleeping.interrupt();
        assertTrue("should obtain the lock of monitor after the sleep thread exit synchronization", exitLock.await(500, MILLISECONDS));
    }

    private Long[] sharedMemory = new Long[1];

    @Test
    @Ignore("CanNotProduce")
    public void sleepHasNotAnySynchronizationSemanticsWhichMeansJVMDoesNotHaveToFlushWriteCachedInRegistersOutToSharedMemory() throws InterruptedException {
        CountDownLatch writeLock = new CountDownLatch(1);

        Thread thread = startThread(() -> {
            sharedMemory[0] = 100L;
            writeLock.countDown();
            try {
                Thread.sleep(Integer.MAX_VALUE);
            } catch (InterruptedException ignored) {/**/}
        });

        writeLock.await();
        long value = sharedMemory[0];
        thread.interrupt();

        assertThat(value, is(not(100L)));
    }

    private static Matcher<Object> inState(Thread.State currentState) {
        return hasProperty("state", equalTo(currentState));
    }

    private Thread startThread(Runnable action) {
        return startThread(action, null);
    }

    private Thread startThread(Runnable action, Thread.UncaughtExceptionHandler exceptionHandler) {
        Thread thread = new Thread(action);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(exceptionHandler);
        thread.start();
        return thread;
    }

    interface ThrowableRunnable extends Runnable {
        @Override
        default void run() {
            try {
                runExceptionally();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        }

        void runExceptionally() throws Throwable;
    }
}
