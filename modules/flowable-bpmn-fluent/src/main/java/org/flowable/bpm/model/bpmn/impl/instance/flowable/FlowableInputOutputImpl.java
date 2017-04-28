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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_INPUT_OUTPUT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputOutput;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputParameter;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableOutputParameter;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN inputOutput Flowable extension element.
 */
public class FlowableInputOutputImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableInputOutput {

    protected static ChildElementCollection<FlowableInputParameter> flowableInputParameterCollection;
    protected static ChildElementCollection<FlowableOutputParameter> flowableOutputParameterCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableInputOutput.class, FLOWABLE_ELEMENT_INPUT_OUTPUT)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableInputOutput>() {
                    @Override
                    public FlowableInputOutput newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableInputOutputImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableInputParameterCollection = sequenceBuilder.elementCollection(FlowableInputParameter.class)
                .build();

        flowableOutputParameterCollection = sequenceBuilder.elementCollection(FlowableOutputParameter.class)
                .build();

        typeBuilder.build();
    }

    public FlowableInputOutputImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public Collection<FlowableInputParameter> getFlowableInputParameters() {
        return flowableInputParameterCollection.get(this);
    }

    @Override
    public Collection<FlowableOutputParameter> getFlowableOutputParameters() {
        return flowableOutputParameterCollection.get(this);
    }
}
