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
package org.flowable.engine.test.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class CallServiceInServiceTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testStartProcessFromDelegate() {
        runtimeService.startProcessInstanceByKey("startProcessFromDelegate");

        // Starting the process should lead to two processes being started,
        // The other one started from the java delegate in the service task
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        assertThat(processInstances).hasSize(2);

        boolean startProcessFromDelegateFound = false;
        boolean oneTaskProcessFound = false;
        for (ProcessInstance processInstance : processInstances) {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
            if ("startProcessFromDelegate".equals(processDefinition.getKey())) {
                startProcessFromDelegateFound = true;
            } else if ("oneTaskProcess".equals(processDefinition.getKey())) {
                oneTaskProcessFound = true;
            }
        }

        assertThat(startProcessFromDelegateFound).isTrue();
        assertThat(oneTaskProcessFound).isTrue();
    }

    @Test
    @Deployment
    public void testRollBackOnException() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("startProcessFromDelegate"))
                .isInstanceOf(Exception.class);

        // Starting the process should cause a rollback of both processes
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testMultipleServiceInvocationsFromDelegate() {
        runtimeService.startProcessInstanceByKey("multipleServiceInvocations");

        // The service task should have created a user which is part of the admin group
        User user = identityService.createUserQuery().singleResult();
        assertThat(user.getId()).isEqualTo("Kermit");
        Group group = identityService.createGroupQuery().groupMember(user.getId()).singleResult();
        assertThat(group).isNotNull();
        assertThat(group.getId()).isEqualTo("admin");

        // Cleanup
        identityService.deleteUser("Kermit");
        identityService.deleteGroup("admin");
        identityService.deleteMembership("Kermit", "admin");
    }

}
