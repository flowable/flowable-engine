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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ERROR_REF;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ERROR_EVENT_DEFINITION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Error;
import org.flowable.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.flowable.bpm.model.bpmn.instance.EventDefinition;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN errorEventDefinition element.
 */
public class ErrorEventDefinitionImpl
        extends EventDefinitionImpl
        implements ErrorEventDefinition {

    protected static AttributeReference<Error> errorRefAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ErrorEventDefinition.class, BPMN_ELEMENT_ERROR_EVENT_DEFINITION)
                .namespaceUri(BPMN20_NS)
                .extendsType(EventDefinition.class)
                .instanceProvider(new ModelTypeInstanceProvider<ErrorEventDefinition>() {
                    @Override
                    public ErrorEventDefinition newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ErrorEventDefinitionImpl(instanceContext);
                    }
                });

        errorRefAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ERROR_REF)
                .qNameAttributeReference(Error.class)
                .build();

        typeBuilder.build();
    }

    public ErrorEventDefinitionImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public Error getError() {
        return errorRefAttribute.getReferenceTargetElement(this);
    }

    @Override
    public void setError(Error error) {
        errorRefAttribute.setReferenceTargetElement(this, error);
    }
}
