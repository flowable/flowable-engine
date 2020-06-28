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
            assertThat(appDeployment).isNotNull();
            
            AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey("extraInfoApp").singleResult();
            
            AppModel appModel = appRepositoryService.getAppModel(appDefinition.getId());
            assertThat(appModel).isInstanceOf(CustomAppModel.class);
            
            CustomAppModel customAppModel = (CustomAppModel) appModel;
            assertThat(customAppModel.getKey()).isEqualTo("extraInfoApp");
            assertThat(customAppModel.getName()).isEqualTo("Extra info app");
            assertThat(customAppModel.getExtraProperty()).isEqualTo("test");
            
        } finally {
            appEngineConfiguration.setAppResourceConverter(defaultAppResourceConverter);
            appRepositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    public void testAppDefinitionDeployedThroughDefaultConverter() {
        String deploymentId = null;
        try {
            deploymentId = appRepositoryService.createDeployment().addClasspathResource("org/flowable/app/engine/test/extraInfoApp.app").deploy().getId();
            AppDeployment appDeployment = appRepositoryService.createDeploymentQuery().singleResult();
            assertThat(appDeployment).isNotNull();

            AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey("extraInfoApp").singleResult();
            assertThat(appDefinition).isNotNull();

            AppModel appModel = appRepositoryService.getAppModel(appDefinition.getId());
            assertThat(appModel).isNotNull();

        } finally {
            if (deploymentId != null) {
                appRepositoryService.deleteDeployment(deploymentId, true);
            }
        }
    }

}
