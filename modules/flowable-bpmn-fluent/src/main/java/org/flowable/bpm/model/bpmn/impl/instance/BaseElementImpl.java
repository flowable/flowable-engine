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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_BASE_ELEMENT;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.Documentation;
import org.flowable.bpm.model.bpmn.instance.ExtensionElements;
import org.flowable.bpm.model.bpmn.instance.di.DiagramElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.Reference;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The BPMN baseElement element.
 */
public abstract class BaseElementImpl
        extends BpmnModelElementInstanceImpl
        implements BaseElement {

    protected static Attribute<String> idAttribute;
    protected static ChildElementCollection<Documentation> documentationCollection;
    protected static ChildElement<ExtensionElements> extensionElementsChild;

    public static void registerType(ModelBuilder bpmnModelBuilder) {
        ModelElementTypeBuilder typeBuilder = bpmnModelBuilder.defineType(BaseElement.class, BPMN_ELEMENT_BASE_ELEMENT)
                .namespaceUri(BPMN20_NS)
                .abstractType();

        idAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ID)
                .idAttribute()
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        documentationCollection = sequenceBuilder.elementCollection(Documentation.class)
                .build();

        extensionElementsChild = sequenceBuilder.element(ExtensionElements.class)
                .build();

        typeBuilder.build();
    }

    public BaseElementImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getId() {
        return idAttribute.getValue(this);
    }

    public void setId(String id) {
        idAttribute.setValue(this, id);
    }

    public Collection<Documentation> getDocumentations() {
        return documentationCollection.get(this);
    }

    public ExtensionElements getExtensionElements() {
        return extensionElementsChild.getChild(this);
    }

    public void setExtensionElements(ExtensionElements extensionElements) {
        extensionElementsChild.setChild(this, extensionElements);
    }

    @SuppressWarnings("rawtypes")
    public DiagramElement getDiagramElement() {
        Collection<Reference> incomingReferences = getIncomingReferencesByType(DiagramElement.class);
        for (Reference<?> reference : incomingReferences) {
            for (ModelElementInstance sourceElement : reference.findReferenceSourceElements(this)) {
                String referenceIdentifier = reference.getReferenceIdentifier(sourceElement);
                if (referenceIdentifier != null && referenceIdentifier.equals(getId())) {
                    return (DiagramElement) sourceElement;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Collection<Reference> getIncomingReferencesByType(Class<? extends ModelElementInstance> referenceSourceTypeClass) {
        Collection<Reference> references = new ArrayList<>();
        // we traverse all incoming references in reverse direction
        for (Reference<?> reference : idAttribute.getIncomingReferences()) {

            ModelElementType sourceElementType = reference.getReferenceSourceElementType();
            Class<? extends ModelElementInstance> sourceInstanceType = sourceElementType.getInstanceType();

            // if the referencing element (source element) is a BPMNDI element, dig deeper
            if (referenceSourceTypeClass.isAssignableFrom(sourceInstanceType)) {
                references.add(reference);
            }
        }
        return references;
    }

}
