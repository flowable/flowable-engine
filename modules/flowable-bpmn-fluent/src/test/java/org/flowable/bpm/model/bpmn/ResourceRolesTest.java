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
package org.flowable.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.instance.HumanPerformer;
import org.flowable.bpm.model.bpmn.instance.Performer;
import org.flowable.bpm.model.bpmn.instance.PotentialOwner;
import org.flowable.bpm.model.bpmn.instance.ResourceRole;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

public class ResourceRolesTest {

    private static BpmnModelInstance modelInstance;

    @BeforeClass
    public static void parseModel() {
        modelInstance = BpmnModelBuilder.readModelFromStream(ResourceRolesTest.class.getResourceAsStream("ResourceRolesTest.bpmn20.xml"));
    }

    @Test
    public void getPerformer() {
        UserTask userTask = modelInstance.getModelElementById("_3");
        Collection<ResourceRole> resourceRoles = userTask.getResourceRoles();
        assertThat(resourceRoles.size()).isEqualTo(1);
        ResourceRole resourceRole = resourceRoles.iterator().next();
        assertThat(resourceRole instanceof Performer).isTrue();
        assertThat(resourceRole.getName()).isEqualTo("Task performer");
    }

    @Test
    public void getHumanPerformer() {
        UserTask userTask = modelInstance.getModelElementById("_7");
        Collection<ResourceRole> resourceRoles = userTask.getResourceRoles();
        assertThat(resourceRoles.size()).isEqualTo(1);
        ResourceRole resourceRole = resourceRoles.iterator().next();
        assertThat(resourceRole instanceof HumanPerformer).isTrue();
        assertThat(resourceRole.getName()).isEqualTo("Task human performer");
    }

    @Test
    public void getPotentialOwner() {
        UserTask userTask = modelInstance.getModelElementById("_9");
        Collection<ResourceRole> resourceRoles = userTask.getResourceRoles();
        assertThat(resourceRoles.size()).isEqualTo(1);
        ResourceRole resourceRole = resourceRoles.iterator().next();
        assertThat(resourceRole instanceof PotentialOwner).isTrue();
        assertThat(resourceRole.getName()).isEqualTo("Task potential owner");
    }

}
