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

package org.flowable.cmmn.test.migration;

import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.FlowableCmmnTest;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Valentin Zickner
 */
@FlowableCmmnTest
public class AbstractCaseMigrationTest {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnMigrationService cmmnMigrationService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;

    @BeforeEach
    protected void setUp(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();
        this.cmmnMigrationService = cmmnEngineConfiguration.getCmmnMigrationService();
        this.cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        this.cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();
    }

    @AfterEach
    void tearDown() {
        List<CmmnDeployment> deployments = this.cmmnRepositoryService.createDeploymentQuery().list();
        for (CmmnDeployment deployment : deployments) {
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deployment.getId());
        }
    }

    protected CaseDefinition deployCaseDefinition(String name, String path) {
        CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                .name(name)
                .addClasspathResource(path)
                .deploy();

        return cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
    }

}
