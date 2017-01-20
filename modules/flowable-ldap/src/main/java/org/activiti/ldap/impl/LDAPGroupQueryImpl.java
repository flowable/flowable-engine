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
package org.activiti.ldap.impl;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.common.api.ActivitiIllegalArgumentException;
import org.activiti.engine.common.impl.Page;
import org.activiti.idm.api.Group;
import org.activiti.idm.engine.impl.GroupQueryImpl;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntity;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.activiti.ldap.LDAPCallBack;
import org.activiti.ldap.LDAPConfigurator;
import org.activiti.ldap.LDAPGroupCache;
import org.activiti.ldap.LDAPTemplate;

public class LDAPGroupQueryImpl extends GroupQueryImpl {
  
  private static final long serialVersionUID = 1L;
  
  protected LDAPConfigurator ldapConfigurator;
  protected LDAPGroupCache ldapGroupCache;
  
  public LDAPGroupQueryImpl(LDAPConfigurator ldapConfigurator, LDAPGroupCache ldapGroupCache) {
    this.ldapConfigurator = ldapConfigurator;
    this.ldapGroupCache = ldapGroupCache;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    return executeQuery().size();
  }

  @Override
  public List<Group> executeList(CommandContext commandContext, Page page) {
    return executeQuery();
  }
  
  protected List<Group> executeQuery() {
    if (getUserId() != null) {
      return findGroupsByUser(getUserId());
    } else {
      throw new ActivitiIllegalArgumentException("This query is not supported by the LDAPGroupManager");
    }
  }
  
  protected List<Group> findGroupsByUser(final String userId) {

    // First try the cache (if one is defined)
    if (ldapGroupCache != null) {
      List<Group> groups = ldapGroupCache.get(userId);
      if (groups != null) {
        return groups;
      }
    }

    // Do the search against Ldap
    LDAPTemplate ldapTemplate = new LDAPTemplate(ldapConfigurator);
    return ldapTemplate.execute(new LDAPCallBack<List<Group>>() {

      public List<Group> executeInContext(InitialDirContext initialDirContext) {

        String searchExpression = ldapConfigurator.getLdapQueryBuilder().buildQueryGroupsForUser(ldapConfigurator, userId);

        List<Group> groups = new ArrayList<Group>();
        try {
          String baseDn = ldapConfigurator.getGroupBaseDn() != null ? ldapConfigurator.getGroupBaseDn() : ldapConfigurator.getBaseDn();
          NamingEnumeration<?> namingEnum = initialDirContext.search(baseDn, searchExpression, createSearchControls());
          while (namingEnum.hasMore()) { // Should be only one
            SearchResult result = (SearchResult) namingEnum.next();

            GroupEntity group = new GroupEntityImpl();
            if (ldapConfigurator.getGroupIdAttribute() != null) {
              group.setId(result.getAttributes().get(ldapConfigurator.getGroupIdAttribute()).get().toString());
            }
            if (ldapConfigurator.getGroupNameAttribute() != null) {
              group.setName(result.getAttributes().get(ldapConfigurator.getGroupNameAttribute()).get().toString());
            }
            if (ldapConfigurator.getGroupTypeAttribute() != null) {
              group.setType(result.getAttributes().get(ldapConfigurator.getGroupTypeAttribute()).get().toString());
            }
            groups.add(group);
          }

          namingEnum.close();

          // Cache results for later
          if (ldapGroupCache != null) {
            ldapGroupCache.add(userId, groups);
          }

          return groups;

        } catch (NamingException e) {
          throw new ActivitiException("Could not find groups for user " + userId, e);
        }
      }

    });
  }
  
  protected SearchControls createSearchControls() {
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    searchControls.setTimeLimit(ldapConfigurator.getSearchTimeLimit());
    return searchControls;
  }
}