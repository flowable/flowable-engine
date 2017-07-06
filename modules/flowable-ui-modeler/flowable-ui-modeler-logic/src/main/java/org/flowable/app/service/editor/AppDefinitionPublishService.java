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
package org.flowable.app.service.editor;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.service.api.AppDefinitionService;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.flowable.app.rest.HttpRequestHelper.executePostRequest;

/**
 * Can't merge this with {@link AppDefinitionService}, as it doesn't have visibility of domain models needed to do the publication.
 *
 * @author jbarrez
 */
@Service
@Transactional
public class AppDefinitionPublishService extends BaseAppDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionPublishService.class);

    @Autowired
    protected Environment environment;

    public void publishAppDefinition(String comment, Model appDefinitionModel, User user) {

        // Create new version of the app model
        modelService.createNewModelVersion(appDefinitionModel, comment, user);

        String deployableZipName = appDefinitionModel.getKey() + ".zip";

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
                deployZipArtifact(deployableZipName, deployZipArtifact, appDefinitionModel.getKey(), appDefinitionModel.getName());
            }
        }
    }

    protected void deployZipArtifact(String artifactName, byte[] zipArtifact, String deploymentKey, String deploymentName) {
        String deployApiUrl = environment.getRequiredProperty("deployment.api.url");
        String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
        String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");

        if (!deployApiUrl.endsWith("/")) {
            deployApiUrl = deployApiUrl.concat("/");
        }
        deployApiUrl = deployApiUrl.concat(String.format("repository/deployments?deploymentKey=%s&deploymentName=%s",
                encode(deploymentKey), encode(deploymentName)));

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entityBuilder.addBinaryBody("artifact", zipArtifact, ContentType.DEFAULT_BINARY, artifactName);

        HttpEntity entity = entityBuilder.build();
        executePostRequest(deployApiUrl, basicAuthUser, basicAuthPassword, entity, HttpStatus.SC_CREATED);
    }

    protected String encode(String string) {
        if (string != null) {
            try {
                return URLEncoder.encode(string, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException("JVM does not support UTF-8 encoding.", uee);
            }
        }
        return null;
    }
}
