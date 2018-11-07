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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.UserQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityImpl;
import org.flowable.ldap.LDAPCallBack;
import org.flowable.ldap.LDAPConfiguration;
import org.flowable.ldap.LDAPTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LDAPUserQueryImpl extends UserQueryImpl {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LDAPUserQueryImpl.class);

    protected LDAPConfiguration ldapConfigurator;

    public LDAPUserQueryImpl(LDAPConfiguration ldapConfigurator) {
        this.ldapConfigurator = ldapConfigurator;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return executeQuery().size();
    }

    @Override
    public List<User> executeList(CommandContext commandContext) {
        return executeQuery();
    }

    protected List<User> executeQuery() {
        if (getId() != null) {
            List<User> result = new ArrayList<>();
            result.add(findById(getId()));
            return result;

        } else if (getIdIgnoreCase() != null) {
            List<User> result = new ArrayList<>();
            result.add(findById(getIdIgnoreCase()));
            return result;

        } else if (getFullNameLike() != null) {
            return executeNameQuery(getFullNameLike());

        } else if (getFullNameLikeIgnoreCase() != null) {
            return executeNameQuery(getFullNameLikeIgnoreCase());

        } else {
            return executeAllUserQuery();
        }
    }

    protected List<User> executeNameQuery(String name) {
        String fullName = name.replaceAll("%", "");
        String searchExpression = ldapConfigurator.getLdapQueryBuilder().buildQueryByFullNameLike(ldapConfigurator, fullName);
        return executeUsersQuery(searchExpression);
    }

    protected List<User> executeAllUserQuery() {
        String searchExpression = ldapConfigurator.getQueryAllUsers();
        return executeUsersQuery(searchExpression);
    }

    protected UserEntity findById(final String userId) {
        LDAPTemplate ldapTemplate = new LDAPTemplate(ldapConfigurator);
        return ldapTemplate.execute(new LDAPCallBack<UserEntity>() {

            @Override
            public UserEntity executeInContext(InitialDirContext initialDirContext) {
                try {

                    String searchExpression = ldapConfigurator.getLdapQueryBuilder().buildQueryByUserId(ldapConfigurator, userId);

                    String baseDn = ldapConfigurator.getUserBaseDn() != null ? ldapConfigurator.getUserBaseDn() : ldapConfigurator.getBaseDn();
                    NamingEnumeration<?> namingEnum = initialDirContext.search(baseDn, searchExpression, createSearchControls());
                    UserEntity user = new UserEntityImpl();
                    while (namingEnum.hasMore()) { // Should be only one
                        SearchResult result = (SearchResult) namingEnum.next();
                        mapSearchResultToUser(result, user);
                    }
                    namingEnum.close();

                    return user;

                } catch (NamingException ne) {
                    LOGGER.error("Could not find user {} : {}", userId, ne.getMessage(), ne);
                    return null;
                }
            }

        });
    }

    protected List<User> executeUsersQuery(final String searchExpression) {
        LDAPTemplate ldapTemplate = new LDAPTemplate(ldapConfigurator);
        return ldapTemplate.execute(new LDAPCallBack<List<User>>() {

            @Override
            public List<User> executeInContext(InitialDirContext initialDirContext) {
                List<User> result = new ArrayList<>();
                try {
                    String baseDn = ldapConfigurator.getUserBaseDn() != null ? ldapConfigurator.getUserBaseDn() : ldapConfigurator.getBaseDn();
                    NamingEnumeration<?> namingEnum = initialDirContext.search(baseDn, searchExpression, createSearchControls());

                    while (namingEnum.hasMore()) {
                        SearchResult searchResult = (SearchResult) namingEnum.next();

                        UserEntity user = new UserEntityImpl();
                        mapSearchResultToUser(searchResult, user);
                        result.add(user);

                    }
                    namingEnum.close();

                } catch (NamingException ne) {
                    LOGGER.debug("Could not execute LDAP query: {}", ne.getMessage(), ne);
                    return null;
                }
                return result;
            }

        });
    }

    protected void mapSearchResultToUser(SearchResult result, UserEntity user) throws NamingException {
        if (ldapConfigurator.getUserIdAttribute() != null) {
            user.setId(result.getAttributes().get(ldapConfigurator.getUserIdAttribute()).get().toString());
        }
        if (ldapConfigurator.getUserFirstNameAttribute() != null) {
            try {
                user.setFirstName(result.getAttributes().get(ldapConfigurator.getUserFirstNameAttribute()).get().toString());
            } catch (NullPointerException e) {
                user.setFirstName("");
            }
        }
        if (ldapConfigurator.getUserLastNameAttribute() != null) {
            try {
                user.setLastName(result.getAttributes().get(ldapConfigurator.getUserLastNameAttribute()).get().toString());
            } catch (NullPointerException e) {
                user.setLastName("");
            }
        }
        if (ldapConfigurator.getUserEmailAttribute() != null) {
            try {
                user.setEmail(result.getAttributes().get(ldapConfigurator.getUserEmailAttribute()).get().toString());
            } catch (NullPointerException e) {
                user.setEmail("");
            }
        }
    }

    protected SearchControls createSearchControls() {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setTimeLimit(ldapConfigurator.getSearchTimeLimit());
        return searchControls;
    }
}
