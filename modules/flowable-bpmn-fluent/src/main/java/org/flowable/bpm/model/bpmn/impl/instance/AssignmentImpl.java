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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ASSIGNMENT;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Assignment;
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

/**
 * The BPMN assignment element.
 */
public class AssignmentImpl
        extends BaseElementImpl
        implements Assignment {

    protected static ChildElement<From> fromChild;
    protected static ChildElement<To> toChild;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Assignment.class, BPMN_ELEMENT_ASSIGNMENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Assignment>() {
                    public Assignment newInstance(ModelTypeInstanceContext instanceContext) {
                        return new AssignmentImpl(instanceContext);
                    }
                });

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        fromChild = sequenceBuilder.element(From.class)
                .required()
                .build();

        toChild = sequenceBuilder.element(To.class)
                .required()
                .build();

        typeBuilder.build();
    }

    public AssignmentImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public From getFrom() {
        return fromChild.getChild(this);
    }

    public void setFrom(From from) {
        fromChild.setChild(this, from);
    }

    public To getTo() {
        return toChild.getChild(this);
    }

    public void setTo(To to) {
        toChild.setChild(this, to);
    }
}
