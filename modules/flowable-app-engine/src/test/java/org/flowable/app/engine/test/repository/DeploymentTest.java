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
package org.flowable.app.engine.test.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppModel;
import org.flowable.app.engine.impl.persistence.entity.deploy.AppDefinitionCacheEntry;
import org.flowable.app.engine.test.AppDeployment;
import org.flowable.app.engine.test.FlowableAppTestCase;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class DeploymentTest extends FlowableAppTestCase {
    
    /**
     * Simplest test possible: deploy the simple-case.cmmn (from the cmmn-converter module) and see if 
     * - a deployment exists
     * - a resouce exists
     * - a case definition was created 
     * - that case definition is in the cache
     * - case definition properties set
     */
    @Test
    @AppDeployment
    public void testAppDefinitionDeployed() throws Exception {
        org.flowable.app.api.repository.AppDeployment appDeployment = appRepositoryService.createDeploymentQuery().singleResult();
        assertNotNull(appDeployment);
        
        List<String> resourceNames = appRepositoryService.getDeploymentResourceNames(appDeployment.getId());
        assertEquals(1, resourceNames.size());
        assertEquals("org/flowable/app/engine/test/repository/DeploymentTest.testAppDefinitionDeployed.app", resourceNames.get(0));
        
        InputStream inputStream = appRepositoryService.getResourceAsStream(appDeployment.getId(), resourceNames.get(0));
        assertNotNull(inputStream);
        inputStream.close();
        
        DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache = appEngineConfiguration.getAppDefinitionCache();
        assertEquals(1, ((DefaultDeploymentCache<AppDefinitionCacheEntry>) appDefinitionCache).getAll().size());
        
        AppDefinitionCacheEntry cachedAppDefinition = ((DefaultDeploymentCache<AppDefinitionCacheEntry>) appDefinitionCache).getAll().iterator().next();
        assertNotNull(cachedAppDefinition.getAppModel());
        assertNotNull(cachedAppDefinition.getAppDefinition());
        
        AppDefinition appDefinition = cachedAppDefinition.getAppDefinition();
        assertNotNull(appDefinition.getId());
        assertNotNull(appDefinition.getDeploymentId());
        assertNotNull(appDefinition.getKey());
        assertNotNull(appDefinition.getResourceName());
        assertTrue(appDefinition.getVersion() > 0);
        
        appDefinition = appRepositoryService.createAppDefinitionQuery().deploymentId(appDeployment.getId()).singleResult();
        assertNotNull(appDefinition.getId());
        assertNotNull(appDefinition.getDeploymentId());
        assertNotNull(appDefinition.getKey());
        assertNotNull(appDefinition.getResourceName());
        assertEquals(1, appDefinition.getVersion());
        
        AppModel appModel = appRepositoryService.getAppModel(appDefinition.getId());
        assertNotNull(appModel);
        
        assertEquals("testApp", appModel.getKey());
        assertEquals("Test app", appModel.getName());
    }
    
    @Test
    public void testAppDefinitionZipDeployed() throws Exception {
        appRepositoryService.createDeployment().addZipInputStream(new ZipInputStream(this.getClass().getClassLoader().getResourceAsStream(
                        "org/flowable/app/engine/test/vacationRequest.zip"))).deploy();
        org.flowable.app.api.repository.AppDeployment appDeployment = appRepositoryService.createDeploymentQuery().singleResult();
        assertNotNull(appDeployment);
        
        List<String> resourceNames = appRepositoryService.getDeploymentResourceNames(appDeployment.getId());
        assertEquals(4, resourceNames.size());
        
        boolean vacationRequestAppFound = false;
        for (String resourceName : resourceNames) {
            if ("vacationRequestApp.app".equals(resourceName)) {
                vacationRequestAppFound = true;
            }
        }
        assertTrue(vacationRequestAppFound);
        
        InputStream inputStream = appRepositoryService.getResourceAsStream(appDeployment.getId(), "vacationRequestApp.app");
        assertNotNull(inputStream);
        inputStream.close();
        
        DeploymentCache<AppDefinitionCacheEntry> appDefinitionCache = appEngineConfiguration.getAppDefinitionCache();
        assertEquals(1, ((DefaultDeploymentCache<AppDefinitionCacheEntry>) appDefinitionCache).getAll().size());
        
        AppDefinitionCacheEntry cachedAppDefinition = ((DefaultDeploymentCache<AppDefinitionCacheEntry>) appDefinitionCache).getAll().iterator().next();
        assertNotNull(cachedAppDefinition.getAppModel());
        assertNotNull(cachedAppDefinition.getAppDefinition());
        
        AppDefinition appDefinition = cachedAppDefinition.getAppDefinition();
        assertNotNull(appDefinition.getId());
        assertNotNull(appDefinition.getDeploymentId());
        assertNotNull(appDefinition.getKey());
        assertNotNull(appDefinition.getResourceName());
        assertTrue(appDefinition.getVersion() > 0);
        
        appDefinition = appRepositoryService.createAppDefinitionQuery().deploymentId(appDeployment.getId()).singleResult();
        assertNotNull(appDefinition.getId());
        assertNotNull(appDefinition.getDeploymentId());
        assertNotNull(appDefinition.getKey());
        assertNotNull(appDefinition.getResourceName());
        assertEquals(1, appDefinition.getVersion());
        
        AppModel appModel = appRepositoryService.getAppModel(appDefinition.getId());
        assertNotNull(appModel);
        
        assertEquals("vacationRequestApp", appModel.getKey());
        assertEquals("Vacation request app", appModel.getName());
        
        appRepositoryService.deleteDeployment(appDeployment.getId(), true);
    }

    @Test
    public void testBulkInsertAppDeployments() {
        List<String> deploymentIds = appEngineConfiguration.getCommandExecutor()
            .execute(commandContext -> {
                org.flowable.app.api.repository.AppDeployment deployment1 = appRepositoryService.createDeployment()
                    .name("First")
                    .key("full-info")
                    .category("test")
                    .addClasspathResource("org/flowable/app/engine/test/fullinfo.app")
                    .deploy();

                org.flowable.app.api.repository.AppDeployment deployment2 = appRepositoryService.createDeployment()
                    .name("Second")
                    .key("test")
                    .addClasspathResource("org/flowable/app/engine/test/test.app")
                    .deploy();
                return Arrays.asList(deployment1.getId(), deployment2.getId());
            });

        assertThat(appRepositoryService.getDeploymentResourceNames(deploymentIds.get(0)))
            .containsExactlyInAnyOrder("org/flowable/app/engine/test/fullinfo.app");
        assertThat(appRepositoryService.getDeploymentResourceNames(deploymentIds.get(1)))
            .containsExactlyInAnyOrder("org/flowable/app/engine/test/test.app");

        assertThat(appRepositoryService.createDeploymentQuery().list())
            .as("deployment time not null")
            .allSatisfy(deployment -> assertThat(deployment.getDeploymentTime()).as(deployment.getName()).isNotNull())
            .extracting(org.flowable.app.api.repository.AppDeployment::getId, org.flowable.app.api.repository.AppDeployment::getName,
                org.flowable.app.api.repository.AppDeployment::getKey, org.flowable.app.api.repository.AppDeployment::getCategory)
            .as("id, name, key, category")
            .containsExactlyInAnyOrder(
                tuple(deploymentIds.get(0), "First", "full-info", "test"),
                tuple(deploymentIds.get(1), "Second", "test", null)
            );

        deploymentIds.forEach(deploymentId -> appRepositoryService.deleteDeployment(deploymentId, true));
    }
}
