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
package org.flowable.admin.service.engine;

import java.util.ArrayList;
import java.util.List;

import org.flowable.admin.domain.EndpointType;
import org.flowable.admin.domain.ServerConfig;
import org.flowable.admin.dto.ServerConfigRepresentation;
import org.flowable.admin.repository.ServerConfigRepository;
import org.flowable.admin.service.engine.exception.FlowableServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jbarrez
 * @author Yvo Swillens
 */
@Service
public class ServerConfigService extends AbstractEncryptingService {

    private static final String REST_PROCESS_APP_NAME = "rest.process.app.name";
    private static final String REST_PROCESS_APP_DESCRIPTION = "rest.process.app.description";
    private static final String REST_PROCESS_APP_HOST = "rest.process.app.host";
    private static final String REST_PROCESS_APP_PORT = "rest.process.app.port";
    private static final String REST_PROCESS_APP_CONTEXT_ROOT = "rest.process.app.contextroot";
    private static final String REST_PROCESS_APP_REST_ROOT = "rest.process.app.restroot";
    private static final String REST_PROCESS_APP_USER = "rest.process.app.user";
    private static final String REST_PROCESS_APP_PASSWORD = "rest.process.app.password";

    private static final String REST_DMN_APP_NAME = "rest.dmn.app.name";
    private static final String REST_DMN_APP_DESCRIPTION = "rest.dmn.app.description";
    private static final String REST_DMN_APP_HOST = "rest.dmn.app.host";
    private static final String REST_DMN_APP_PORT = "rest.dmn.app.port";
    private static final String REST_DMN_APP_CONTEXT_ROOT = "rest.dmn.app.contextroot";
    private static final String REST_DMN_APP_REST_ROOT = "rest.dmn.app.restroot";
    private static final String REST_DMN_APP_USER = "rest.dmn.app.user";
    private static final String REST_DMN_APP_PASSWORD = "rest.dmn.app.password";

    private static final String REST_FORM_APP_NAME = "rest.form.app.name";
    private static final String REST_FORM_APP_DESCRIPTION = "rest.form.app.description";
    private static final String REST_FORM_APP_HOST = "rest.form.app.host";
    private static final String REST_FORM_APP_PORT = "rest.form.app.port";
    private static final String REST_FORM_APP_CONTEXT_ROOT = "rest.form.app.contextroot";
    private static final String REST_FORM_APP_REST_ROOT = "rest.form.app.restroot";
    private static final String REST_FORM_APP_USER = "rest.form.app.user";
    private static final String REST_FORM_APP_PASSWORD = "rest.form.app.password";

    private static final String REST_CONTENT_APP_NAME = "rest.content.app.name";
    private static final String REST_CONTENT_APP_DESCRIPTION = "rest.content.app.description";
    private static final String REST_CONTENT_APP_HOST = "rest.content.app.host";
    private static final String REST_CONTENT_APP_PORT = "rest.content.app.port";
    private static final String REST_CONTENT_APP_CONTEXT_ROOT = "rest.content.app.contextroot";
    private static final String REST_CONTENT_APP_REST_ROOT = "rest.content.app.restroot";
    private static final String REST_CONTENT_APP_USER = "rest.content.app.user";
    private static final String REST_CONTENT_APP_PASSWORD = "rest.content.app.password";

    @Autowired
    protected Environment environment;

    @Autowired
    protected ServerConfigRepository serverConfigRepository;

    @Transactional
    public void createDefaultServerConfigs() {
        List<ServerConfig> serverConfigs = getDefaultServerConfigs();
        for (ServerConfig serverConfig : serverConfigs) {
            save(serverConfig, true);
        }
    }

    @Transactional
    public ServerConfig findOne(String id) {
        return serverConfigRepository.get(id);
    }

    @Transactional
    public ServerConfig findOneByEndpointTypeCode(EndpointType endpointType) {
        List<ServerConfig> serverConfigs = serverConfigRepository.getByEndpointType(endpointType);

        if (serverConfigs == null) {
            throw new FlowableServiceException("No server config found");
        }

        if (serverConfigs.size() > 1) {
            throw new FlowableServiceException("Only one server config per endpoint type allowed");
        }

        return serverConfigs.get(0);
    }

    @Transactional
    public List<ServerConfigRepresentation> findAll() {
        return createServerConfigRepresentation(serverConfigRepository.getAll());
    }

    @Transactional
    public void save(ServerConfig serverConfig, boolean encryptPassword) {
        if (encryptPassword) {
            serverConfig.setPassword(encrypt(serverConfig.getPassword()));
        }
        serverConfigRepository.save(serverConfig);
    }

    public String getServerConfigDecryptedPassword(ServerConfig serverConfig) {
        return decrypt(serverConfig.getPassword());
    }

    protected List<ServerConfigRepresentation> createServerConfigRepresentation(List<ServerConfig> serverConfigs) {
        List<ServerConfigRepresentation> serversRepresentations = new ArrayList<>();
        for (ServerConfig serverConfig : serverConfigs) {
            serversRepresentations.add(createServerConfigRepresentation(serverConfig));
        }
        return serversRepresentations;
    }

    protected ServerConfigRepresentation createServerConfigRepresentation(ServerConfig serverConfig) {
        ServerConfigRepresentation serverRepresentation = new ServerConfigRepresentation();
        serverRepresentation.setId(serverConfig.getId());
        serverRepresentation.setName(serverConfig.getName());
        serverRepresentation.setDescription(serverConfig.getDescription());
        serverRepresentation.setServerAddress(serverConfig.getServerAddress());
        serverRepresentation.setServerPort(serverConfig.getPort());
        serverRepresentation.setContextRoot(serverConfig.getContextRoot());
        serverRepresentation.setRestRoot(serverConfig.getRestRoot());
        serverRepresentation.setUserName(serverConfig.getUserName());
        serverRepresentation.setEndpointType(serverConfig.getEndpointType());
        return serverRepresentation;
    }

    public ServerConfig getDefaultServerConfig(EndpointType endpointType) {

        ServerConfig serverConfig = new ServerConfig();

        switch (endpointType) {

            case PROCESS:
                serverConfig.setName(environment.getRequiredProperty(REST_PROCESS_APP_NAME));
                serverConfig.setDescription(environment.getRequiredProperty(REST_PROCESS_APP_DESCRIPTION));
                serverConfig.setServerAddress(environment.getRequiredProperty(REST_PROCESS_APP_HOST));
                serverConfig.setPort(environment.getRequiredProperty(REST_PROCESS_APP_PORT, Integer.class));
                serverConfig.setContextRoot(environment.getRequiredProperty(REST_PROCESS_APP_CONTEXT_ROOT));
                serverConfig.setRestRoot(environment.getRequiredProperty(REST_PROCESS_APP_REST_ROOT));
                serverConfig.setUserName(environment.getRequiredProperty(REST_PROCESS_APP_USER));
                serverConfig.setPassword(environment.getRequiredProperty(REST_PROCESS_APP_PASSWORD));
                serverConfig.setEndpointType(endpointType.getEndpointCode());
                break;

            case DMN:
                serverConfig.setName(environment.getRequiredProperty(REST_DMN_APP_NAME));
                serverConfig.setDescription(environment.getRequiredProperty(REST_DMN_APP_DESCRIPTION));
                serverConfig.setServerAddress(environment.getRequiredProperty(REST_DMN_APP_HOST));
                serverConfig.setPort(environment.getRequiredProperty(REST_DMN_APP_PORT, Integer.class));
                serverConfig.setContextRoot(environment.getRequiredProperty(REST_DMN_APP_CONTEXT_ROOT));
                serverConfig.setRestRoot(environment.getRequiredProperty(REST_DMN_APP_REST_ROOT));
                serverConfig.setUserName(environment.getRequiredProperty(REST_DMN_APP_USER));
                serverConfig.setPassword(environment.getRequiredProperty(REST_DMN_APP_PASSWORD));
                serverConfig.setEndpointType(endpointType.getEndpointCode());
                break;

            case FORM:
                serverConfig.setName(environment.getRequiredProperty(REST_FORM_APP_NAME));
                serverConfig.setDescription(environment.getRequiredProperty(REST_FORM_APP_DESCRIPTION));
                serverConfig.setServerAddress(environment.getRequiredProperty(REST_FORM_APP_HOST));
                serverConfig.setPort(environment.getRequiredProperty(REST_FORM_APP_PORT, Integer.class));
                serverConfig.setContextRoot(environment.getRequiredProperty(REST_FORM_APP_CONTEXT_ROOT));
                serverConfig.setRestRoot(environment.getRequiredProperty(REST_FORM_APP_REST_ROOT));
                serverConfig.setUserName(environment.getRequiredProperty(REST_FORM_APP_USER));
                serverConfig.setPassword(environment.getRequiredProperty(REST_FORM_APP_PASSWORD));
                serverConfig.setEndpointType(endpointType.getEndpointCode());
                break;

            case CONTENT:
                serverConfig.setName(environment.getRequiredProperty(REST_CONTENT_APP_NAME));
                serverConfig.setDescription(environment.getRequiredProperty(REST_CONTENT_APP_DESCRIPTION));
                serverConfig.setServerAddress(environment.getRequiredProperty(REST_CONTENT_APP_HOST));
                serverConfig.setPort(environment.getRequiredProperty(REST_CONTENT_APP_PORT, Integer.class));
                serverConfig.setContextRoot(environment.getRequiredProperty(REST_CONTENT_APP_CONTEXT_ROOT));
                serverConfig.setRestRoot(environment.getRequiredProperty(REST_CONTENT_APP_REST_ROOT));
                serverConfig.setUserName(environment.getRequiredProperty(REST_CONTENT_APP_USER));
                serverConfig.setPassword(environment.getRequiredProperty(REST_CONTENT_APP_PASSWORD));
                serverConfig.setEndpointType(endpointType.getEndpointCode());
                break;
        }

        return serverConfig;
    }

    public List<ServerConfig> getDefaultServerConfigs() {
        List<ServerConfig> serverConfigs = new ArrayList<>();

        serverConfigs.add(getDefaultServerConfig(EndpointType.PROCESS));
        serverConfigs.add(getDefaultServerConfig(EndpointType.DMN));
        serverConfigs.add(getDefaultServerConfig(EndpointType.FORM));
        serverConfigs.add(getDefaultServerConfig(EndpointType.CONTENT));

        return serverConfigs;
    }
}
