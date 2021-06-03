package org.flowable.common.engine.impl.el;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Date;

import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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
        assertThat(node.get("age")).isExactlyInstanceOf(IntNode.class);
    }

    @Test
    void longValueShouldBeLongNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "age", 11L);
        assertThat(node.get("age")).isExactlyInstanceOf(LongNode.class);
    }

    @Test
    void doubleValueShouldBeDoubleNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "net", 11.1);
        assertThat(node.get("net")).isExactlyInstanceOf(DoubleNode.class);
    }

    @Test
    void stringValueShouldBeTextNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "name", "flowable");
        assertThat(node.get("name")).isExactlyInstanceOf(TextNode.class);
    }

    @Test
    void bigDecimalValueShouldBeBigDecimalNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "net", BigDecimal.ZERO);
        assertThat(node.get("net")).isExactlyInstanceOf(DecimalNode.class);
    }

    @Test
    void booleanValueShouldBeBooleanNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "isAlive", true);
        assertThat(node.get("isAlive")).isExactlyInstanceOf(BooleanNode.class);
    }

    @Test
    void dateValueShouldBeTextNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "dob", new Date());
        assertThat(node.get("dob")).isExactlyInstanceOf(TextNode.class);
    }

    @Test
    void jsonValueShouldBeJsonNode() {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode value = mapper.createObjectNode();
        resolver.setValue(context, node, "address", value);
        assertThat(node.get("address")).isExactlyInstanceOf(ObjectNode.class);
    }

    @Test
    void nullValueShouldBeNullNode() {
        ObjectNode node = mapper.createObjectNode();
        resolver.setValue(context, node, "age", null);
        assertThat(node.get("age")).isExactlyInstanceOf(NullNode.class);
    }
}