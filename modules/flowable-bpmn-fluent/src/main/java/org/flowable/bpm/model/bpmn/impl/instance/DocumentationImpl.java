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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TEXT_FORMAT;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_DOCUMENTATION;

import org.flowable.bpm.model.bpmn.instance.Documentation;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN documentation element.
 */
public class DocumentationImpl
        extends BpmnModelElementInstanceImpl
        implements Documentation {

    protected static Attribute<String> idAttribute;
    protected static Attribute<String> textFormatAttribute;

    public static void registerType(ModelBuilder modelBuilder) {

        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Documentation.class, BPMN_ELEMENT_DOCUMENTATION)
                .namespaceUri(BPMN20_NS)
                .instanceProvider(new ModelTypeInstanceProvider<Documentation>() {
                    @Override
                    public Documentation newInstance(ModelTypeInstanceContext instanceContext) {
                        return new DocumentationImpl(instanceContext);
                    }
                });

        idAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_ID)
                .idAttribute()
                .build();

        textFormatAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_TEXT_FORMAT)
                .defaultValue("text/plain")
                .build();

        typeBuilder.build();
    }

    public DocumentationImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public String getId() {
        return idAttribute.getValue(this);
    }

    @Override
    public void setId(String id) {
        idAttribute.setValue(this, id);
    }

    @Override
    public String getTextFormat() {
        return textFormatAttribute.getValue(this);
    }

    @Override
    public void setTextFormat(String textFormat) {
        textFormatAttribute.setValue(this, textFormat);
    }

}
