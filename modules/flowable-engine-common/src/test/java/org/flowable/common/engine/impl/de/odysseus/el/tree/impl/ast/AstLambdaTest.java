/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.flowable.common.engine.impl.de.odysseus.el.TestCase;
import org.flowable.common.engine.impl.de.odysseus.el.tree.Bindings;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.el.ReadOnlyMapELResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class AstLambdaTest extends TestCase {

    SimpleContext context;

    @BeforeEach
    void setUp() {
        context = new SimpleContext(new SimpleResolver(new BeanELResolver()));
    }

    @Test
    void simpleLambdaParsing() {
        // Test that lambda expressions can be parsed successfully
        Bindings bindings = new Bindings(null, null, null);

        // Single parameter lambda
        parse("${x -> x + 1}").getRoot().getValue(bindings, context, Object.class);

        // Multiple parameter lambda
        parse("${(x, y) -> x + y}").getRoot().getValue(bindings, context, Object.class);

        // Lambda with parentheses around single parameter
        parse("${(x) -> x * 2}").getRoot().getValue(bindings, context, Object.class);

        // Lambda with complex expression
        parse("${(x, y) -> x * y + 1}").getRoot().getValue(bindings, context, Object.class);
    }

    @Test
    void lambdaInvocation() {
        // Test that lambda expressions can be invoked with arguments
        Bindings bindings = new Bindings(null, null, null);

        // Single parameter lambda
        assertThat(parse("${(x -> x + 1)(4)}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(5L);

        // Multiple parameter lambda
        assertThat(parse("${((x, y) -> x + y)(4, 5)}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(9L);

        // Lambda with parentheses around single parameter
        assertThat(parse("${((x) -> x * 2)(6)}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(12L);

        // Lambda with complex expression
        assertThat(parse("${((x, y) -> x * y + 1)(4, 5)}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(21L);

        // Lambda without parameters
        assertThat(parse("${(() -> 42)()}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(42L);
    }

    @Test
    void multiLevelLambdaInvocation() {
        // Test that lambda expressions can be invoked with arguments
        Bindings bindings = new Bindings(null, null, null);

        assertThat(parse("${(x -> y -> y + x + 1)(4)(5)}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(10L);
    }

    @Test
    void coerceToFunctionalInterface() {
        Bindings bindings = new Bindings(null, new ValueExpression[] { null }, null);

        SimpleContext context = new SimpleContext(new CompositeELResolver(
                List.of(
                        new ReadOnlyMapELResolver(Map.of("bean", new TestBean("initialValue"))),
                        new BeanELResolver()
                )
        ));

        assertThat(parse("${bean.map(x -> x.concat(' test'))}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo("initialValue test");

        assertThat(parse("${bean.supplier(() -> 'test')}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo("test");

        assertThat(parse("${bean.satisfies(value -> value eq 'test')}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(Boolean.FALSE);

        assertThat(parse("${bean.satisfies(value -> value eq 'initialValue')}").getRoot().getValue(bindings, context, Object.class))
                .isEqualTo(Boolean.TRUE);
    }

    static class TestBean {

        protected final String value;

        TestBean(String value) {
            this.value = value;
        }

        public Object map(Function<String, Object> mapper) {
            return mapper.apply(value);
        }

        public Object supplier(Supplier<Object> supplier) {
            return supplier.get();
        }

        public boolean satisfies(Predicate<Object> predicate) {
            return predicate.test(value);
        }
    }

}
