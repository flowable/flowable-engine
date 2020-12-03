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
package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class VariableAggregationDefinition {

    protected String implementationType;
    protected String implementation;

    protected String target;
    protected String targetExpression;
    protected List<Variable> definitions;

    protected boolean storeAsTransientVariable;
    protected boolean createOverviewVariable;

    public String getImplementationType() {
        return implementationType;
    }

    public void setImplementationType(String implementationType) {
        this.implementationType = implementationType;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTargetExpression() {
        return targetExpression;
    }

    public void setTargetExpression(String targetExpression) {
        this.targetExpression = targetExpression;
    }

    public List<Variable> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<Variable> definitions) {
        this.definitions = definitions;
    }

    public void addDefinition(Variable definition) {
        if (definitions == null) {
            definitions = new ArrayList<>();
        }

        definitions.add(definition);
    }

    public boolean isStoreAsTransientVariable() {
        return storeAsTransientVariable;
    }

    public void setStoreAsTransientVariable(boolean storeAsTransientVariable) {
        this.storeAsTransientVariable = storeAsTransientVariable;
    }

    public boolean isCreateOverviewVariable() {
        return createOverviewVariable;
    }

    public void setCreateOverviewVariable(boolean createOverviewVariable) {
        this.createOverviewVariable = createOverviewVariable;
    }

    @Override
    public VariableAggregationDefinition clone() {
        VariableAggregationDefinition aggregation = new VariableAggregationDefinition();
        aggregation.setValues(this);
        return aggregation;
    }

    public void setValues(VariableAggregationDefinition otherVariableDefinitionAggregation) {
        setImplementationType(otherVariableDefinitionAggregation.getImplementationType());
        setImplementation(otherVariableDefinitionAggregation.getImplementation());
        setTarget(otherVariableDefinitionAggregation.getTarget());
        setTargetExpression(otherVariableDefinitionAggregation.getTargetExpression());
        List<Variable> otherDefinitions = otherVariableDefinitionAggregation.getDefinitions();
        if (otherDefinitions != null) {
            List<Variable> newDefinitions = new ArrayList<>(otherDefinitions.size());
            for (Variable otherDefinition : otherDefinitions) {
                newDefinitions.add(otherDefinition.clone());
            }

            setDefinitions(newDefinitions);
        }
        setStoreAsTransientVariable(otherVariableDefinitionAggregation.isStoreAsTransientVariable());
        setCreateOverviewVariable(otherVariableDefinitionAggregation.isCreateOverviewVariable());
    }

    public static class Variable {

        protected String source;
        protected String target;
        protected String targetExpression;
        protected String sourceExpression;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getTargetExpression() {
            return targetExpression;
        }

        public void setTargetExpression(String targetExpression) {
            this.targetExpression = targetExpression;
        }

        public String getSourceExpression() {
            return sourceExpression;
        }

        public void setSourceExpression(String sourceExpression) {
            this.sourceExpression = sourceExpression;
        }

        @Override
        public Variable clone() {
            Variable definition = new Variable();
            definition.setValues(this);
            return definition;
        }

        public void setValues(Variable otherDefinition) {
            setSource(otherDefinition.getSource());
            setSourceExpression(otherDefinition.getSourceExpression());
            setTarget(otherDefinition.getTarget());
            setTargetExpression(otherDefinition.getTargetExpression());
        }
    }

}
