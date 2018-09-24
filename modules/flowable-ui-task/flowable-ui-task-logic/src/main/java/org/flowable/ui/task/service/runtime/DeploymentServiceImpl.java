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
package org.flowable.ui.task.service.runtime;

import java.util.List;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.ui.task.service.api.DeploymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
@Service
@Transactional
public class DeploymentServiceImpl implements DeploymentService {

    @Autowired
    protected AppRepositoryService appRepositoryService;

    @Override
    public void deleteAppDefinition(String appDefinitionKey) {
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().appDefinitionKey(appDefinitionKey).list();
        if (appDefinitions != null) {
            for (AppDefinition appDefinition : appDefinitions) {
                appRepositoryService.deleteDeployment(appDefinition.getDeploymentId(), true);
            }
        }
    }
}
