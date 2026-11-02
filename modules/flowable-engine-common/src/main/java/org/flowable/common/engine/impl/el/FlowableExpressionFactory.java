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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.de.odysseus.el.ExpressionFactoryImpl;
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeBuilder;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Parser;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstFunction;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstIdentifier;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstParameters;

/**
 * @author Filip Hrisafov
 */
public class FlowableExpressionFactory extends ExpressionFactoryImpl {

    public FlowableExpressionFactory() {
        super();
    }

    @Override
    protected TreeBuilder createDefaultTreeBuilder(Builder.Feature... features) {
        return new FlowableExpressionBuilder(features);
    }

    public void setAstFunctionCreators(Collection<FlowableAstFunctionCreator> astFunctionCreators) {
        TreeBuilder storeBuilder = this.store.getBuilder();
        if (storeBuilder instanceof FlowableExpressionBuilder expressionBuilder) {

            expressionBuilder.getAstFunctionCreators().clear();
            for (FlowableAstFunctionCreator astFunctionCreator : astFunctionCreators) {
                expressionBuilder.addAstFunctionCreator(astFunctionCreator);
            }

        }
    }

    protected static class FlowableExpressionBuilder extends Builder {

        protected final Map<String, FlowableAstFunctionCreator> astFunctionCreators = new HashMap<>();
        protected FlowableAstFunctionCreator defaultFunctionCreator = new FlowableAstFunctionCreator() {

            @Override
            public Collection<String> getFunctionNames() {
                return Collections.emptySet();
            }

            @Override
            public AstFunction createFunction(String name, int index, AstParameters parameters, boolean varargs, FlowableExpressionParser parser) {
                return new AstFunction(name, index, parameters, varargs);
            }
        };

        protected FlowableExpressionBuilder(Builder.Feature... features) {
            super(features);
        }

        @Override
        protected Parser createParser(String expression) {
            return new FlowableExpressionParserImpl(this, expression);
        }

        public void addAstFunctionCreator(FlowableAstFunctionCreator astFunctionCreator) {
            //variables:get
            //vars:get
            //var:get
            for (String functionName : astFunctionCreator.getFunctionNames()) {
                astFunctionCreators.put(functionName, astFunctionCreator);
            }
        }

        public Map<String, FlowableAstFunctionCreator> getAstFunctionCreators() {
            return astFunctionCreators;
        }

        public FlowableAstFunctionCreator getAstFunctionCreator(String functionName) {
            return astFunctionCreators.getOrDefault(functionName, getDefaultFunctionCreator());
        }

        public FlowableAstFunctionCreator getDefaultFunctionCreator() {
            return defaultFunctionCreator;
        }

        public void setDefaultFunctionCreator(FlowableAstFunctionCreator defaultFunctionCreator) {
            this.defaultFunctionCreator = defaultFunctionCreator;
        }
    }

    protected static class FlowableExpressionParserImpl extends Parser implements FlowableExpressionParser {

        protected final FlowableExpressionBuilder flowableContext;

        public FlowableExpressionParserImpl(FlowableExpressionBuilder context, String input) {
            super(context, input);
            this.flowableContext = context;
        }

        @Override
        public AstIdentifier createIdentifier(String name) {
            return identifier(name);
        }

        @Override
        protected AstFunction createAstFunction(String name, int index, AstParameters params) {
            return flowableContext.getAstFunctionCreator(name).createFunction(name, index, params, context.isEnabled(Builder.Feature.VARARGS), this);
        }
    }

}
