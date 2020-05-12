package com.ikatas.jls.test.conversion;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;

public class BoxingConversionTest {
    @Test
    public void boxingToCorrespondingWrapperTypeAutomatically() {
        int n = 1;

        Integer boxed = n;

        assertThat(boxed, equalTo(n));
    }

    @Test
    public void boxingToCorrespondingWrapperTypeAndThenApplyWideningReferenceConversionAutomatically() {
        int n = 1;

        Number boxed = n;

        assertThat(boxed, isA(Integer.class));
    }
}
