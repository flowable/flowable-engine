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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_DEFINITION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_MUST_UNDERSTAND;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_EXTENSION;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Documentation;
import org.flowable.bpm.model.bpmn.instance.Extension;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN extension element.
 */
public class ExtensionImpl
        extends BpmnModelElementInstanceImpl
        implements Extension {

    protected static Attribute<String> definitionAttribute;
    protected static Attribute<Boolean> mustUnderstandAttribute;
    protected static ChildElementCollection<Documentation> documentationCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Extension.class, BPMN_ELEMENT_EXTENSION)
                .namespaceUri(BPMN20_NS)
                .instanceProvider(new ModelTypeInstanceProvider<Extension>() {
                    @Override
                    public Extension newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ExtensionImpl(instanceContext);
                    }
                });

        // TODO: qname reference extension definition
        definitionAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_DEFINITION)
                .build();

        mustUnderstandAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_MUST_UNDERSTAND)
                .defaultValue(false)
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        documentationCollection = sequenceBuilder.elementCollection(Documentation.class)
                .build();

        typeBuilder.build();
    }

    public ExtensionImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    @Override
    public String getDefinition() {
        return definitionAttribute.getValue(this);
    }

    @Override
    public void setDefinition(String Definition) {
        definitionAttribute.setValue(this, Definition);
    }

    @Override
    public boolean mustUnderstand() {
        return mustUnderstandAttribute.getValue(this);
    }

    @Override
    public void setMustUnderstand(boolean mustUnderstand) {
        mustUnderstandAttribute.setValue(this, mustUnderstand);
    }

    @Override
    public Collection<Documentation> getDocumentations() {
        return documentationCollection.get(this);
    }
}
