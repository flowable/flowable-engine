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
package flowable;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.spring.boot.ldap.FlowableLdapProperties;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * @author Filip Hrisafov
 */
@ActiveProfiles("test")
public abstract class AbstractSampleLdapTest {

    @Autowired
    protected FlowableLdapProperties ldapProperties;

    @Autowired
    protected IdmIdentityService idmIdentityService;

    protected InMemoryDirectoryServer directoryServer;

    private Collection<String> privilegeIds = new ArrayList<>();

    @Before
    public void createLdapService() throws LDAPException {
        directoryServer = InMemoryDirectoryServerCreator.create(ldapProperties);
        directoryServer.startListening();

        Privilege repositoryPrivilege = idmIdentityService.createPrivilege("repository-privilege");
        Privilege managementPrivilege = idmIdentityService.createPrivilege("management-privilege");

        privilegeIds.add(repositoryPrivilege.getId());
        privilegeIds.add(managementPrivilege.getId());

        idmIdentityService.addGroupPrivilegeMapping(repositoryPrivilege.getId(), "user");
        idmIdentityService.addGroupPrivilegeMapping(managementPrivilege.getId(), "admin");
        idmIdentityService.addUserPrivilegeMapping(managementPrivilege.getId(), "fozzie");
    }

    @After
    public void shutDownLdapServer() {
        directoryServer.shutDown(true);
        privilegeIds.forEach(idmIdentityService::deletePrivilege);
    }
}
