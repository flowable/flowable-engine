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
package org.flowable.bpm.model.bpmn.instance;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.bpmn.Query;
import org.flowable.bpm.model.bpmn.impl.QueryImpl;
import org.flowable.bpm.model.xml.impl.util.ReflectUtil;
import org.junit.Before;

import java.io.InputStream;
import java.util.Collection;

public abstract class AbstractEventDefinitionTest
        extends BpmnModelElementInstanceTest {

    protected Query<EventDefinition> eventDefinitionQuery;

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(EventDefinition.class, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return null;
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return null;
    }

    @Before
    public void getEvent() {
        InputStream inputStream = ReflectUtil.getResourceAsStream("org/flowable/bpm/model/bpmn/EventDefinitionsTest.bpmn20.xml");
        IntermediateThrowEvent event = BpmnModelBuilder.readModelFromStream(inputStream).getModelElementById("event");
        eventDefinitionQuery = new QueryImpl<>(event.getEventDefinitions());
    }

}
