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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.EngineConfigurator;
import org.flowable.engine.impl.cfg.IdmEngineConfigurator;
import org.flowable.engine.impl.util.EngineServiceUtil;

/**
 * A {@link EngineConfigurator} that integrates a LDAP system with the Flowable process engine. The LDAP system will be consulted primarily for getting user information and in particular for
 * fetching groups of a user.
 * 
 * This class is extensible and many methods can be overridden when the default behavior is not fitting your use case.
 * 
 * Check the docs (specifically the setters) to see how this class can be tweaked.
 * 
 * @author Joram Barrez
 */
public class LDAPConfigurator extends IdmEngineConfigurator {

    protected LDAPConfiguration ldapConfiguration;

    @Override
    public void beforeInit(AbstractEngineConfiguration engineConfiguration) {
        // Nothing to do
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        
        this.idmEngineConfiguration = new LdapIdmEngineConfiguration();
        
        if (ldapConfiguration == null) {
            throw new FlowableException("ldapConfiguration is not set");
        }

        LDAPGroupCache ldapGroupCache = null;
        if (ldapConfiguration.getGroupCacheSize() > 0) {
            ldapGroupCache = new LDAPGroupCache(ldapConfiguration.getGroupCacheSize(), 
                    ldapConfiguration.getGroupCacheExpirationTime(), engineConfiguration.getClock());
            
            if (ldapConfiguration.getGroupCacheListener() != null) {
                ldapGroupCache.setLdapCacheListener(ldapConfiguration.getGroupCacheListener());
            }
        }
        
        super.configure(engineConfiguration);
        
        EngineServiceUtil.getIdmEngineConfiguration(engineConfiguration)
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
