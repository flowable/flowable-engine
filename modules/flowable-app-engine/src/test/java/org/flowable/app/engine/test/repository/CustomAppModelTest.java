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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.api.repository.AppModel;
import org.flowable.app.api.repository.AppResourceConverter;
import org.flowable.app.engine.test.FlowableAppTestCase;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class CustomAppModelTest extends FlowableAppTestCase {
    
    @Test
    public void testAppDefinitionDeployed() throws Exception {
        AppResourceConverter defaultAppResourceConverter = appEngineConfiguration.getAppResourceConverter();
        appEngineConfiguration.setAppResourceConverter(new CustomAppResourceConverter(appEngineConfiguration.getObjectMapper()));
        
        String deploymentId = null;
        try {
            deploymentId = appRepositoryService.createDeployment().addClasspathResource("org/flowable/app/engine/test/extraInfoApp.app").deploy().getId();
            AppDeployment appDeployment = appRepositoryService.createDeploymentQuery().singleResult();
            assertNotNull(appDeployment);
            
            AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey("extraInfoApp").singleResult();
            
            AppModel appModel = appRepositoryService.getAppModel(appDefinition.getId());
            assertNotNull(appModel);
            assertTrue(appModel instanceof CustomAppModel);
            
            CustomAppModel customAppModel = (CustomAppModel) appModel;
            assertEquals("extraInfoApp", customAppModel.getKey());
            assertEquals("Extra info app", customAppModel.getName());
            assertEquals("test", customAppModel.getExtraProperty());
            
        } finally {
            appEngineConfiguration.setAppResourceConverter(defaultAppResourceConverter);
            appRepositoryService.deleteDeployment(deploymentId, true);
        }
    }
}
