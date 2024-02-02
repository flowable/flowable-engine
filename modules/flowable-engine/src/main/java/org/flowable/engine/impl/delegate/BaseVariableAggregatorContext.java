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
package org.flowable.engine.impl.delegate;

import org.flowable.bpmn.model.VariableAggregationDefinition;
import org.flowable.engine.delegate.variable.VariableAggregatorContext;

/**
 * @author Filip Hrisafov
 */
public class BaseVariableAggregatorContext implements VariableAggregatorContext {

    protected final VariableAggregationDefinition definition;
    protected final String state;

    public BaseVariableAggregatorContext(VariableAggregationDefinition definition, String state) {
        this.definition = definition;
        this.state = state;
    }

    @Override
    public VariableAggregationDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getState() {
        return state;
    }

    public static VariableAggregatorContext complete(VariableAggregationDefinition definition) {
        return new BaseVariableAggregatorContext(definition, VariableAggregatorContext.COMPLETE);
    }

    public static VariableAggregatorContext overview(VariableAggregationDefinition definition) {
        return new BaseVariableAggregatorContext(definition, VariableAggregatorContext.OVERVIEW);
    }
}
