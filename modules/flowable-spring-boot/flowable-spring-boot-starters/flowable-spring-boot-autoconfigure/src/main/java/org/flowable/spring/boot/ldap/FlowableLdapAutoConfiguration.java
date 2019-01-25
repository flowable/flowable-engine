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
package org.flowable.spring.boot.ldap;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.ldap.LDAPConfiguration;
import org.flowable.ldap.LDAPGroupCache;
import org.flowable.ldap.LDAPIdentityServiceImpl;
import org.flowable.ldap.LDAPQueryBuilder;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.condition.ConditionalOnLdap;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.security.FlowableAuthenticationProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for the Flowable LDAP Integration.
 *
 * @author Filip Hrisafov
 */
@ConditionalOnLdap
@AutoConfigureBefore({
    FlowableSecurityAutoConfiguration.class,
    IdmEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class
})
@EnableConfigurationProperties({
    FlowableLdapProperties.class
})
@Configuration
public class FlowableLdapAutoConfiguration {

    protected final FlowableLdapProperties properties;
    protected final LDAPQueryBuilder ldapQueryBuilder;
    protected final LDAPGroupCache.LDAPGroupCacheListener ldapGroupCacheListener;

    public FlowableLdapAutoConfiguration(
        FlowableLdapProperties properties,
        ObjectProvider<LDAPQueryBuilder> ldapQueryBuilder,
        ObjectProvider<LDAPGroupCache.LDAPGroupCacheListener> ldapGroupCacheListener
    ) {
        this.properties = properties;
        this.ldapQueryBuilder = ldapQueryBuilder.getIfAvailable();
        this.ldapGroupCacheListener = ldapGroupCacheListener.getIfAvailable();
    }

    @Bean
    @ConditionalOnMissingBean
    public LDAPConfiguration ldapConfiguration() {
        LDAPConfiguration ldapConfiguration = new LDAPConfiguration();

        properties.customize(ldapConfiguration);

        if (ldapQueryBuilder != null) {
            ldapConfiguration.setLdapQueryBuilder(ldapQueryBuilder);
        }

        if (ldapGroupCacheListener != null) {
            ldapConfiguration.setGroupCacheListener(ldapGroupCacheListener);
        }

        return ldapConfiguration;
    }

    @Bean
    public EngineConfigurationConfigurer<SpringIdmEngineConfiguration> ldapIdmEngineConfigurer(LDAPConfiguration ldapConfiguration) {
        return idmEngineConfiguration -> idmEngineConfiguration
            .setIdmIdentityService(new LDAPIdentityServiceImpl(ldapConfiguration, createCache(idmEngineConfiguration, ldapConfiguration)));
    }

    // We need a custom AuthenticationProvider for the LDAP Support
    // since the default (DaoAuthenticationProvider) from Spring Security
    // uses a Password encoder to perform the matching and we need to use
    // the IdmIdentityService for LDAP
    @Bean
    @ConditionalOnMissingBean(AuthenticationProvider.class)
    public FlowableAuthenticationProvider flowableAuthenticationProvider(IdmIdentityService idmIdentitySerivce,
        UserDetailsService userDetailsService) {
        return new FlowableAuthenticationProvider(idmIdentitySerivce, userDetailsService);
    }

    protected LDAPGroupCache createCache(SpringIdmEngineConfiguration engineConfiguration, LDAPConfiguration ldapConfiguration) {
        LDAPGroupCache ldapGroupCache = null;
        if (ldapConfiguration.getGroupCacheSize() > 0) {
            // We need to use a supplier for the clock as the clock would be created later
            ldapGroupCache = new LDAPGroupCache(ldapConfiguration.getGroupCacheSize(),
                ldapConfiguration.getGroupCacheExpirationTime(), engineConfiguration::getClock);

            if (ldapConfiguration.getGroupCacheListener() != null) {
                ldapGroupCache.setLdapCacheListener(ldapConfiguration.getGroupCacheListener());
            }
        }
        return ldapGroupCache;
    }

}
