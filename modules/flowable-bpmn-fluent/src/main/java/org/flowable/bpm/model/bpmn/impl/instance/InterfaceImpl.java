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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_INTERFACE;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Interface;
import org.flowable.bpm.model.bpmn.instance.Operation;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN interface element.
 */
public class InterfaceImpl
        extends RootElementImpl
        implements Interface {

    protected static Attribute<String> nameAttribute;
    protected static Attribute<String> implementationRefAttribute;
    protected static ChildElementCollection<Operation> operationCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Interface.class, BPMN_ELEMENT_INTERFACE)
                .namespaceUri(BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Interface>() {
                    @Override
                    public Interface newInstance(ModelTypeInstanceContext instanceContext) {
                        return new InterfaceImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .required()
                .build();

        implementationRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION_REF)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        operationCollection = sequenceBuilder.elementCollection(Operation.class)
                .required()
                .build();

        typeBuilder.build();
    }

    public InterfaceImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return nameAttribute.getValue(this);
    }

    @Override
    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    @Override
    public String getImplementationRef() {
        return implementationRefAttribute.getValue(this);
    }

    @Override
    public void setImplementationRef(String implementationRef) {
        implementationRefAttribute.setValue(this, implementationRef);
    }

    @Override
    public Collection<Operation> getOperations() {
        return operationCollection.get(this);
    }
}
