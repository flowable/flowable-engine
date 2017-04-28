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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_OPERATION_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_SERVICE_TASK;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_CLASS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_DELEGATE_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_EXPRESSION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_RESULT_VARIABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.ServiceTaskBuilder;
import org.flowable.bpm.model.bpmn.instance.Operation;
import org.flowable.bpm.model.bpmn.instance.ServiceTask;
import org.flowable.bpm.model.bpmn.instance.Task;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN serviceTask element.
 */
public class ServiceTaskImpl
        extends TaskImpl
        implements ServiceTask {

    protected static Attribute<String> implementationAttribute;
    protected static AttributeReference<Operation> operationRefAttribute;

    /* Flowable extensions */

    protected static Attribute<String> flowableClassAttribute;
    protected static Attribute<String> flowableDelegateExpressionAttribute;
    protected static Attribute<String> flowableExpressionAttribute;
    protected static Attribute<String> flowableResultVariableAttribute;
    protected static Attribute<String> flowableTypeAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ServiceTask.class, BPMN_ELEMENT_SERVICE_TASK)
                .namespaceUri(BPMN20_NS)
                .extendsType(Task.class)
                .instanceProvider(new ModelTypeInstanceProvider<ServiceTask>() {
                    public ServiceTask newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ServiceTaskImpl(instanceContext);
                    }
                });

        implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
                .defaultValue("##WebService")
                .build();

        operationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_OPERATION_REF)
                .qNameAttributeReference(Operation.class)
                .build();

        /* Flowable extensions */

        flowableClassAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_CLASS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableDelegateExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_DELEGATE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        flowableExpressionAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_EXPRESSION)
                .namespace(FLOWABLE_NS)
                .build();

        flowableResultVariableAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_RESULT_VARIABLE)
                .namespace(FLOWABLE_NS)
                .build();

        flowableTypeAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_TYPE)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public ServiceTaskImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public ServiceTaskBuilder builder() {
        return new ServiceTaskBuilder((BpmnModelInstance) modelInstance, this);
    }

    public String getImplementation() {
        return implementationAttribute.getValue(this);
    }

    public void setImplementation(String implementation) {
        implementationAttribute.setValue(this, implementation);
    }

    public Operation getOperation() {
        return operationRefAttribute.getReferenceTargetElement(this);
    }

    public void setOperation(Operation operation) {
        operationRefAttribute.setReferenceTargetElement(this, operation);
    }

    /* Flowable extensions */

    public String getFlowableClass() {
        return flowableClassAttribute.getValue(this);
    }

    public void setFlowableClass(String flowableClass) {
        flowableClassAttribute.setValue(this, flowableClass);
    }

    public String getFlowableDelegateExpression() {
        return flowableDelegateExpressionAttribute.getValue(this);
    }

    public void setFlowableDelegateExpression(String flowableExpression) {
        flowableDelegateExpressionAttribute.setValue(this, flowableExpression);
    }

    public String getFlowableExpression() {
        return flowableExpressionAttribute.getValue(this);
    }

    public void setFlowableExpression(String flowableExpression) {
        flowableExpressionAttribute.setValue(this, flowableExpression);
    }

    public String getFlowableResultVariable() {
        return flowableResultVariableAttribute.getValue(this);
    }

    public void setFlowableResultVariable(String flowableResultVariable) {
        flowableResultVariableAttribute.setValue(this, flowableResultVariable);
    }

    public String getFlowableType() {
        return flowableTypeAttribute.getValue(this);
    }

    public void setFlowableType(String flowableType) {
        flowableTypeAttribute.setValue(this, flowableType);
    }
}
