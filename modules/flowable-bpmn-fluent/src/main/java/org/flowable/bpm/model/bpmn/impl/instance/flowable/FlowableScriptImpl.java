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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_RESOURCE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_SCRIPT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableScript;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN script Flowable extension element.
 */
public class FlowableScriptImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableScript {

    protected static Attribute<String> flowableResourceAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableScript.class, FLOWABLE_ELEMENT_SCRIPT)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableScript>() {
                    public FlowableScript newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableScriptImpl(instanceContext);
                    }
                });

        flowableResourceAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_RESOURCE)
                .build();

        typeBuilder.build();
    }

    public FlowableScriptImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getFlowableResource() {
        return flowableResourceAttribute.getValue(this);
    }

    public void setFlowableResource(String flowableResource) {
        flowableResourceAttribute.setValue(this, flowableResource);
    }
}
