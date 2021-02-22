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
package org.flowable.ui.idm.conf;

import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.ui.idm.properties.FlowableIdmAppProperties;
import org.flowable.ui.idm.service.keycloak.KeycloakConfiguration;
import org.flowable.ui.idm.service.keycloak.KeycloakIdentityServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Filip Hrisafov
 */
@AutoConfigureBefore({
        FlowableSecurityAutoConfiguration.class,
        IdmEngineServicesAutoConfiguration.class,
        ProcessEngineServicesAutoConfiguration.class,
})
@AutoConfigureAfter({
        LdapAutoConfiguration.class
})
@ConditionalOnProperty(prefix = "flowable.idm.app.keycloak", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(name = "ldapIdmEngineConfigurer") // should only be activated if ldap is not enabled
@Configuration(proxyBeanMethods = false)
public class KeycloakIdmAutoConfiguration {

    protected final FlowableIdmAppProperties properties;

    public KeycloakIdmAutoConfiguration(FlowableIdmAppProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public KeycloakConfiguration flowableKeycloakConfiguration() {
        FlowableIdmAppProperties.Keycloak keycloak = properties.getKeycloak();
        KeycloakConfiguration keycloakConfiguration = new KeycloakConfiguration();

        PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        mapper.from(keycloak.getServer()).to(keycloakConfiguration::setServer);
        mapper.from(keycloak.getAuthenticationRealm()).to(keycloakConfiguration::setAuthenticationRealm);
        mapper.from(keycloak.getAuthenticationUser()).to(keycloakConfiguration::setAuthenticationUser);
        mapper.from(keycloak.getAuthenticationPassword()).to(keycloakConfiguration::setAuthenticationPassword);
        mapper.from(keycloak.getRealm()).to(keycloakConfiguration::setRealm);

        return keycloakConfiguration;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringIdmEngineConfiguration> keycloakIdmEngineConfigurer(KeycloakConfiguration keycloakConfiguration) {
        return engineConfiguration -> {
            engineConfiguration.setIdmIdentityService(new KeycloakIdentityServiceImpl(keycloakConfiguration, engineConfiguration));
        };
    }

}
