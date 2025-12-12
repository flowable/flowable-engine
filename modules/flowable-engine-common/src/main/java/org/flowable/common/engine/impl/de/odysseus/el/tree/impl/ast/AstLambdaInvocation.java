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

/**
 * Lambda invocation node. Represents invoking a lambda expression with arguments.
 *
 * @author Filip Hrisafov
 */
public class AstLambdaInvocation extends AstRightValue {
    private final AstNode lambdaNode;
    private final AstParameters params;

    public AstLambdaInvocation(AstNode lambdaNode, AstParameters params) {
        this.lambdaNode = lambdaNode;
        this.params = params;
    }

    @Override
    public void appendStructure(StringBuilder builder, Bindings bindings) {
        lambdaNode.appendStructure(builder, bindings);
        params.appendStructure(builder, bindings);
    }

    @Override
    public Object eval(Bindings bindings, ELContext context) {
        // Evaluate the lambda expression
        Object lambdaObj = lambdaNode.eval(bindings, context);

        if (!(lambdaObj instanceof LambdaExpression)) {
            throw new ELException("Expected LambdaExpression but got: " +
                (lambdaObj == null ? "null" : lambdaObj.getClass().getName()));
        }

        LambdaExpression lambda = (LambdaExpression) lambdaObj;

        // Evaluate the arguments
        Object[] args = params.eval(bindings, context);

        // Invoke the lambda
        Object result = lambda.invoke(context, args);
        return result;
    }

    @Override
    public int getCardinality() {
        return 2;
    }

    @Override
    public AstNode getChild(int i) {
        return i == 0 ? lambdaNode : i == 1 ? params : null;
    }

    @Override
    public String toString() {
        return lambdaNode.toString() + params.toString();
    }
}
