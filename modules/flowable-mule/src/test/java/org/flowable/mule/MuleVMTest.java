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
package org.flowable.mule;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.Test;

/**
 * @author Esteban Robles Luna
 * @author Tijs Rademakers
 */
public class MuleVMTest extends AbstractMuleTest {

    @Test
    public void send() throws Exception {
        assertThat(muleContext.isStarted()).isTrue();

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/mule/testVM.bpmn20.xml").deploy();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("muleProcess");
        assertThat(processInstance.isEnded()).isFalse();
        Object result = runtimeService.getVariable(processInstance.getProcessInstanceId(), "theVariable");
        assertThat(result).isEqualTo(30);
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");

        processEngine.getHistoryService().deleteHistoricProcessInstance(processInstance.getId());
        repositoryService.deleteDeployment(deployment.getId());
        assertAndEnsureCleanDb(processEngine);
        ProcessEngines.destroy();
    }

    @Override
    protected String getConfigFile() {
        return "mule-config.xml";
    }
}
