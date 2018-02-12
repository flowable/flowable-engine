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
package org.flowable.app.conf;

import javax.sql.DataSource;

import org.flowable.engine.common.impl.util.DefaultClockImpl;
import org.flowable.engine.common.runtime.Clock;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.IdmManagementService;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.ServiceImpl;
import org.flowable.idm.spring.SpringIdmEngineConfiguration;
import org.flowable.idm.spring.authentication.SpringEncoder;
import org.flowable.ldap.LDAPConfiguration;
import org.flowable.ldap.LDAPGroupCache;
import org.flowable.ldap.LDAPGroupCache.LDAPGroupCacheListener;
import org.flowable.ldap.LDAPIdentityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ComponentScan(basePackages = {
    "org.flowable.idm.extension.conf", // For custom configuration classes
    "org.flowable.idm.extension.bean" // For custom beans
})
public class FlowableIdmEngineConfiguration {

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired(required = false)
    protected LDAPGroupCacheListener groupCacheListener;

    @Autowired
    protected Environment environment;

    @Qualifier("customIdmIdentityService")
    @Autowired(required = false)
    protected IdmIdentityService idService;

    @Bean(name = "idmEngine")
    public IdmEngine idmEngine() {
        return idmEngineConfiguration().buildIdmEngine();
    }

    @Bean(name = "idmEngineConfiguration")
    public IdmEngineConfiguration idmEngineConfiguration() {
        SpringIdmEngineConfiguration idmEngineConfiguration = new SpringIdmEngineConfiguration();
        idmEngineConfiguration.setDataSource(dataSource);
        idmEngineConfiguration.setDatabaseSchemaUpdate(IdmEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        idmEngineConfiguration.setTransactionManager(transactionManager);

        if (environment.getProperty("ldap.enabled", Boolean.class, false)) {
            initializeLdap(idmEngineConfiguration);
        } else {
            idmEngineConfiguration.setPasswordEncoder(new SpringEncoder(passwordEncoder()));
        }

        return idmEngineConfiguration;
    }

    protected void initializeLdap(SpringIdmEngineConfiguration idmEngineConfiguration) {
        LDAPConfiguration ldapConfiguration = new LDAPConfiguration();
        ldapConfiguration.setServer(environment.getRequiredProperty("ldap.server"));
        ldapConfiguration.setPort(environment.getRequiredProperty("ldap.port", Integer.class));
        ldapConfiguration.setUser(environment.getRequiredProperty("ldap.user"));
        ldapConfiguration.setPassword(environment.getRequiredProperty("ldap.password"));

        ldapConfiguration.setBaseDn(environment.getRequiredProperty("ldap.basedn"));
        ldapConfiguration.setQueryUserByUserId(environment.getRequiredProperty("ldap.query.userbyid"));
        ldapConfiguration.setQueryUserByFullNameLike(environment.getRequiredProperty("ldap.query.userbyname"));
        ldapConfiguration.setQueryAllUsers(environment.getRequiredProperty("ldap.query.userall"));
        ldapConfiguration.setQueryGroupsForUser(environment.getRequiredProperty("ldap.query.groupsforuser"));
        ldapConfiguration.setQueryAllGroups(environment.getRequiredProperty("ldap.query.groupall"));

        ldapConfiguration.setUserIdAttribute(environment.getRequiredProperty("ldap.attribute.userid"));
        ldapConfiguration.setUserFirstNameAttribute(environment.getRequiredProperty("ldap.attribute.firstname"));
        ldapConfiguration.setUserLastNameAttribute(environment.getRequiredProperty("ldap.attribute.lastname"));
        ldapConfiguration.setUserEmailAttribute(environment.getRequiredProperty("ldap.attribute.email"));

        ldapConfiguration.setGroupIdAttribute(environment.getRequiredProperty("ldap.attribute.groupid"));
        ldapConfiguration.setGroupNameAttribute(environment.getRequiredProperty("ldap.attribute.groupname"));

        ldapConfiguration.setGroupCacheSize(environment.getRequiredProperty("ldap.cache.groupsize", Integer.class));
        ldapConfiguration.setGroupCacheExpirationTime(environment.getRequiredProperty("ldap.cache.groupexpiration", Long.class));

        LDAPGroupCache ldapGroupCache = null;
        if (ldapConfiguration.getGroupCacheSize() > 0) {
            ldapGroupCache = new LDAPGroupCache(ldapConfiguration.getGroupCacheSize(),
                    ldapConfiguration.getGroupCacheExpirationTime(), new DefaultClockImpl());

            if (groupCacheListener != null) {
                ldapGroupCache.setLdapCacheListener(groupCacheListener);
            }
        }

        LDAPIdentityServiceImpl ldapIdentityService = new LDAPIdentityServiceImpl(ldapConfiguration, ldapGroupCache);
        idmEngineConfiguration.setIdmIdentityService(ldapIdentityService);
    }

    @Bean(name = "clock")
    @DependsOn("idmEngine")
    public Clock getClock() {
        return idmEngineConfiguration().getClock();
    }

    @Bean(name = "defaultIdmIdentityService")
    public IdmIdentityService idmIdentityService() {
        IdmIdentityService defaultIdmService = idmEngine().getIdmIdentityService();
        if (idService != null) {
            ((ServiceImpl) idService).setCommandExecutor(((ServiceImpl) defaultIdmService).getCommandExecutor());
            return idService;
        }
        return idmEngine().getIdmIdentityService();
    }

    @Bean
    public IdmManagementService idmManagementService() {
        return idmEngine().getIdmManagementService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        String encoderConfig = environment.getProperty("security.passwordencoder", String.class, "");
        if ("spring_bcrypt".equalsIgnoreCase(encoderConfig)) {
            return new BCryptPasswordEncoder();
        } else {
            return NoOpPasswordEncoder.getInstance();
        }
    }
}
