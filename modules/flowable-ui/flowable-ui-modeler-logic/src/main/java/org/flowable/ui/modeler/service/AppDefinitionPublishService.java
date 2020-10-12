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
package org.flowable.ui.modeler.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipInputStream;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.common.tenant.TenantProvider;
import org.flowable.ui.modeler.domain.AppDefinition;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.serviceapi.AppDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Can't merge this with {@link AppDefinitionService}, as it doesn't have visibility of domain models needed to do the publication.
 * 
 * @author jbarrez
 */
@Service
@Transactional
public class AppDefinitionPublishService extends BaseAppDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionPublishService.class);

    protected AppRepositoryService appRepositoryService;

    public AppDefinitionPublishService(ObjectProvider<AppRepositoryService> appRepositoryService) {
        this.appRepositoryService = appRepositoryService.getIfAvailable();
    }

    @Autowired
    protected TenantProvider tenantProvider;

    public void publishAppDefinition(String comment, Model appDefinitionModel, String userId) {
        if (appRepositoryService == null) {
            throw new FlowableIllegalStateException("Cannot publish apps from a standalone Modeler application");
        }

        // Create new version of the app model
        modelService.createNewModelVersion(appDefinitionModel, comment, userId);

        AppDefinition appDefinition = null;
        try {
            appDefinition = resolveAppDefinition(appDefinitionModel);
        } catch (Exception e) {
            LOGGER.error("Error deserializing app {}", appDefinitionModel.getId(), e);
            throw new InternalServerErrorException("Could not deserialize app definition");
        }

        if (appDefinition != null) {
            byte[] deployZipArtifact = createDeployableZipArtifact(appDefinitionModel, appDefinition);

            if (deployZipArtifact != null) {
                deployZipArtifact(deployZipArtifact, appDefinitionModel.getKey(), appDefinitionModel.getName());
            }
        }
    }

    protected void deployZipArtifact(byte[] zipArtifact, String deploymentKey, String deploymentName) {
        AppDeploymentBuilder deploymentBuilder = appRepositoryService.createDeployment()
                .key(deploymentKey)
                .name(deploymentName);


        String tenantId = tenantProvider.getTenantId();

        if (tenantId != null) {
            deploymentBuilder.tenantId(tenantId);
        }

        try (InputStream bytesStream = new ByteArrayInputStream(zipArtifact);
             ZipInputStream zipStream = new ZipInputStream(bytesStream)) {

            deploymentBuilder.addZipInputStream(zipStream);

        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read bytes", ex);
        }

        deploymentBuilder.deploy();
    }
}
