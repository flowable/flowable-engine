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

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
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
    protected RepositoryService repositoryService;

    @Override
    public void deleteAppDefinition(Long appDefinitionId) {
        // First test if deployment is still there, otherwise the transaction will be rolled back
        List<Deployment> deployments = repositoryService.createDeploymentQuery().deploymentKey(String.valueOf(appDefinitionId)).list();
        if (deployments != null) {
            for (Deployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
