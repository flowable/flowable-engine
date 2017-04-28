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
package org.flowable.bpm.model.bpmn.impl.instance.di;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ATTRIBUTE_DOCUMENTATION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ATTRIBUTE_ID;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ATTRIBUTE_NAME;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ATTRIBUTE_RESOLUTION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ELEMENT_DIAGRAM;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_NS;

import org.flowable.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.flowable.bpm.model.bpmn.instance.di.Diagram;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.attribute.Attribute;

/**
 * The DI Diagram element.
 */
public abstract class DiagramImpl
        extends BpmnModelElementInstanceImpl
        implements Diagram {

    protected static Attribute<String> nameAttribute;
    protected static Attribute<String> documentationAttribute;
    protected static Attribute<Double> resolutionAttribute;
    protected static Attribute<String> idAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Diagram.class, DI_ELEMENT_DIAGRAM)
                .namespaceUri(DI_NS)
                .abstractType();

        nameAttribute = typeBuilder.stringAttribute(DI_ATTRIBUTE_NAME)
                .build();

        documentationAttribute = typeBuilder.stringAttribute(DI_ATTRIBUTE_DOCUMENTATION)
                .build();

        resolutionAttribute = typeBuilder.doubleAttribute(DI_ATTRIBUTE_RESOLUTION)
                .build();

        idAttribute = typeBuilder.stringAttribute(DI_ATTRIBUTE_ID)
                .idAttribute()
                .build();

        typeBuilder.build();
    }

    public DiagramImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public String getName() {
        return nameAttribute.getValue(this);
    }

    public void setName(String name) {
        nameAttribute.setValue(this, name);
    }

    public String getDocumentation() {
        return documentationAttribute.getValue(this);
    }

    public void setDocumentation(String documentation) {
        documentationAttribute.setValue(this, documentation);
    }

    public double getResolution() {
        return resolutionAttribute.getValue(this);
    }

    public void setResolution(double resolution) {
        resolutionAttribute.setValue(this, resolution);
    }

    public String getId() {
        return idAttribute.getValue(this);
    }

    public void setId(String id) {
        idAttribute.setValue(this, id);
    }
}
