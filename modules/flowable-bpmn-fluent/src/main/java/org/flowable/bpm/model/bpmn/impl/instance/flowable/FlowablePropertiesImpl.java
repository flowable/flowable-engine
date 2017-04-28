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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_PROPERTIES;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableProperties;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableProperty;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN properties Flowable extension element.
 */
public class FlowablePropertiesImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableProperties {

    protected static ChildElementCollection<FlowableProperty> flowablePropertyCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableProperties.class, FLOWABLE_ELEMENT_PROPERTIES)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableProperties>() {
                    public FlowableProperties newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowablePropertiesImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowablePropertyCollection = sequenceBuilder.elementCollection(FlowableProperty.class)
                .build();

        typeBuilder.build();
    }

    public FlowablePropertiesImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Collection<FlowableProperty> getFlowableProperties() {
        return flowablePropertyCollection.get(this);
    }
}
