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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_IMPLEMENTATION_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_OPERATION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.BaseElement;
import org.flowable.bpm.model.bpmn.instance.Error;
import org.flowable.bpm.model.bpmn.instance.Message;
import org.flowable.bpm.model.bpmn.instance.Operation;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReference;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;

/**
 * The BPMN operation element.
 */
public class OperationImpl
        extends BaseElementImpl
        implements Operation {

    protected static Attribute<String> nameAttribute;
    protected static Attribute<String> implementationRefAttribute;
    protected static ElementReference<Message, InMessageRef> inMessageRefChild;
    protected static ElementReference<Message, OutMessageRef> outMessageRefChild;
    protected static ElementReferenceCollection<Error, ErrorRef> errorRefCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Operation.class, BPMN_ELEMENT_OPERATION)
                .namespaceUri(BPMN20_NS)
                .extendsType(BaseElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Operation>() {
                    public Operation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new OperationImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .required()
                .build();

        implementationRefAttribute = typeBuilder.stringAttribute(BPMN_ELEMENT_IMPLEMENTATION_REF)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        inMessageRefChild = sequenceBuilder.element(InMessageRef.class)
                .required()
                .qNameElementReference(Message.class)
                .build();

        outMessageRefChild = sequenceBuilder.element(OutMessageRef.class)
                .qNameElementReference(Message.class)
                .build();

        errorRefCollection = sequenceBuilder.elementCollection(ErrorRef.class)
                .qNameElementReferenceCollection(Error.class)
                .build();

        typeBuilder.build();
    }

    public OperationImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public String getImplementationRef() {
        return implementationRefAttribute.getValue(this);
    }

    public void setImplementationRef(String implementationRef) {
        implementationRefAttribute.setValue(this, implementationRef);
    }

    public Message getInMessage() {
        return inMessageRefChild.getReferenceTargetElement(this);
    }

    public void setInMessage(Message message) {
        inMessageRefChild.setReferenceTargetElement(this, message);
    }

    public Message getOutMessage() {
        return outMessageRefChild.getReferenceTargetElement(this);
    }

    public void setOutMessage(Message message) {
        outMessageRefChild.setReferenceTargetElement(this, message);
    }

    public Collection<Error> getErrors() {
        return errorRefCollection.getReferenceTargetElements(this);
    }
}
