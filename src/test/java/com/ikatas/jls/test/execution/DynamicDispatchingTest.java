package com.ikatas.jls.test.execution;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DynamicDispatchingTest {
    @Test
    public void fieldInitializersInvokedAfterConstructorInvocations() {
        class Superclass {
            int baseValue = -2; //field initializer to be invoked after constructor invocation but before the rest constructor body invocations

            Superclass() {
                super(); // constructor invocations to be invoked first during instance initialization
                baseValue = getInitialValue(); // constructor body invoked after field initializers
            }

            int getInitialValue() {
                throw new UnsupportedOperationException();
            }
        }

        class Subclass extends Superclass {
            int value = -1;

            Subclass() {
                super(); // constructor invocations to be invoked first during instance initialization
            }

            @Override
            int getInitialValue() {
                return value;
            }
        }

        var instance = new Subclass();

        assertThat(instance.baseValue, equalTo(0));
        assertThat(instance.value, equalTo(-1));
    }
}
