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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CALLED_ELEMENT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CALL_ACTIVITY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.CallActivityBuilder;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.CallActivity;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN callActivity element.
 */
public class CallActivityImpl
        extends ActivityImpl
        implements CallActivity {

    protected static Attribute<String> calledElementAttribute;
    protected static Attribute<Boolean> flowableAsyncAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(CallActivity.class, BPMN_ELEMENT_CALL_ACTIVITY)
                .namespaceUri(BPMN20_NS)
                .extendsType(Activity.class)
                .instanceProvider(new ModelTypeInstanceProvider<CallActivity>() {
                    public CallActivity newInstance(ModelTypeInstanceContext instanceContext) {
                        return new CallActivityImpl(instanceContext);
                    }
                });

        calledElementAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_CALLED_ELEMENT)
                .build();

        /* Flowable extensions */

        flowableAsyncAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        typeBuilder.build();
    }

    public CallActivityImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public CallActivityBuilder builder() {
        return new CallActivityBuilder((BpmnModelInstance) modelInstance, this);
    }

    public String getCalledElement() {
        return calledElementAttribute.getValue(this);
    }

    public void setCalledElement(String calledElement) {
        calledElementAttribute.setValue(this, calledElement);
    }
}
