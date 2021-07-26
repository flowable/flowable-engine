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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.h2.util.IOUtils;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class DeploymentTest extends FlowableCmmnTestCase {

    /**
     * Simplest test possible: deploy the simple-case.cmmn (from the cmmn-converter module) and see if 
     * - a deployment exists
     * - a resource exists
     * - a case definition was created 
     * - that case definition is in the cache
     * - case definition properties set
     */
    @Test
    public void testCaseDefinitionDeployed() throws Exception {

        DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache = cmmnEngineConfiguration.getCaseDefinitionCache();
        caseDefinitionCache.clear();

        String deploymentId = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDeployed.cmmn").deploy().getId();

        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(cmmnDeployment).isNotNull();
        
        List<String> resourceNames = cmmnRepositoryService.getDeploymentResourceNames(cmmnDeployment.getId());
        assertThat(resourceNames).hasSize(1);
        assertThat(resourceNames.get(0)).isEqualTo("org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDeployed.cmmn");
        
        InputStream inputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), resourceNames.get(0));
        assertThat(inputStream).isNotNull();
        inputStream.close();
        
        assertThat(((DefaultDeploymentCache<CaseDefinitionCacheEntry>) caseDefinitionCache).getAll()).hasSize(1);

        CaseDefinitionCacheEntry cachedCaseDefinition = ((DefaultDeploymentCache<CaseDefinitionCacheEntry>) caseDefinitionCache).getAll().iterator().next();
        assertThat(cachedCaseDefinition)
                .extracting(
                        CaseDefinitionCacheEntry::getCase,
                        CaseDefinitionCacheEntry::getCmmnModel,
                        CaseDefinitionCacheEntry::getCaseDefinition)
                .isNotNull();
        
        CaseDefinition caseDefinition = cachedCaseDefinition.getCaseDefinition();
        assertThat(caseDefinition)
                .extracting(
                        CaseDefinition::getId,
                        CaseDefinition::getDeploymentId,
                        CaseDefinition::getKey,
                        CaseDefinition::getResourceName)
                .isNotNull();
        assertThat(caseDefinition.getVersion()).isPositive();
        
        caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(cmmnDeployment.getId()).singleResult();
        assertThat(caseDefinition)
                .extracting(
                        CaseDefinition::getId,
                        CaseDefinition::getDeploymentId,
                        CaseDefinition::getKey,
                        CaseDefinition::getResourceName)
                .isNotNull();
        assertThat(caseDefinition.getVersion()).isEqualTo(1);
        
        CmmnModel cmmnModel = cmmnRepositoryService.getCmmnModel(caseDefinition.getId());
        assertThat(cmmnModel).isNotNull();
        
        // CmmnParser should have added behavior to plan items
        assertThat(cmmnModel.getPrimaryCase().getPlanModel().getPlanItems())
                .extracting(PlanItem::getBehavior)
                .isNotNull();

        cmmnRepositoryService.deleteDeployment(deploymentId, true);
    }
    
    @Test
    @CmmnDeployment
    public void testCaseDefinitionDI() throws Exception {
        org.flowable.cmmn.api.repository.CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().singleResult();
        assertThat(cmmnDeployment).isNotNull();
        
        List<String> resourceNames = cmmnRepositoryService.getDeploymentResourceNames(cmmnDeployment.getId());
        assertThat(resourceNames).hasSize(2);
        
        String resourceName = "org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDI.cmmn";
        String diagramResourceName = "org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionDI.caseB.png";
        assertThat(resourceNames).contains(resourceName, diagramResourceName);
        
        InputStream inputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), resourceName);
        assertThat(inputStream).isNotNull();
        IOUtils.closeSilently(inputStream);
        
        InputStream diagramInputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(), diagramResourceName);
        assertThat(diagramInputStream).isNotNull();
        IOUtils.closeSilently(diagramInputStream);
        
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(cmmnDeployment.getId()).singleResult();
        
        InputStream caseDiagramInputStream = cmmnRepositoryService.getCaseDiagram(caseDefinition.getId());
        assertThat(caseDiagramInputStream).isNotNull();
        IOUtils.closeSilently(caseDiagramInputStream);
    }

    @Test
    public void testBulkInsertCmmnDeployments() {

        List<String> deploymentIds = cmmnEngineConfiguration.getCommandExecutor()
            .execute(commandContext -> {
                org.flowable.cmmn.api.repository.CmmnDeployment deployment1 = cmmnRepositoryService.createDeployment()
                    .name("First deployment")
                    .key("one-human")
                    .category("test")
                    .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                    .deploy();
                org.flowable.cmmn.api.repository.CmmnDeployment deployment2 = cmmnRepositoryService.createDeployment()
                    .name("Second deployment")
                    .key("example-task")
                    .addClasspathResource("org/flowable/cmmn/test/example-task-model.cmmn")
                    .deploy();

                return Arrays.asList(deployment1.getId(), deployment2.getId());
            });

        assertThat(cmmnRepositoryService.getDeploymentResourceNames(deploymentIds.get(0)))
            .containsExactlyInAnyOrder("org/flowable/cmmn/test/one-human-task-model.cmmn");

        assertThat(cmmnRepositoryService.getDeploymentResourceNames(deploymentIds.get(1)))
            .containsExactlyInAnyOrder("org/flowable/cmmn/test/example-task-model.cmmn");

        assertThat(cmmnRepositoryService.createDeploymentQuery().list())
            .as("Deployment time not null")
            .allSatisfy(deployment -> assertThat(deployment.getDeploymentTime()).as(deployment.getName()).isNotNull())
            .extracting(org.flowable.cmmn.api.repository.CmmnDeployment::getId, org.flowable.cmmn.api.repository.CmmnDeployment::getName,
                org.flowable.cmmn.api.repository.CmmnDeployment::getKey, org.flowable.cmmn.api.repository.CmmnDeployment::getCategory)
            .as("id, name, key, category")
            .containsExactlyInAnyOrder(
                tuple(deploymentIds.get(0), "First deployment", "one-human", "test"),
                tuple(deploymentIds.get(1), "Second deployment", "example-task", null)
            );

        deploymentIds.forEach(deploymentId -> cmmnRepositoryService.deleteDeployment(deploymentId, true));
    }

    @Test
    public void deployingCaseModelWithErrorsShouldFail() {
        assertThatThrownBy(() -> cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionWithErrors.cmmn")
                .deploy())
                .isInstanceOf(FlowableException.class)
                .hasMessage("Errors while parsing:\n"
                        + "[Validation set: 'flowable-case' "
                        + "| Problem: 'flowable-humantask-listener-implementation-missing'] : Element 'class', 'expression' or 'delegateExpression' is mandatory on executionListener - "
                        + "[Extra info : caseDefinitionId = myCase | caseDefinitionName = My Invalid Case Model |  | id = task1 |  | name = Task 1 | ] "
                        + "( line: 12, column: 54)\n");

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list()).isEmpty();
    }

    @Test
    public void deployingCaseModelWithWarningsShouldNotFail() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = null;

        try {
            deployment = cmmnRepositoryService.createDeployment()
                    .addClasspathResource("org/flowable/cmmn/test/repository/DeploymentTest.testCaseDefinitionWithNoPlanItems.cmmn")
                    .deploy();
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().list())
                    .extracting(CaseDefinition::getKey)
                    .containsExactlyInAnyOrder("emptyCase");
        } finally {
            if (deployment != null) {
                cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
            }
        }

    }
}
