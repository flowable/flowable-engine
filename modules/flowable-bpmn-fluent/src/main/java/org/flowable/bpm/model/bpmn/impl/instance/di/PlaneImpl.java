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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ELEMENT_PLANE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_NS;

import org.flowable.bpm.model.bpmn.instance.di.DiagramElement;
import org.flowable.bpm.model.bpmn.instance.di.Node;
import org.flowable.bpm.model.bpmn.instance.di.Plane;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

/**
 * The DI Plane element.
 */
public abstract class PlaneImpl
        extends NodeImpl
        implements Plane {

    protected static ChildElementCollection<DiagramElement> diagramElementCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Plane.class, DI_ELEMENT_PLANE)
                .namespaceUri(DI_NS)
                .extendsType(Node.class)
                .abstractType();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        diagramElementCollection = sequenceBuilder.elementCollection(DiagramElement.class)
                .build();

        typeBuilder.build();
    }

    public PlaneImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Collection<DiagramElement> getDiagramElements() {
        return diagramElementCollection.get(this);
    }
}
