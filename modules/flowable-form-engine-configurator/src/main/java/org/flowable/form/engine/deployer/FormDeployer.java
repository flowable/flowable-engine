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
package org.flowable.form.engine.deployer;

import java.util.Map;

import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.form.api.FormDeploymentBuilder;
import org.flowable.form.api.FormRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class FormDeployer implements EngineDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormDeployer.class);

    @Override
    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        if (!deployment.isNew())
            return;

        LOGGER.debug("FormDeployer: processing deployment {}", deployment.getName());

        FormDeploymentBuilder formDeploymentBuilder = null;

        Map<String, EngineResource> resources = deployment.getResources();
        for (String resourceName : resources.keySet()) {
            if (resourceName.endsWith(".form")) {
                LOGGER.info("FormDeployer: processing resource {}", resourceName);
                if (formDeploymentBuilder == null) {
                    FormRepositoryService formRepositoryService = CommandContextUtil.getFormRepositoryService();
                    formDeploymentBuilder = formRepositoryService.createDeployment().name(deployment.getName());
                }

                formDeploymentBuilder.addFormBytes(resourceName, resources.get(resourceName).getBytes());
            }
        }

        if (formDeploymentBuilder != null) {
            formDeploymentBuilder.parentDeploymentId(deployment.getId());
            if (deployment.getTenantId() != null && deployment.getTenantId().length() > 0) {
                formDeploymentBuilder.tenantId(deployment.getTenantId());
            }

            formDeploymentBuilder.deploy();
        }
    }
}
