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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_FLOW_ELEMENT;

import org.flowable.bpm.model.bpmn.instance.Auditing;
import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.CategoryValue;
import org.flowable.bpm.model.bpmn.instance.FlowElement;
import org.flowable.bpm.model.bpmn.instance.Monitoring;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN flowElement element.
 */
public abstract class FlowElementImpl
        extends BaseElementImpl
        implements FlowElement {

    protected static Attribute<String> nameAttribute;
    protected static ChildElement<Auditing> auditingChild;
    protected static ChildElement<Monitoring> monitoringChild;
    protected static ElementReferenceCollection<CategoryValue, CategoryValueRef> categoryValueRefCollection;

    public static void registerType(ModelBuilder modelBuilder) {

        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(FlowElement.class, BPMN_ELEMENT_FLOW_ELEMENT)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .abstractType();

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        auditingChild = sequenceBuilder.element(Auditing.class)
                .build();

        monitoringChild = sequenceBuilder.element(Monitoring.class)
                .build();

        categoryValueRefCollection = sequenceBuilder.elementCollection(CategoryValueRef.class)
                .qNameElementReferenceCollection(CategoryValue.class)
                .build();

        typeBuilder.build();
    }

    public FlowElementImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public Auditing getAuditing() {
        return auditingChild.getChild(this);
    }

    public void setAuditing(Auditing auditing) {
        auditingChild.setChild(this, auditing);
    }

    public Monitoring getMonitoring() {
        return monitoringChild.getChild(this);
    }

    public void setMonitoring(Monitoring monitoring) {
        monitoringChild.setChild(this, monitoring);
    }

    public Collection<CategoryValue> getCategoryValueRefs() {
        return categoryValueRefCollection.getReferenceTargetElements(this);
    }
}
