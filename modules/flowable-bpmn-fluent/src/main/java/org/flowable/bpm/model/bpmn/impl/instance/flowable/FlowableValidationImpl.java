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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ELEMENT_VALIDATION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableConstraint;
import org.flowable.bpm.model.bpmn.instance.flowable.FlowableValidation;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN validation Flowable extension element.
 */
public class FlowableValidationImpl
        extends BpmnModelElementInstanceImpl
        implements FlowableValidation {

    protected static ChildElementCollection<FlowableConstraint> flowableConstraintCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowableValidation.class, FLOWABLE_ELEMENT_VALIDATION)
                .namespaceUri(FLOWABLE_NS)
                .instanceProvider(new ModelTypeInstanceProvider<FlowableValidation>() {
                    public FlowableValidation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new FlowableValidationImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        flowableConstraintCollection = sequenceBuilder.elementCollection(FlowableConstraint.class)
                .build();

        typeBuilder.build();
    }

    public FlowableValidationImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Collection<FlowableConstraint> getFlowableConstraints() {
        return flowableConstraintCollection.get(this);
    }
}
