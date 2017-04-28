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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_EXTENSION_ELEMENTS;

import org.flowable.bpm.model.bpmn.Query;
import org.flowable.bpm.model.bpmn.impl.QueryImpl;
import org.flowable.bpm.model.bpmn.instance.ExtensionElements;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.impl.util.ModelUtil;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

import java.util.Collection;

/**
 * The BPMN extensionElements element.
 */
public class ExtensionElementsImpl
        extends BpmnModelElementInstanceImpl
        implements ExtensionElements {

    public static void registerType(ModelBuilder modelBuilder) {

        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(ExtensionElements.class, BPMN_ELEMENT_EXTENSION_ELEMENTS)
                .namespaceUri(BPMN20_NS)
                .instanceProvider(new ModelElementTypeBuilder.ModelTypeInstanceProvider<ExtensionElements>() {
                    public ExtensionElements newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ExtensionElementsImpl(instanceContext);
                    }
                });

        typeBuilder.build();
    }

    public ExtensionElementsImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public Collection<ModelElementInstance> getElements() {
        return ModelUtil.getModelElementCollection(getDomElement().getChildElements(), modelInstance);
    }

    public Query<ModelElementInstance> getElementsQuery() {
        return new QueryImpl<>(getElements());
    }

    public ModelElementInstance addExtensionElement(String namespaceUri, String localName) {
        ModelElementType extensionElementType = modelInstance.registerGenericType(namespaceUri, localName);
        ModelElementInstance extensionElement = extensionElementType.newInstance(modelInstance);
        addChildElement(extensionElement);
        return extensionElement;
    }

    public <T extends ModelElementInstance> T addExtensionElement(Class<T> extensionElementClass) {
        ModelElementInstance extensionElement = modelInstance.newInstance(extensionElementClass);
        addChildElement(extensionElement);
        return extensionElementClass.cast(extensionElement);
    }

    @Override
    public void addChildElement(ModelElementInstance extensionElement) {
        getDomElement().appendChild(extensionElement.getDomElement());
    }

}
