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
package org.flowable.cmmn.test.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class CaseDefinitionCategoryTest extends FlowableCmmnTestCase {

    private String deploymentId1;

    @Before
    public void deployTestDeployments() {
        this.deploymentId1 = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/repository/simple-case.cmmn")
                .deploy()
                .getId();
    }

    @After
    public void deleteTestDeployments() {
        List<CmmnDeployment> deployments = cmmnRepositoryService.createDeploymentQuery().list();
        for (CmmnDeployment cmmnDeployment : deployments) {
            cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
        }
    }

    @Test
    public void testUpdateCategory() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).singleResult();
        assertThat(caseDefinition.getCategory()).isEqualTo("http://flowable.org/cmmn");
        
        cmmnRepositoryService.setCaseDefinitionCategory(caseDefinition.getId(), "testCategory");
        caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).singleResult();
        assertThat(caseDefinition.getCategory()).isEqualTo("testCategory");
        
        caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).caseDefinitionCategory("testCategory").singleResult();
        assertThat(caseDefinition).isNotNull();
    }
    
    @Test
    public void testDescriptionPersistency() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId1).singleResult();
        assertThat(caseDefinition.getDescription()).isEqualTo("This is a sample description");
    }
}
