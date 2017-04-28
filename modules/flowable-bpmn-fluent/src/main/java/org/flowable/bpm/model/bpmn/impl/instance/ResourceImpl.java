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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_RESOURCE;
import static org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import org.flowable.bpm.model.bpmn.instance.Resource;
import org.flowable.bpm.model.bpmn.instance.ResourceParameter;
import org.flowable.bpm.model.bpmn.instance.RootElement;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The BPMN resource element.
 */
public class ResourceImpl
        extends RootElementImpl
        implements Resource {

    protected static Attribute<String> nameAttribute;
    protected static ChildElementCollection<ResourceParameter> resourceParameterCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Resource.class, BPMN_ELEMENT_RESOURCE)
                .namespaceUri(BPMN20_NS)
                .extendsType(RootElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Resource>() {
                    public Resource newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ResourceImpl(instanceContext);
                    }
                });

        nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME)
                .required()
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        resourceParameterCollection = sequenceBuilder.elementCollection(ResourceParameter.class)
                .build();

        typeBuilder.build();
    }

    public ResourceImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public Collection<ResourceParameter> getResourceParameters() {
        return resourceParameterCollection.get(this);
    }
}
