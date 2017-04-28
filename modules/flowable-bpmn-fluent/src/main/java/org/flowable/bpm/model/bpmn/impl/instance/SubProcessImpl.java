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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TRIGGERED_BY_EVENT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_SUB_PROCESS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASYNC;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.SubProcessBuilder;
import org.flowable.bpm.model.bpmn.instance.Activity;
import org.flowable.bpm.model.bpmn.instance.Artifact;
import org.flowable.bpm.model.bpmn.instance.FlowElement;
import org.flowable.bpm.model.bpmn.instance.LaneSet;
import org.flowable.bpm.model.bpmn.instance.SubProcess;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN subProcess element.
 */
public class SubProcessImpl
        extends ActivityImpl
        implements SubProcess {

    protected static Attribute<Boolean> triggeredByEventAttribute;
    protected static ChildElementCollection<LaneSet> laneSetCollection;
    protected static ChildElementCollection<FlowElement> flowElementCollection;
    protected static ChildElementCollection<Artifact> artifactCollection;

    /* Flowable extensions */
    protected static Attribute<Boolean> flowableAsyncAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(SubProcess.class, BPMN_ELEMENT_SUB_PROCESS)
                .namespaceUri(BPMN20_NS)
                .extendsType(Activity.class)
                .instanceProvider(new ModelTypeInstanceProvider<SubProcess>() {
                    @Override
                    public SubProcess newInstance(ModelTypeInstanceContext instanceContext) {
                        return new SubProcessImpl(instanceContext);
                    }
                });

        triggeredByEventAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_TRIGGERED_BY_EVENT)
                .defaultValue(false)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        laneSetCollection = sequenceBuilder.elementCollection(LaneSet.class)
                .build();

        flowElementCollection = sequenceBuilder.elementCollection(FlowElement.class)
                .build();

        artifactCollection = sequenceBuilder.elementCollection(Artifact.class)
                .build();

        /* Flowable extensions */

        flowableAsyncAttribute = typeBuilder.booleanAttribute(FLOWABLE_ATTRIBUTE_ASYNC)
                .namespace(FLOWABLE_NS)
                .defaultValue(false)
                .build();

        typeBuilder.build();
    }

    public SubProcessImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public SubProcessBuilder builder() {
        return new SubProcessBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public boolean triggeredByEvent() {
        return triggeredByEventAttribute.getValue(this);
    }

    @Override
    public void setTriggeredByEvent(boolean triggeredByEvent) {
        triggeredByEventAttribute.setValue(this, triggeredByEvent);
    }

    @Override
    public Collection<LaneSet> getLaneSets() {
        return laneSetCollection.get(this);
    }

    @Override
    public Collection<FlowElement> getFlowElements() {
        return flowElementCollection.get(this);
    }

    @Override
    public Collection<Artifact> getArtifacts() {
        return artifactCollection.get(this);
    }
}
