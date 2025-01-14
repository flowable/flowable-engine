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
package org.flowable.cmmn.test.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CaseDefinitionCacheTest {
    
    @Test
    public void testCaseDefinitionCache() {
        CmmnEngine cmmnEngine = null;
        String flowableCmmnCfgXml = "flowable.cache.cfg.xml";
        try (InputStream inputStream = FlowableCmmnTestCase.class.getClassLoader().getResourceAsStream(flowableCmmnCfgXml)) {
            cmmnEngine = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream).buildCmmnEngine();
            CmmnEngineConfiguration cmmnEngineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
            CmmnManagementService cmmnManagementService = cmmnEngine.getCmmnManagementService();
            CmmnRepositoryService cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
            CmmnRuntimeService cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
            CmmnHistoryService cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
            
            List<String> deploymentIds = new ArrayList<>();
            int nrOfModels = 9;
            for (int nrOfIterations = 0; nrOfIterations < 10; nrOfIterations++) {
                CmmnDeploymentBuilder deploymentBuilder = cmmnRepositoryService.createDeployment();
                for (int i = 1; i <= nrOfModels; i++) {
                    deploymentBuilder.addClasspathResource("org/flowable/cmmn/test/cache/case" + i + ".cmmn");
                }
                CmmnDeployment deployment = deploymentBuilder.deploy();
                deploymentIds.add(0, deployment.getId());

                assertThat(cmmnRepositoryService.createCaseDefinitionQuery().count()).isEqualTo((nrOfIterations + 1) * nrOfModels);

                for (int i = 1; i <= nrOfModels; i++) {
                    cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("case" + i).start();
                }

                assertThat(cmmnManagementService.createJobQuery().count()).isEqualTo((nrOfIterations + 1) * nrOfModels);
            }
            
            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngine, 30000, 200, true);
            
            assertThat(cmmnManagementService.createJobQuery().count()).isEqualTo(0);
            
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                for (int i = 1; i <= nrOfModels; i++) {
                    assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("case" + i).count()).isEqualTo(10);
                }
            }
            
            for (String deploymentId : deploymentIds) {
                cmmnRepositoryService.deleteDeployment(deploymentId, true);
            }
            
        } catch (IOException e) {
            throw new FlowableException("Could not create CMMN engine", e);
            
        } finally {
            if (cmmnEngine != null) {
                cmmnEngine.close();
            }
        }
    }

}
