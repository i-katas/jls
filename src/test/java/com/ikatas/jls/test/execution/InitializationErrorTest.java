package com.ikatas.jls.test.execution;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class InitializationErrorTest {
    static int a = bValue();//b has not been initialized yet
    static int b = 1;

    static int bValue() {
        return b;
    }

    @Test
    public void initializingFieldWithStaticMethodMayCauseUnexpectedInitializingError() {
        assertThat(b, equalTo(1));
        assertThat(a, equalTo(0));
    }
}
