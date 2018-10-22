package org.flowable.dmn.engine.impl.el.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CollectionUtilTest {

    @Test
    public void noneOf() {
        assertTrue(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4")));
        assertFalse(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2")));
        assertFalse(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3")));

        assertTrue(CollectionUtil.noneOf(Arrays.asList("group1", "group2"), "group3"));
        assertFalse(CollectionUtil.noneOf(Arrays.asList("group1", "group2"),"group2"));

        assertTrue(CollectionUtil.noneOf("group1, group2", "group3, group4"));
        assertFalse(CollectionUtil.noneOf("group1, group2", "group1, group2"));
        assertFalse(CollectionUtil.noneOf("group1, group2", "group2, group3"));

        ObjectMapper mapper = new ObjectMapper();
        assertTrue(CollectionUtil.noneOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))));
        assertFalse(CollectionUtil.noneOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2"))));
        assertFalse(CollectionUtil.noneOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))));
    }


    @Test
    public void anyOf() {
        assertFalse(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4")));
        assertTrue(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2")));
        assertTrue(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3")));

        assertFalse(CollectionUtil.anyOf(Arrays.asList("group1", "group2"), "group3"));
        assertTrue(CollectionUtil.anyOf(Arrays.asList("group1", "group2"),"group2"));

        assertFalse(CollectionUtil.anyOf("group1, group2", "group3, group4"));
        assertTrue(CollectionUtil.anyOf("group1, group2", "group1, group2"));
        assertTrue(CollectionUtil.anyOf("group1, group2", "group2, group3"));

        ObjectMapper mapper = new ObjectMapper();
        assertFalse(CollectionUtil.anyOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))));
        assertTrue(CollectionUtil.anyOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2"))));
        assertTrue(CollectionUtil.anyOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))));
    }

    @Test
    public void notAllOf() {
        assertTrue(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4")));
        assertFalse(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2")));
        assertTrue(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3")));

        assertTrue(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"), "group3"));
        assertFalse(CollectionUtil.notAllOf(Arrays.asList("group1", "group2"),"group2"));

        assertTrue(CollectionUtil.notAllOf("group1, group2", "group3, group4"));
        assertFalse(CollectionUtil.notAllOf("group1, group2", "group1, group2"));
        assertTrue(CollectionUtil.notAllOf("group1, group2", "group2, group3"));

        ObjectMapper mapper = new ObjectMapper();
        assertTrue(CollectionUtil.notAllOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))));
        assertFalse(CollectionUtil.notAllOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2"))));
        assertTrue(CollectionUtil.notAllOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))));
    }

    @Test
    public void allOf() {
        assertFalse(CollectionUtil.allOf(Arrays.asList("group1", "group2"), Arrays.asList("group3", "group4")));
        assertTrue(CollectionUtil.allOf(Arrays.asList("group1", "group2"), Arrays.asList("group1", "group2")));
        assertFalse(CollectionUtil.allOf(Arrays.asList("group1", "group2"), Arrays.asList("group2", "group3")));

        assertFalse(CollectionUtil.allOf(Arrays.asList("group1", "group2"), "group3"));
        assertTrue(CollectionUtil.allOf(Arrays.asList("group1", "group2"),"group2"));

        assertFalse(CollectionUtil.allOf("group1, group2", "group3, group4"));
        assertTrue(CollectionUtil.allOf("group1, group2", "group1, group2"));
        assertFalse(CollectionUtil.allOf("group1, group2", "group2, group3"));

        ObjectMapper mapper = new ObjectMapper();
        assertFalse(CollectionUtil.allOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group3", "group4"))));
        assertTrue(CollectionUtil.allOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group1", "group2"))));
        assertFalse(CollectionUtil.allOf(mapper.valueToTree(Arrays.asList("group1", "group2")), mapper.valueToTree(Arrays.asList("group2", "group3"))));
    }

}
