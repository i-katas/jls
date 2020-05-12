package com.ikatas.jls.test.conversion;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class UnboxingConversionTest {
    @Test
    public void unboxingWrapperTypeToCorrespondingPrimitiveTypeAutomatically() {
        Integer n = 1;

        int unboxed = n;

        assertThat(unboxed, equalTo(n));
    }

    @Test
    public void unboxingWrapperTypeToCorrespondingPrimitiveTypeAndThenApplyWideningPrimitiveConversionAutomatically() {
        Integer n = 1;

        long unboxed = n;

        assertThat(unboxed, equalTo(1L));
    }
}
