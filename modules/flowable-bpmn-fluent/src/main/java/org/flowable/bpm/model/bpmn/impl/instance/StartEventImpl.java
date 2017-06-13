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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IS_INTERRUPTING;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_START_EVENT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_FORM_HANDLER_CLASS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_FORM_KEY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_INITIATOR;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.StartEventBuilder;
import org.flowable.bpm.model.bpmn.instance.CatchEvent;
import org.flowable.bpm.model.bpmn.instance.StartEvent;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN startEvent element.
 */
public class StartEventImpl
        extends CatchEventImpl
        implements StartEvent {

    protected static Attribute<Boolean> isInterruptingAttribute;

    /* Flowable extensions */

    protected static Attribute<Boolean> flowableAsyncAttribute;
    protected static Attribute<String> flowableFormHandlerClassAttribute;
    protected static Attribute<String> flowableFormKeyAttribute;
    protected static Attribute<String> flowableInitiatorAttribute;

    public static void registerType(ModelBuilder modelBuilder) {

        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(StartEvent.class, BPMN_ELEMENT_START_EVENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(CatchEvent.class)
                .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<StartEvent>() {
                    @Override
                    public StartEvent newInstance(ModelTypeInstanceContext instanceContext) {
                        return new StartEventImpl(instanceContext);
                    }
                });

        isInterruptingAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_INTERRUPTING)
                .defaultValue(true)
                .build();

        /* Flowable extensions */

        flowableAsyncAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        flowableFormHandlerClassAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_FORM_HANDLER_CLASS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableFormKeyAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_FORM_KEY)
                .namespace(FLOWABLE_NS)
                .build();

        flowableInitiatorAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_INITIATOR)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public StartEventImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public StartEventBuilder builder() {
        return new StartEventBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public boolean isInterrupting() {
        return isInterruptingAttribute.getValue(this);
    }

    @Override
    public void setInterrupting(boolean isInterrupting) {
        isInterruptingAttribute.setValue(this, isInterrupting);
    }

    @Override
    public String getFlowableFormHandlerClass() {
        return flowableFormHandlerClassAttribute.getValue(this);
    }

    @Override
    public void setFlowableFormHandlerClass(String flowableFormHandlerClass) {
        flowableFormHandlerClassAttribute.setValue(this, flowableFormHandlerClass);
    }

    @Override
    public String getFlowableFormKey() {
        return flowableFormKeyAttribute.getValue(this);
    }

    @Override
    public void setFlowableFormKey(String flowableFormKey) {
        flowableFormKeyAttribute.setValue(this, flowableFormKey);
    }

    @Override
    public String getFlowableInitiator() {
        return flowableInitiatorAttribute.getValue(this);
    }

    @Override
    public void setFlowableInitiator(String flowableInitiator) {
        flowableInitiatorAttribute.setValue(this, flowableInitiator);
    }
}
