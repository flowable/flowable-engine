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

    private static final String APP_NAME = "app.name";
    private static final String APP_DESCRIPTION = "app.description";
    private static final String APP_HOST = "app.host";
    private static final String APP_PORT = "app.port";
    private static final String APP_CONTEXT_ROOT = "app.contextroot";
    private static final String APP_REST_ROOT = "app.restroot";
    private static final String APP_USER = "app.user";
    private static final String APP_PASSWORD = "app.password";

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
    public void createCmmnDefaultServerConfig() {
        ServerConfig serverConfig = getDefaultServerConfig(EndpointType.CMMN);
        save(serverConfig, true);
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
        String endpointTypeString = null;

        switch (endpointType) {

            case PROCESS:
                endpointTypeString = "process";
                break;
                
            case CMMN:
                endpointTypeString = "cmmn";
                break;

            case DMN:
                endpointTypeString = "dmn";
                break;

            case FORM:
                endpointTypeString = "form";
                break;

            case CONTENT:
                endpointTypeString = "content";
                break;
        }
        
        String propertyPrefix = "rest." + endpointTypeString + ".";
        serverConfig.setName(environment.getRequiredProperty(propertyPrefix + APP_NAME));
        serverConfig.setDescription(environment.getRequiredProperty(propertyPrefix + APP_DESCRIPTION));
        serverConfig.setServerAddress(environment.getRequiredProperty(propertyPrefix + APP_HOST));
        serverConfig.setPort(environment.getRequiredProperty(propertyPrefix + APP_PORT, Integer.class));
        serverConfig.setContextRoot(environment.getRequiredProperty(propertyPrefix + APP_CONTEXT_ROOT));
        serverConfig.setRestRoot(environment.getRequiredProperty(propertyPrefix + APP_REST_ROOT));
        serverConfig.setUserName(environment.getRequiredProperty(propertyPrefix + APP_USER));
        serverConfig.setPassword(environment.getRequiredProperty(propertyPrefix + APP_PASSWORD));
        serverConfig.setEndpointType(endpointType.getEndpointCode());

        return serverConfig;
    }

    public List<ServerConfig> getDefaultServerConfigs() {
        List<ServerConfig> serverConfigs = new ArrayList<>();

        serverConfigs.add(getDefaultServerConfig(EndpointType.PROCESS));
        serverConfigs.add(getDefaultServerConfig(EndpointType.CMMN));
        serverConfigs.add(getDefaultServerConfig(EndpointType.DMN));
        serverConfigs.add(getDefaultServerConfig(EndpointType.FORM));
        serverConfigs.add(getDefaultServerConfig(EndpointType.CONTENT));

        return serverConfigs;
    }
}
