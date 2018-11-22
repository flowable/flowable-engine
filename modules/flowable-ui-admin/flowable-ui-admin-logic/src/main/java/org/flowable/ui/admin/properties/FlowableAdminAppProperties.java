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
package org.flowable.ui.admin.properties;

import java.util.EnumMap;
import java.util.Map;

import org.flowable.ui.admin.domain.EndpointType;
import org.flowable.ui.admin.domain.ServerConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.Assert;

/**
 * Properties for the Admin UI App.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.admin.app")
public class FlowableAdminAppProperties implements InitializingBean {

    /**
     * The prefix for the database tables.
     */
    private String dataSourcePrefix = "";

    /**
     * The configuration for the server endpoints.
     */
    private Map<EndpointType, ServerConfig> serverConfig = new EnumMap<>(EndpointType.class);

    /**
     * The security configuration for the admin UI application.
     */
    @NestedConfigurationProperty
    private final Security security = new Security();

    public String getDataSourcePrefix() {
        return dataSourcePrefix;
    }

    public void setDataSourcePrefix(String dataSourcePrefix) {
        this.dataSourcePrefix = dataSourcePrefix;
    }

    public Map<EndpointType, ServerConfig> getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(Map<EndpointType, ServerConfig> serverConfig) {
        this.serverConfig = serverConfig;
    }

    public Security getSecurity() {
        return security;
    }

    @Override
    public void afterPropertiesSet() {
        // We are not using validation so we can use JSR303 to do the validation
        Encryption encryption = getSecurity().getEncryption();
        Assert.notNull(encryption.getCredentialsIVSpec(), "flowable.admin.app.security.encryption.credentials-i-v-spec must be set");
        Assert.notNull(encryption.getCredentialsSecretSpec(), "flowable.admin.app.security.encryption.credentials-secret-spec must be set");
        validateServerConfig();
    }

    private void validateServerConfig() {
        for (Map.Entry<EndpointType, ServerConfig> configEntry : serverConfig.entrySet()) {
            EndpointType type = configEntry.getKey();
            ServerConfig config = configEntry.getValue();
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
    }

    /**
     * The security configuration for the Admin Application.
     */
    public static class Security {

        /**
         * The encryption configuration for the rest endpoints.
         */
        @NestedConfigurationProperty
        private final Encryption encryption = new Encryption();

        /**
         * Perform a preemptive basic authentication when issuing requests to the flowable REST API.
         * This is an experimental property and might be removed without notice.
         */
        private boolean preemptiveBasicAuthentication = false;

        public Encryption getEncryption() {
            return encryption;
        }

        public boolean isPreemptiveBasicAuthentication() {
            return preemptiveBasicAuthentication;
        }

        public void setPreemptiveBasicAuthentication(boolean preemptiveBasicAuthentication) {
            this.preemptiveBasicAuthentication = preemptiveBasicAuthentication;
        }
    }

    /**
     * Passwords for rest endpoints and master configs are stored encrypted in the database using AES/CBC/PKCS5PADDING
     * It needs a 128-bit initialization vector (http://en.wikipedia.org/wiki/Initialization_vector)
     * and a 128-bit secret key represented as 16 ascii characters below
     * <p>
     * Do note that if these properties are changed after passwords have been saved, all existing passwords
     * will not be able to be decrypted and the password would need to be reset in the UI.
     */
    public static class Encryption {

        /**
         * The string that needs to be used to create an IvParameterSpec object using it's the bytes.
         */
        private String credentialsIVSpec;

        /**
         * The string that needs to be used to create a SecretKeySpec using it's bytes.
         */
        private String credentialsSecretSpec;

        public String getCredentialsIVSpec() {
            return credentialsIVSpec;
        }

        public void setCredentialsIVSpec(String credentialsIVSpec) {
            this.credentialsIVSpec = credentialsIVSpec;
        }

        public String getCredentialsSecretSpec() {
            return credentialsSecretSpec;
        }

        public void setCredentialsSecretSpec(String credentialsSecretSpec) {
            this.credentialsSecretSpec = credentialsSecretSpec;
        }
    }
}
