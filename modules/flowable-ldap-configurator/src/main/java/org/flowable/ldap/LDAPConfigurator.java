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
package org.flowable.ldap;

import org.flowable.engine.cfg.AbstractProcessEngineConfigurator;
import org.flowable.engine.cfg.ProcessEngineConfigurator;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.EngineServiceUtil;

/**
 * A {@link ProcessEngineConfigurator} that integrates a LDAP system with the Flowable process engine. The LDAP system will be consulted primarily for getting user information and in particular for
 * fetching groups of a user.
 * 
 * This class is extensible and many methods can be overridden when the default behavior is not fitting your use case.
 * 
 * Check the docs (specifically the setters) to see how this class can be tweaked.
 * 
 * @author Joram Barrez
 */
public class LDAPConfigurator extends AbstractProcessEngineConfigurator {

    protected LDAPConfiguration ldapConfiguration;

    @Override
    public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // Nothing to do
    }

    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        
        if (ldapConfiguration == null) {
            throw new FlowableException("ldapConfiguration is not set");
        }

        LDAPGroupCache ldapGroupCache = null;
        if (ldapConfiguration.getGroupCacheSize() > 0) {
            ldapGroupCache = new LDAPGroupCache(ldapConfiguration.getGroupCacheSize(), 
                    ldapConfiguration.getGroupCacheExpirationTime(), processEngineConfiguration.getClock());
            
            if (ldapConfiguration.getGroupCacheListener() != null) {
                ldapGroupCache.setLdapCacheListener(ldapConfiguration.getGroupCacheListener());
            }
        }

        EngineServiceUtil.getIdmEngineConfiguration(processEngineConfiguration)
                .setIdmIdentityService(new LDAPIdentityServiceImpl(ldapConfiguration, ldapGroupCache));
    }

    // Getters and Setters //////////////////////////////////////////////////

    public LDAPConfiguration getLdapConfiguration() {
        return ldapConfiguration;
    }

    public void setLdapConfiguration(LDAPConfiguration ldapConfiguration) {
        this.ldapConfiguration = ldapConfiguration;
    }

}
