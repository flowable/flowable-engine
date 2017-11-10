package org.flowable.idm.engine.impl;

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


import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.impl.GroupQueryImpl;

public class SealGroupQueryImpl extends GroupQueryImpl {

    private static final long serialVersionUID = 1L;

    @Override
    public long executeCount(CommandContext commandContext) {
        return executeList(commandContext).size();
    }

    private final SealIdentityServiceImpl idService;

    public SealGroupQueryImpl(SealIdentityServiceImpl aThis) {
        this.idService = aThis;
    }

    @Override
    public List<Group> executeList(CommandContext commandContext) {
        List<Group> ret = new ArrayList<>();
        if (id == null) {
            ret.addAll(createMirrorSealGroups());
        } else {
            if (findInSeal(id)) {
                ret.add(createMirrorSealGroup(id));
            }
        }
        return ret;
    }

    private List<Group> createMirrorSealGroups() {
        List<Group> all = new ArrayList<>();
        all.add(createMirrorSealGroup("group1"));
        return all;
    }

    private boolean findInSeal(String id) {
        return true;
    }

    private Group createMirrorSealGroup(final String id) {
        Group g = null;
        g = new Group() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public void setId(String id) {

            }

            @Override
            public String getName() {
                return id;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public String getType() {
                return "Seal-Group";
            }

            @Override
            public void setType(String string) {

            }
        };
        return g;
    }
//    protected List<Group> executeQuery() {
//        if (getUserId() != null) {
//            return findGroupsByUser(getUserId());
//        } else {
//            return findAllGroups();
//        }
//    }
//
//    protected List<Group> findGroupsByUser(String userId) {
//
//        // First try the cache (if one is defined)
//        if (ldapGroupCache != null) {
//            List<Group> groups = ldapGroupCache.get(userId);
//            if (groups != null) {
//                return groups;
//            }
//        }
//
//        String searchExpression = ldapConfigurator.getLdapQueryBuilder().buildQueryGroupsForUser(ldapConfigurator, userId);
//        List<Group> groups = executeGroupQuery(searchExpression);
//
//        // Cache results for later
//        if (ldapGroupCache != null) {
//            ldapGroupCache.add(userId, groups);
//        }
//
//        return groups;
//    }
//
//    protected List<Group> findAllGroups() {
//        String searchExpression = ldapConfigurator.getQueryAllGroups();
//        List<Group> groups = executeGroupQuery(searchExpression);
//        return groups;
//    }
//
//    protected List<Group> executeGroupQuery(final String searchExpression) {
//        LDAPTemplate ldapTemplate = new LDAPTemplate(ldapConfigurator);
//        return ldapTemplate.execute(new LDAPCallBack<List<Group>>() {
//
//            @Override
//            public List<Group> executeInContext(InitialDirContext initialDirContext) {
//
//                List<Group> groups = new ArrayList<>();
//                try {
//                    String baseDn = ldapConfigurator.getGroupBaseDn() != null ? ldapConfigurator.getGroupBaseDn() : ldapConfigurator.getBaseDn();
//                    NamingEnumeration<?> namingEnum = initialDirContext.search(baseDn, searchExpression, createSearchControls());
//                    while (namingEnum.hasMore()) { // Should be only one
//                        SearchResult result = (SearchResult) namingEnum.next();
//
//                        GroupEntity group = new GroupEntityImpl();
//                        if (ldapConfigurator.getGroupIdAttribute() != null) {
//                            group.setId(result.getAttributes().get(ldapConfigurator.getGroupIdAttribute()).get().toString());
//                        }
//                        if (ldapConfigurator.getGroupNameAttribute() != null) {
//                            group.setName(result.getAttributes().get(ldapConfigurator.getGroupNameAttribute()).get().toString());
//                        }
//                        if (ldapConfigurator.getGroupTypeAttribute() != null) {
//                            group.setType(result.getAttributes().get(ldapConfigurator.getGroupTypeAttribute()).get().toString());
//                        }
//                        groups.add(group);
//                    }
//
//                    namingEnum.close();
//
//                    return groups;
//
//                } catch (NamingException e) {
//                    throw new FlowableException("Could not find groups " + searchExpression, e);
//                }
//            }
//
//        });
//    }
//
//    protected SearchControls createSearchControls() {
//        SearchControls searchControls = new SearchControls();
//        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
//        searchControls.setTimeLimit(ldapConfigurator.getSearchTimeLimit());
//        return searchControls;
//    }
}
