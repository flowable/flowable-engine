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
package org.flowable.ldap.impl;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.impl.GroupQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.flowable.ldap.LDAPCallBack;
import org.flowable.ldap.LDAPConfiguration;
import org.flowable.ldap.LDAPGroupCache;
import org.flowable.ldap.LDAPTemplate;

public class LDAPGroupQueryImpl extends GroupQueryImpl {

    private static final long serialVersionUID = 1L;

    protected LDAPConfiguration ldapConfigurator;
    protected LDAPGroupCache ldapGroupCache;

    public LDAPGroupQueryImpl(LDAPConfiguration ldapConfigurator, LDAPGroupCache ldapGroupCache) {
        this.ldapConfigurator = ldapConfigurator;
        this.ldapGroupCache = ldapGroupCache;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return executeQuery().size();
    }

    @Override
    public List<Group> executeList(CommandContext commandContext) {
        return executeQuery();
    }

    protected List<Group> executeQuery() {
        if (getUserId() != null) {
            return findGroupsByUser(getUserId());
        } else {
            return findAllGroups();
        }
    }

    protected List<Group> findGroupsByUser(String userId) {

        // First try the cache (if one is defined)
        if (ldapGroupCache != null) {
            List<Group> groups = ldapGroupCache.get(userId);
            if (groups != null) {
                return groups;
            }
        }

        String searchExpression = ldapConfigurator.getLdapQueryBuilder().buildQueryGroupsForUser(ldapConfigurator, userId);
        List<Group> groups = executeGroupQuery(searchExpression);

        // Cache results for later
        if (ldapGroupCache != null) {
            ldapGroupCache.add(userId, groups);
        }

        return groups;
    }

    protected List<Group> findAllGroups() {
        String searchExpression = ldapConfigurator.getQueryAllGroups();
        List<Group> groups = executeGroupQuery(searchExpression);
        return groups;
    }

    protected List<Group> executeGroupQuery(final String searchExpression) {
        LDAPTemplate ldapTemplate = new LDAPTemplate(ldapConfigurator);
        return ldapTemplate.execute(new LDAPCallBack<List<Group>>() {

            @Override
            public List<Group> executeInContext(InitialDirContext initialDirContext) {

                List<Group> groups = new ArrayList<>();
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

                    return groups;

                } catch (NamingException e) {
                    throw new FlowableException("Could not find groups " + searchExpression, e);
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
