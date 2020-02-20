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
package org.flowable.engine.impl.bpmn.data;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A transformation based data output association
 * 
 * @author Esteban Robles Luna
 */
public class TransformationDataOutputAssociation extends AbstractDataAssociation {

    private static final long serialVersionUID = 1L;

    protected Expression transformation;

    public TransformationDataOutputAssociation(String sourceRef, String targetRef, Expression transformation) {
        super(sourceRef, targetRef);
        this.transformation = transformation;
    }

    @Override
    public void evaluate(DelegateExecution execution) {
        Object value = this.transformation.getValue(execution);

        VariableTypes variableTypes = CommandContextUtil.getVariableServiceConfiguration().getVariableTypes();
        try {
            variableTypes.findVariableType(value);
        } catch (final FlowableException e) {
            // Couldn't find a variable type that is able to serialize the output value
            // Perhaps the output value is a Java bean, we ry to convert it as JSon
            try {
                value = new ObjectMapper().convertValue(value, JsonNode.class);
            } catch (final IllegalArgumentException e1) {
                throw new FlowableException("An error occurs converting output value as JSon", e1);
            }
        }

        execution.setVariable(this.getTarget(), value);
    }
}
