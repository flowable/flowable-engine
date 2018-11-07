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
package org.flowable.cmmn.test.prefix;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Joram Barrez
 */
public class CmmnPrefixTest {
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testPrefixCase() {
        CmmnEngine cmmnEngine = null;
        String deploymentId = null;
        String flowableCmmnCfgXml = "flowable.prefix.cfg.xml";
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream(flowableCmmnCfgXml)) {
            cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            CmmnEngineConfiguration cmmnEngineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
            CmmnRepositoryService cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
            CmmnRuntimeService cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
            CmmnTaskService cmmnTaskService = cmmnEngine.getCmmnTaskService();
            CmmnHistoryService cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
            
            CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/prefix/CmmnPrefixTest.testPrefixCase.cmmn")
                .deploy();
            deploymentId = deployment.getId();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneHumanTaskCase")
                    .variable("testPrefix", "tested")
                    .start();
            assertTrue(caseInstance.getId().startsWith("CAS-"));
            
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertTrue(task.getId().startsWith("TSK-"));
            cmmnTaskService.complete(task.getId());

            if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
                HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
                assertTrue(historicTaskInstance.getId().startsWith("TSK-"));
                
                if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
                    HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
                    assertTrue(historicVariableInstance.getId().startsWith("VAR-"));
                }
            }
            
        } catch (IOException e) {
            throw new FlowableException("Could not create CMMN engine", e);
            
        } finally {
            if (cmmnEngine != null) {
                if (deploymentId != null) {
                    cmmnEngine.getCmmnRepositoryService().deleteDeployment(deploymentId, true);
                }
                
                cmmnEngine.close();
            }
        }
    }

}
