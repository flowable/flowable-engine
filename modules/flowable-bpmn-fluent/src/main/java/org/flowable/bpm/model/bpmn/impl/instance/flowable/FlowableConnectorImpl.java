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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_CONNECTOR;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableConnector;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableConnectorId;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableInputOutput;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN connector Flowable extension element.
 */
public class FlowableConnectorImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableConnector {

    protected static ChildElement<FlowableConnectorId> flowableConnectorIdChild;
    protected static ChildElement<FlowableInputOutput> flowableInputOutputChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableConnector.class, FLOWABLE_ELEMENT_CONNECTOR)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableConnector>() {
                    public FlowableConnector newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableConnectorImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableConnectorIdChild = sequenceBuilder.element(FlowableConnectorId.class)
                .required()
                .build();

        flowableInputOutputChild = sequenceBuilder.element(FlowableInputOutput.class)
                .build();

        typeBuilder.build();
    }

    public FlowableConnectorImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public FlowableConnectorId getFlowableConnectorId() {
        return flowableConnectorIdChild.getChild(this);
    }

    public void setFlowableConnectorId(FlowableConnectorId flowableConnectorId) {
        flowableConnectorIdChild.setChild(this, flowableConnectorId);
    }

    public FlowableInputOutput getFlowableInputOutput() {
        return flowableInputOutputChild.getChild(this);
    }

    public void setFlowableInputOutput(FlowableInputOutput flowableInputOutput) {
        flowableInputOutputChild.setChild(this, flowableInputOutput);
    }

}
