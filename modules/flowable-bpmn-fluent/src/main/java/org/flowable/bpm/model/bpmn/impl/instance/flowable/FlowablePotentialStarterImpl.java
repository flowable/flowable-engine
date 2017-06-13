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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_POTENTIAL_STARTER;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.ResourceAssignmentExpression;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowablePotentialStarter;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN potentialStarter Flowable extension.
 */
public class FlowablePotentialStarterImpl
        extends BpmnModelElementInstanceImpl
        implements FlowablePotentialStarter {

    protected static ChildElement<ResourceAssignmentExpression> resourceAssignmentExpressionChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowablePotentialStarter.class, FLOWABLE_ELEMENT_POTENTIAL_STARTER)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowablePotentialStarter>() {
                    @Override
                    public FlowablePotentialStarter newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowablePotentialStarterImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        resourceAssignmentExpressionChild = sequenceBuilder.element(ResourceAssignmentExpression.class)
                .build();

        typeBuilder.build();
    }

    public FlowablePotentialStarterImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public ResourceAssignmentExpression getResourceAssignmentExpression() {
        return resourceAssignmentExpressionChild.getChild(this);
    }

    @Override
    public void setResourceAssignmentExpression(ResourceAssignmentExpression resourceAssignmentExpression) {
        resourceAssignmentExpressionChild.setChild(this, resourceAssignmentExpression);
    }
}
