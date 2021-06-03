package org.flowable.common.engine.impl.el;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Prathamesh Mane
 */
class JsonNodeELResolverTest {
    private final ELResolver resolver = new JsonNodeELResolver();
    private final ELContext context = new FlowableElContext(resolver, null);

    @Test
    void integerValueShouldBeIntNode() {
        ObjectNode node = new ObjectMapper().createObjectNode();
        resolver.setValue(context, node, "age", 11);
        assertTrue(node.get("age").isInt());
    }
}