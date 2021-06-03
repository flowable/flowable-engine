package org.flowable.common.engine.impl.el;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Prathamesh Mane
 */
class JsonNodeELResolverTest {
    private final ELResolver resolver = new JsonNodeELResolver();
    private final ELContext context = new FlowableElContext(resolver, null);
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void integerValueShouldBeIntNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "age", 11);
        assertTrue(node.get("age").isInt());
    }

    @Test
    void longValueShouldBeLongNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "age", 11L);
        assertTrue(node.get("age").isLong());
    }

    @Test
    void doubleValueShouldBeDoubleNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "net", 11.1);
        assertTrue(node.get("net").isDouble());
    }

    @Test
    void stringValueShouldBeTextNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "name", "flowable");
        assertTrue(node.get("name").isTextual());
    }

    @Test
    void bigDecimalValueShouldBeBigDecimalNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "net", BigDecimal.ZERO);
        assertTrue(node.get("net").isBigDecimal());
    }

    @Test
    void booleanValueShouldBeBooleanNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "isAlive", true);
        assertTrue(node.get("isAlive").isBoolean());
    }

    @Test
    void dateValueShouldBeTextNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "dob", new Date());
        assertTrue(node.get("dob").isTextual());
    }

    @Test
    void jsonValueShouldBeJsonNode() {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode value = mapper.createObjectNode();
        resolver.setValue(context, node, "address", value);
        assertTrue(node.get("address").isObject());
    }

    @Test
    void nullValueShouldBeNullNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "age", null);
        assertTrue(node.get("age").isNull());
    }
}