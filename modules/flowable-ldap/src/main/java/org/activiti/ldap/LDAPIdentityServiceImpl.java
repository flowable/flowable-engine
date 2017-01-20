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
package org.activiti.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.GroupQuery;
import org.activiti.idm.api.NativeGroupQuery;
import org.activiti.idm.api.NativeUserQuery;
import org.activiti.idm.api.User;
import org.activiti.idm.api.UserQuery;
import org.activiti.idm.engine.impl.IdmIdentityServiceImpl;
import org.activiti.ldap.impl.LDAPGroupQueryImpl;
import org.activiti.ldap.impl.LDAPUserQueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LDAPIdentityServiceImpl extends IdmIdentityServiceImpl {
  
  private static Logger logger = LoggerFactory.getLogger(LDAPIdentityServiceImpl.class);
  
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected LDAPConfigurator ldapConfigurator;
  protected LDAPGroupCache ldapGroupCache;
  
  public LDAPIdentityServiceImpl(ProcessEngineConfigurationImpl processEngineConfiguration, 
      LDAPConfigurator ldapConfigurator, LDAPGroupCache ldapGroupCache) {
    
    this.processEngineConfiguration = processEngineConfiguration;
    this.ldapConfigurator = ldapConfigurator;
    this.ldapGroupCache = ldapGroupCache;
  }
  
  @Override
  public UserQuery createUserQuery() {
    return new LDAPUserQueryImpl(ldapConfigurator);
  }
  
  @Override
  public GroupQuery createGroupQuery() {
    return new LDAPGroupQueryImpl(ldapConfigurator, ldapGroupCache);
  }
  
  @Override
  public boolean checkPassword(String userId, String password) {
    return executeCheckPassword(userId, password);
  }

  @Override
  public User newUser(String userId) {
    throw new ActivitiException("LDAP identity service doesn't support creating a new user");
  }

  @Override
  public void saveUser(User user) {
    throw new ActivitiException("LDAP identity service doesn't support saving an user");
  }

  @Override
  public NativeUserQuery createNativeUserQuery() {
    throw new ActivitiException("LDAP identity service doesn't support native querying");
  }

  @Override
  public void deleteUser(String userId) {
    throw new ActivitiException("LDAP identity service doesn't support deleting an user");
  }

  @Override
  public Group newGroup(String groupId) {
    throw new ActivitiException("LDAP identity service doesn't support creating a new group");
  }

  @Override
  public NativeGroupQuery createNativeGroupQuery() {
    throw new ActivitiException("LDAP identity service doesn't support native querying");
  }

  @Override
  public void saveGroup(Group group) {
    throw new ActivitiException("LDAP identity service doesn't support saving a group");
  }

  @Override
  public void deleteGroup(String groupId) {
    throw new ActivitiException("LDAP identity service doesn't support deleting a group");
  }
  
  protected boolean executeCheckPassword(final String userId, final String password) {
    // Extra password check, see http://forums.activiti.org/comment/22312
    if (password == null || password.length() == 0) {
      throw new ActivitiException("Null or empty passwords are not allowed!");
    }

    try {
      LDAPTemplate ldapTemplate = new LDAPTemplate(ldapConfigurator);
      return ldapTemplate.execute(new LDAPCallBack<Boolean>() {

        public Boolean executeInContext(InitialDirContext initialDirContext) {

          if (initialDirContext == null) {
            return false;
          }

          // Do the actual search for the user
          String userDn = null;
          try {

            String searchExpression = ldapConfigurator.getLdapQueryBuilder().buildQueryByUserId(ldapConfigurator, userId);
            String baseDn = ldapConfigurator.getUserBaseDn() != null ? ldapConfigurator.getUserBaseDn() : ldapConfigurator.getBaseDn();
            NamingEnumeration<?> namingEnum = initialDirContext.search(baseDn, searchExpression, createSearchControls());

            while (namingEnum.hasMore()) { // Should be only one
              SearchResult result = (SearchResult) namingEnum.next();
              userDn = result.getNameInNamespace();
            }
            namingEnum.close();

          } catch (NamingException ne) {
            logger.info("Could not authenticate user " + userId + " : " + ne.getMessage(), ne);
            return false;
          }

          // Now we have the user DN, we can need to create a connection it ('bind' in ldap lingo) to check if the user is valid
          if (userDn != null) {
            InitialDirContext verificationContext = null;
            try {
              verificationContext = LDAPConnectionUtil.createDirectoryContext(ldapConfigurator, userDn, password);
            } catch (ActivitiException e) {
              // Do nothing, an exception will be thrown if the login fails
            }

            if (verificationContext != null) {
              LDAPConnectionUtil.closeDirectoryContext(verificationContext);
              return true;
            }
          }

          return false;

        }
      });

    } catch (ActivitiException e) {
      logger.info("Could not authenticate user : " + e);
      return false;
    }
  }

  protected SearchControls createSearchControls() {
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setTimeLimit(ldapConfigurator.getSearchTimeLimit());
    return searchControls;
  }

  public LDAPGroupCache getLdapGroupCache() {
    return ldapGroupCache;
  }

  public void setLdapGroupCache(LDAPGroupCache ldapGroupCache) {
    this.ldapGroupCache = ldapGroupCache;
  }
}
