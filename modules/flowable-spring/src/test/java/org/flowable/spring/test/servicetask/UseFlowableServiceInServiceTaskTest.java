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
package org.flowable.spring.test.servicetask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/flowable/spring/test/servicetask/servicetaskSpringTest-context.xml")
public class UseFlowableServiceInServiceTaskTest extends SpringFlowableTestCase {

    /**
     * This test will use the regular mechanism (delegateExecution.getProcessEngine().getRuntimeService()) to obtain the {@link RuntimeService} to start a new process.
     */
    @Test
    @Deployment
    public void testUseRuntimeServiceNotInjectedInServiceTask() {
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

    /**
     * This test will use the dependency injection of Spring to inject the runtime service in the Java delegate.
     */
    @Test
    @Deployment
    public void testUseInjectedRuntimeServiceInServiceTask() {
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

}
