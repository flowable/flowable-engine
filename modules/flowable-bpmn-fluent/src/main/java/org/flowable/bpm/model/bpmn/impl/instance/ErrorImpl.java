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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ERROR_CODE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_STRUCTURE_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ERROR;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.impl.BpmnModelConstants;
import org.flowable.bpm.model.bpmn.instance.Error;
import org.flowable.bpm.model.bpmn.instance.ItemDefinition;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

public class ErrorImpl
        extends RootElementImpl
        implements Error {

    protected static Attribute<String> nameAttribute;
    protected static Attribute<String> errorCodeAttribute;

    protected static AttributeReference<ItemDefinition> structureRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Error.class, BPMN_ELEMENT_ERROR)
                .namespaceUri(BpmnModelConstants.BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Error>() {
                    public Error newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ErrorImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .build();

        errorCodeAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ERROR_CODE)
                .build();

        structureRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_STRUCTURE_REF)
                .qNameAttributeReference(ItemDefinition.class)
                .build();

        typeBuilder.build();
    }

    public ErrorImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public String getErrorCode() {
        return errorCodeAttribute.getValue(this);
    }

    public void setErrorCode(String errorCode) {
        errorCodeAttribute.setValue(this, errorCode);
    }

    public ItemDefinition getStructure() {
        return structureRefAttribute.getReferenceTargetElement(this);
    }

    public void setStructure(ItemDefinition structure) {
        structureRefAttribute.setReferenceTargetElement(this, structure);
    }
}
