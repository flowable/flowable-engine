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
package org.flowable.ui.admin.service.engine;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.flowable.ui.admin.dto.ServerConfigRepresentation;
import org.flowable.ui.admin.properties.FlowableAdminAppProperties;
import org.flowable.ui.admin.repository.ServerConfigRepository;
import org.flowable.ui.admin.service.engine.exception.FlowableServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author jbarrez
 * @author Yvo Swillens
 * @author Filip Hrisafov
 */
@Service
public class ServerConfigService extends AbstractEncryptingService {

    protected final FlowableAdminAppProperties properties;

    @Autowired
    protected ServerConfigRepository serverConfigRepository;

    public ServerConfigService(FlowableAdminAppProperties properties) {
        super(properties);
        this.properties = properties;
    }

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

        ServerConfig config = properties.getServerConfig().get(endpointType);
        if (config == null) {
            throw new FlowableIllegalArgumentException("Configuration for '" + endpointType + "' is missing.");
        }

        validateServerConfig(endpointType, config);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setName(config.getName());
        serverConfig.setDescription(config.getDescription());
        serverConfig.setServerAddress(config.getServerAddress());
        serverConfig.setPort(config.getPort());
        serverConfig.setContextRoot(config.getContextRoot());
        serverConfig.setRestRoot(config.getRestRoot());
        serverConfig.setUserName(config.getUserName());
        serverConfig.setPassword(config.getPassword());
        serverConfig.setEndpointType(endpointType.getEndpointCode());

        return serverConfig;
    }

    protected void validateServerConfig(EndpointType type, ServerConfig config) {
        String endpointPrefixVariable = "flowable.admin.app.server-config." + type.name().toLowerCase();
        Assert.hasText(config.getName(), endpointPrefixVariable + ".name must be set");
        Assert.hasText(config.getDescription(), endpointPrefixVariable + ".description must be set");
        //TODO needs to be host
        Assert.hasText(config.getServerAddress(), endpointPrefixVariable + ".server-address must be set");
        Assert.notNull(config.getPort(), endpointPrefixVariable + ".port must be set");
        Assert.hasText(config.getContextRoot(), endpointPrefixVariable + ".context-root must be set");
        Assert.hasText(config.getRestRoot(), endpointPrefixVariable + ".rest-root must be set");
        Assert.hasText(config.getUserName(), endpointPrefixVariable + ".user-name must be set");
        Assert.hasText(config.getPassword(), endpointPrefixVariable + ".password must be set");
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
