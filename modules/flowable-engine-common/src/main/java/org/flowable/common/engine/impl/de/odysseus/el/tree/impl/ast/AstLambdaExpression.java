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

import org.flowable.common.engine.impl.de.odysseus.el.tree.Bindings;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.LambdaExpression;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.flowable.common.engine.impl.javax.el.ValueReference;

/**
 * Lambda expression node. Represents a lambda expression like {@code x -> x + 1}.
 *
 * @author Filip Hrisafov
 */
public class AstLambdaExpression extends AstRightValue {
    private final AstLambdaParameters parameters;
    private final AstNode body;

    public AstLambdaExpression(AstLambdaParameters parameters, AstNode body) {
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public void appendStructure(StringBuilder builder, Bindings bindings) {
        parameters.appendStructure(builder, bindings);
        builder.append(" -> ");
        body.appendStructure(builder, bindings);
    }

    @Override
    public Object eval(Bindings bindings, ELContext context) {
        // Create a ValueExpression from the body
        ValueExpression bodyExpression = new LambdaBodyValueExpression(bindings, body);

        // Create and return a LambdaExpression
        LambdaExpression lambda = new LambdaExpression(parameters.getParameterNames(), bodyExpression);
        lambda.setELContext(context);
        return lambda;
    }

    @Override
    public int getCardinality() {
        return 2;
    }

    @Override
    public AstNode getChild(int i) {
        return i == 0 ? parameters : i == 1 ? body : null;
    }

    @Override
    public String toString() {
        return parameters.toString() + " -> " + body.toString();
    }

    /**
     * Simple ValueExpression implementation that wraps an AstNode for lambda body evaluation.
     */
    private static class LambdaBodyValueExpression extends ValueExpression {
        private final Bindings bindings;
        private final AstNode body;

        public LambdaBodyValueExpression(Bindings bindings, AstNode body) {
            this.bindings = bindings;
            this.body = body;
        }

        @Override
        public Object getValue(ELContext context) throws ELException {
            return body.eval(bindings, context);
        }

        @Override
        public void setValue(ELContext context, Object value) throws ELException {
            throw new ELException("Lambda body is not an lvalue");
        }

        @Override
        public boolean isReadOnly(ELContext context) throws ELException {
            return true;
        }

        @Override
        public Class<?> getType(ELContext context) throws ELException {
            return Object.class;
        }

        @Override
        public Class<?> getExpectedType() {
            return Object.class;
        }

        @Override
        public String getExpressionString() {
            return body.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LambdaBodyValueExpression)) return false;
            LambdaBodyValueExpression other = (LambdaBodyValueExpression) obj;
            return body.equals(other.body) && bindings.equals(other.bindings);
        }

        @Override
        public int hashCode() {
            return body.hashCode() * 31 + bindings.hashCode();
        }

        @Override
        public boolean isLiteralText() {
            return false;
        }

        @Override
        public ValueReference getValueReference(ELContext context) {
            return null;
        }
    }
}
