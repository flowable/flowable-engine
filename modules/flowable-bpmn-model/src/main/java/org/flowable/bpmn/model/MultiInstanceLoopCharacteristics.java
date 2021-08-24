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
package org.flowable.bpmn.model;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class MultiInstanceLoopCharacteristics extends BaseElement {

    protected String inputDataItem;
    protected String collectionString;
    protected CollectionHandler collectionHandler;
    protected String loopCardinality;
    protected String completionCondition;
    protected String elementVariable;
    protected String elementIndexVariable;
    protected boolean sequential;
    protected boolean noWaitStatesAsyncLeave;

    protected VariableAggregationDefinitions aggregations;

    public String getInputDataItem() {
        return inputDataItem;
    }

    public void setInputDataItem(String inputDataItem) {
        this.inputDataItem = inputDataItem;
    }

    public String getCollectionString() {
        return collectionString;
    }

    public void setCollectionString(String collectionString) {
        this.collectionString = collectionString;
    }

    public CollectionHandler getHandler() {
		return collectionHandler;
	}

	public void setHandler(CollectionHandler collectionHandler) {
		this.collectionHandler = collectionHandler;
	}

	public String getLoopCardinality() {
        return loopCardinality;
    }

    public void setLoopCardinality(String loopCardinality) {
        this.loopCardinality = loopCardinality;
    }

    public String getCompletionCondition() {
        return completionCondition;
    }

    public void setCompletionCondition(String completionCondition) {
        this.completionCondition = completionCondition;
    }

    public String getElementVariable() {
        return elementVariable;
    }

    public void setElementVariable(String elementVariable) {
        this.elementVariable = elementVariable;
    }

    public String getElementIndexVariable() {
        return elementIndexVariable;
    }

    public void setElementIndexVariable(String elementIndexVariable) {
        this.elementIndexVariable = elementIndexVariable;
    }

    public boolean isSequential() {
        return sequential;
    }

    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }

    public boolean isNoWaitStatesAsyncLeave() {
        return noWaitStatesAsyncLeave;
    }

    public void setNoWaitStatesAsyncLeave(boolean noWaitStatesAsyncLeave) {
        this.noWaitStatesAsyncLeave = noWaitStatesAsyncLeave;
    }

    public VariableAggregationDefinitions getAggregations() {
        return aggregations;
    }

    public void setAggregations(VariableAggregationDefinitions aggregations) {
        this.aggregations = aggregations;
    }

    public void addAggregation(VariableAggregationDefinition aggregation) {
        if (this.aggregations == null) {
            this.aggregations = new VariableAggregationDefinitions();
        }

        this.aggregations.getAggregations().add(aggregation);
    }

    @Override
    public MultiInstanceLoopCharacteristics clone() {
        MultiInstanceLoopCharacteristics clone = new MultiInstanceLoopCharacteristics();
        clone.setValues(this);
        return clone;
    }

    public void setValues(MultiInstanceLoopCharacteristics otherLoopCharacteristics) {
        setInputDataItem(otherLoopCharacteristics.getInputDataItem());
        setCollectionString(otherLoopCharacteristics.getCollectionString());
        if (otherLoopCharacteristics.getHandler() != null) {
        	setHandler(otherLoopCharacteristics.getHandler().clone());
        }
        setLoopCardinality(otherLoopCharacteristics.getLoopCardinality());
        setCompletionCondition(otherLoopCharacteristics.getCompletionCondition());
        setElementVariable(otherLoopCharacteristics.getElementVariable());
        setElementIndexVariable(otherLoopCharacteristics.getElementIndexVariable());
        setSequential(otherLoopCharacteristics.isSequential());
        setNoWaitStatesAsyncLeave(otherLoopCharacteristics.isNoWaitStatesAsyncLeave());

        if (otherLoopCharacteristics.getAggregations() != null) {
            setAggregations(otherLoopCharacteristics.getAggregations().clone());
        }
    }
}
