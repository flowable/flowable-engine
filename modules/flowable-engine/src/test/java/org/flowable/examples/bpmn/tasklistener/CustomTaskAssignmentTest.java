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
package org.flowable.examples.bpmn.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Falko Menge <falko.menge@camunda.com>
 * @author Frederik Heremans
 */
public class CustomTaskAssignmentTest extends PluggableFlowableTestCase {

    @BeforeEach
    protected void setUp() throws Exception {

        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("fozzie"));
        identityService.saveUser(identityService.newUser("gonzo"));

        identityService.saveGroup(identityService.newGroup("management"));

        identityService.createMembership("kermit", "management");
    }

    @AfterEach
    protected void tearDown() throws Exception {
        identityService.deleteUser("kermit");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteGroup("management");
    }

    @Test
    @Deployment
    public void testCandidateGroupAssignment() {
        runtimeService.startProcessInstanceByKey("customTaskAssignment");
        assertThat(taskService.createTaskQuery().taskCandidateGroup("management").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isZero();
    }

    @Test
    @Deployment
    public void testCandidateUserAssignment() {
        runtimeService.startProcessInstanceByKey("customTaskAssignment");
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("gonzo").count()).isZero();
    }

    @Test
    @Deployment
    public void testAssigneeAssignment() {
        runtimeService.startProcessInstanceByKey("setAssigneeInListener");
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().taskAssignee("fozzie").count()).isZero();
        assertThat(taskService.createTaskQuery().taskAssignee("gonzo").count()).isZero();
    }

    @Test
    @Deployment
    public void testOverwriteExistingAssignments() {
        runtimeService.startProcessInstanceByKey("overrideAssigneeInListener");
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().taskAssignee("fozzie").count()).isZero();
        assertThat(taskService.createTaskQuery().taskAssignee("gonzo").count()).isZero();
    }

    @Test
    @Deployment
    public void testOverwriteExistingAssignmentsFromVariable() {
        // prepare variables
        Map<String, String> assigneeMappingTable = new HashMap<>();
        assigneeMappingTable.put("fozzie", "gonzo");

        Map<String, Object> variables = new HashMap<>();
        variables.put("assigneeMappingTable", assigneeMappingTable);

        // start process instance
        runtimeService.startProcessInstanceByKey("customTaskAssignment", variables);

        // check task lists
        assertThat(taskService.createTaskQuery().taskAssignee("gonzo").singleResult()).isNotNull();
        assertThat(taskService.createTaskQuery().taskAssignee("fozzie").count()).isZero();
        assertThat(taskService.createTaskQuery().taskAssignee("kermit").count()).isZero();
    }

    @Test
    @Deployment
    public void testReleaseTask() throws Exception {
        runtimeService.startProcessInstanceByKey("releaseTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssignee("fozzie").singleResult();
        assertThat(task).isNotNull();
        String taskId = task.getId();

        // Set assignee to null
        taskService.setAssignee(taskId, null);

        task = taskService.createTaskQuery().taskAssignee("fozzie").singleResult();
        assertThat(task).isNull();

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getAssignee()).isNull();
    }

}
