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

import org.flowable.idm.engine.IdmEngineConfiguration;

/**
 * Lightweight {@link IdmEngineConfiguration} to be used when running with LDAP.
 * 
 * @author Joram Barrez
 */
public class LdapIdmEngineConfiguration extends IdmEngineConfiguration {
    
    public LdapIdmEngineConfiguration() {
        setUsingRelationalDatabase(false);
    }
    
    @Override
    public void initDataManagers() {
        // No need to initialize data managers when using ldap
    }

}
