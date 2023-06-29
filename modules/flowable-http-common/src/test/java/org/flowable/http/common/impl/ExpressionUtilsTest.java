package org.flowable.http.common.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionUtilsTest {

    @Test
    void getStringSetFromField() {
        assertEquals(Set.of("4XX", "5XX"), ExpressionUtils.getStringSetFromField("4XX, 5XX"));
    }
}