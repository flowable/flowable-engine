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
package org.flowable.bpm.model.bpmn.impl.instance.flowable;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_FORM_DATA;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.BpmnModelConstants;
import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormData;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableFormField;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN formData Flowable extension element.
 */
public class FlowableFormDataImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableFormData {

    protected static ChildElementCollection<FlowableFormField> flowableFormFieldCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableFormData.class, FLOWABLE_ELEMENT_FORM_DATA)
                .namespaceUri(BpmnModelConstants.FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableFormData>() {
                    @Override
                    public FlowableFormData newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableFormDataImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableFormFieldCollection = sequenceBuilder.elementCollection(FlowableFormField.class)
                .build();

        typeBuilder.build();
    }

    public FlowableFormDataImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public Collection<FlowableFormField> getFlowableFormFields() {
        return flowableFormFieldCollection.get(this);
    }
}
