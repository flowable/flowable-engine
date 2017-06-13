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
package org.flowable.bpm.model.bpmn.impl.instance;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_BEHAVIOR;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_IS_SEQUENTIAL;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_MULTI_INSTANCE_LOOP_CHARACTERISTICS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_NONE_BEHAVIOR_EVENT_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ONE_BEHAVIOR_EVENT_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_COLLECTION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ELEMENT_VARIABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_EXCLUSIVE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.MultiInstanceFlowCondition;
import org.flowable.bpm.model.bpmn.builder.MultiInstanceLoopCharacteristicsBuilder;
import org.flowable.bpm.model.bpmn.instance.CompletionCondition;
import org.flowable.bpm.model.bpmn.instance.ComplexBehaviorDefinition;
import org.flowable.bpm.model.bpmn.instance.DataInput;
import org.flowable.bpm.model.bpmn.instance.DataOutput;
import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.bpmn.instance.InputDataItem;
import org.flowable.bpm.model.bpmn.instance.LoopCardinality;
import org.flowable.bpm.model.bpmn.instance.LoopCharacteristics;
import org.flowable.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import org.flowable.bpm.model.bpmn.instance.OutputDataItem;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;
import org.flowable.bpm.model.xml.type.reference.ElementReference;

import java.util.Collection;

/**
 * The BPMN 2.0 multiInstanceLoopCharacteristics element.
 */
public class MultiInstanceLoopCharacteristicsImpl
        extends LoopCharacteristicsImpl
        implements MultiInstanceLoopCharacteristics {

    protected static Attribute<Boolean> isSequentialAttribute;
    protected static Attribute<MultiInstanceFlowCondition> behaviorAttribute;
    protected static AttributeReference<EventDefinition> oneBehaviorEventRefAttribute;
    protected static AttributeReference<EventDefinition> noneBehaviorEventRefAttribute;
    protected static ChildElement<LoopCardinality> loopCardinalityChild;
    protected static ElementReference<DataInput, LoopDataInputRef> loopDataInputRefChild;
    protected static ElementReference<DataOutput, LoopDataOutputRef> loopDataOutputRefChild;
    protected static ChildElement<InputDataItem> inputDataItemChild;
    protected static ChildElement<OutputDataItem> outputDataItemChild;
    protected static ChildElementCollection<ComplexBehaviorDefinition> complexBehaviorDefinitionCollection;
    protected static ChildElement<CompletionCondition> completionConditionChild;
    protected static Attribute<Boolean> flowableAsync;
    protected static Attribute<Boolean> flowableExclusive;
    protected static Attribute<String> flowableCollection;
    protected static Attribute<String> flowableElementVariable;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder
                .defineType(MultiInstanceLoopCharacteristics.class, BPMN_ELEMENT_MULTI_INSTANCE_LOOP_CHARACTERISTICS)
                .namespaceUri(BPMN20_NS)
                .extendsType(LoopCharacteristics.class)
                .instanceProvider(new ModelTypeInstanceProvider<MultiInstanceLoopCharacteristics>() {

                    @Override
                    public MultiInstanceLoopCharacteristics newInstance(ModelTypeInstanceContext instanceContext) {
                        return new MultiInstanceLoopCharacteristicsImpl(instanceContext);
                    }
                });

        isSequentialAttribute = typeBuilder.booleanAttribute(BPMN_ELEMENT_IS_SEQUENTIAL)
                .defaultValue(false)
                .build();

        behaviorAttribute = typeBuilder.enumAttribute(BPMN_ELEMENT_BEHAVIOR, MultiInstanceFlowCondition.class)
                .defaultValue(MultiInstanceFlowCondition.All)
                .build();

        oneBehaviorEventRefAttribute = typeBuilder.stringAttribute(BPMN_ELEMENT_ONE_BEHAVIOR_EVENT_REF)
                .qNameAttributeReference(EventDefinition.class)
                .build();

        noneBehaviorEventRefAttribute = typeBuilder.stringAttribute(BPMN_ELEMENT_NONE_BEHAVIOR_EVENT_REF)
                .qNameAttributeReference(EventDefinition.class)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        loopCardinalityChild = sequenceBuilder.element(LoopCardinality.class)
                .build();

        loopDataInputRefChild = sequenceBuilder.element(LoopDataInputRef.class)
                .qNameElementReference(DataInput.class)
                .build();

        loopDataOutputRefChild = sequenceBuilder.element(LoopDataOutputRef.class)
                .qNameElementReference(DataOutput.class)
                .build();

        outputDataItemChild = sequenceBuilder.element(OutputDataItem.class)
                .build();

        inputDataItemChild = sequenceBuilder.element(InputDataItem.class)
                .build();

        complexBehaviorDefinitionCollection = sequenceBuilder.elementCollection(ComplexBehaviorDefinition.class)
                .build();

        completionConditionChild = sequenceBuilder.element(CompletionCondition.class)
                .build();

        flowableAsync = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        flowableExclusive = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_EXCLUSIVE)
                .namespace(FLOWABLE_NS)
                .defaultValue(true)
                .build();

        flowableCollection = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_COLLECTION)
                .namespace(FLOWABLE_NS)
                .build();

        flowableElementVariable = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_ELEMENT_VARIABLE)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public MultiInstanceLoopCharacteristicsImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public MultiInstanceLoopCharacteristicsBuilder builder() {
        return new MultiInstanceLoopCharacteristicsBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public LoopCardinality getLoopCardinality() {
        return loopCardinalityChild.getChild(this);
    }

    @Override
    public void setLoopCardinality(LoopCardinality loopCardinality) {
        loopCardinalityChild.setChild(this, loopCardinality);
    }

    @Override
    public DataInput getLoopDataInputRef() {
        return loopDataInputRefChild.getReferenceTargetElement(this);
    }

    @Override
    public void setLoopDataInputRef(DataInput loopDataInputRef) {
        loopDataInputRefChild.setReferenceTargetElement(this, loopDataInputRef);
    }

    @Override
    public DataOutput getLoopDataOutputRef() {
        return loopDataOutputRefChild.getReferenceTargetElement(this);
    }

    @Override
    public void setLoopDataOutputRef(DataOutput loopDataOutputRef) {
        loopDataOutputRefChild.setReferenceTargetElement(this, loopDataOutputRef);
    }

    @Override
    public InputDataItem getInputDataItem() {
        return inputDataItemChild.getChild(this);
    }

    @Override
    public void setInputDataItem(InputDataItem inputDataItem) {
        inputDataItemChild.setChild(this, inputDataItem);
    }

    @Override
    public OutputDataItem getOutputDataItem() {
        return outputDataItemChild.getChild(this);
    }

    @Override
    public void setOutputDataItem(OutputDataItem outputDataItem) {
        outputDataItemChild.setChild(this, outputDataItem);
    }

    @Override
    public Collection<ComplexBehaviorDefinition> getComplexBehaviorDefinitions() {
        return complexBehaviorDefinitionCollection.get(this);
    }

    @Override
    public CompletionCondition getCompletionCondition() {
        return completionConditionChild.getChild(this);
    }

    @Override
    public void setCompletionCondition(CompletionCondition completionCondition) {
        completionConditionChild.setChild(this, completionCondition);
    }

    @Override
    public boolean isSequential() {
        return isSequentialAttribute.getValue(this);
    }

    @Override
    public void setSequential(boolean sequential) {
        isSequentialAttribute.setValue(this, sequential);
    }

    @Override
    public MultiInstanceFlowCondition getBehavior() {
        return behaviorAttribute.getValue(this);
    }

    @Override
    public void setBehavior(MultiInstanceFlowCondition behavior) {
        behaviorAttribute.setValue(this, behavior);
    }

    @Override
    public EventDefinition getOneBehaviorEventRef() {
        return oneBehaviorEventRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setOneBehaviorEventRef(EventDefinition oneBehaviorEventRef) {
        oneBehaviorEventRefAttribute.setReferenceTargetElement(this, oneBehaviorEventRef);
    }

    @Override
    public EventDefinition getNoneBehaviorEventRef() {
        return noneBehaviorEventRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setNoneBehaviorEventRef(EventDefinition noneBehaviorEventRef) {
        noneBehaviorEventRefAttribute.setReferenceTargetElement(this, noneBehaviorEventRef);
    }

    @Override
    public boolean isFlowableAsync() {
        return flowableAsync.getValue(this);
    }

    @Override
    public void setFlowableAsync(boolean isFlowableAsync) {
        flowableAsync.setValue(this, isFlowableAsync);
    }

    @Override
    public boolean isFlowableExclusive() {
        return flowableExclusive.getValue(this);
    }

    @Override
    public void setFlowableExclusive(boolean isFlowableExclusive) {
        flowableExclusive.setValue(this, isFlowableExclusive);
    }

    @Override
    public String getFlowableCollection() {
        return flowableCollection.getValue(this);
    }

    @Override
    public void setFlowableCollection(String expression) {
        flowableCollection.setValue(this, expression);
    }

    @Override
    public String getFlowableElementVariable() {
        return flowableElementVariable.getValue(this);
    }

    @Override
    public void setFlowableElementVariable(String variableName) {
        flowableElementVariable.setValue(this, variableName);
    }
}
