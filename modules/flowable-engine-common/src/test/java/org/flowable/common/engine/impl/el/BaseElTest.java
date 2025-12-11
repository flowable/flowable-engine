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
package org.flowable.common.engine.impl.el;

import org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl;
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeBuilder;
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeStore;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;

/**
 * @author Filip Hrisafov
 */
public class BaseElTest {

    protected ExpressionFactory expressionFactory;

    protected Object eval(String expression) {
        return eval(expression, Object.class);
    }

    protected Object eval(String expression, ELContext context) {
        return eval(expression, context, Object.class);
    }

    protected <T> T eval(String expression, Class<T> expectedType) {
        ELContext context = new SimpleContext();
        ValueExpression valueExpression = getExpressionFactory().createValueExpression(context, "${" + expression + "}", expectedType);
        return valueExpression.getValue(context);
    }

    protected <T> T eval(String expression, ELContext context, Class<T> expectedType) {
        if (context == null) {
            context = new SimpleContext();
        }
        ValueExpression valueExpression = getExpressionFactory().createValueExpression(context, "${" + expression + "}", expectedType);
        return valueExpression.getValue(context);
    }

    protected ExpressionFactory getExpressionFactory() {
        if (expressionFactory == null) {
            expressionFactory = createExpressionFactory();
        }
        return expressionFactory;
    }

    protected ExpressionFactory createExpressionFactory() {
        TreeBuilder builder = new Builder(Builder.Feature.METHOD_INVOCATIONS, Builder.Feature.VARARGS);
        TreeStore store = new TreeStore(builder, null);
        return new ExpressionFactoryImpl(store);
    }

}
