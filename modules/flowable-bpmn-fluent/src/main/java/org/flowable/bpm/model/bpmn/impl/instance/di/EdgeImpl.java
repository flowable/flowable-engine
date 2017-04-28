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

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_ELEMENT_EDGE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.DI_NS;

import org.flowable.bpm.model.bpmn.instance.di.DiagramElement;
import org.flowable.bpm.model.bpmn.instance.di.Edge;
import org.flowable.bpm.model.bpmn.instance.di.Waypoint;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

public abstract class EdgeImpl
        extends DiagramElementImpl
        implements Edge {

    protected static ChildElementCollection<Waypoint> waypointCollection;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Edge.class, DI_ELEMENT_EDGE)
                .namespaceUri(DI_NS)
                .extendsType(DiagramElement.class)
                .abstractType();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        waypointCollection = sequenceBuilder.elementCollection(Waypoint.class)
                .minOccurs(2)
                .build();

        typeBuilder.build();
    }

    public EdgeImpl(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Collection<Waypoint> getWaypoints() {
        return waypointCollection.get(this);
    }
}
