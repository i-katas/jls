package com.ikatas.jls.test.conversion;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class WideningAndNarrowingConversionTest {
    @Test
    public void charCastToByteApplyWideningPrimitiveConversionImplicitlyAndNarrowingPrimitiveConversionExplicitly() {
        char c = 97;

        byte b = (byte) c;

        assertThat(b, equalTo((byte) 97));
    }
}
