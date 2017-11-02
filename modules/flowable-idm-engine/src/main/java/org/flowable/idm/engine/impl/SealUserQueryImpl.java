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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.app.security.DefaultPrivileges;
import org.flowable.engine.common.api.query.QueryProperty;
import org.flowable.engine.common.impl.AbstractQuery;
import org.flowable.engine.common.impl.Direction;

import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.CommandExecutor;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.impl.IdmIdentityServiceImpl;
import org.flowable.idm.engine.impl.UserQueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SealUserQueryImpl extends UserQueryImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(SealUserQueryImpl.class);

    private final SealIdentityServiceImpl idService;

    public SealUserQueryImpl(SealIdentityServiceImpl aThis) {
        this.idService = aThis;
    }

    @Override
    public List<User> executeList(CommandContext commandContext) {
        List<User> ret = new ArrayList<>();
        if (id == null && idIgnoreCase == null) {
            ret.addAll(createMirrorSealUsers());
        } else {
            id = id == null ? idIgnoreCase : id;
            if (findInSeal(id)) {
                ret.add(createMirrorSealUser(id));
            }
        }
        return ret;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return executeList(commandContext).size();

    }

    private boolean findInSeal(String id) {
        return true;
    }

    private List<User> createMirrorSealUsers() {
        List<User> all = new ArrayList<>();
        all.add(createMirrorSealUser("aghonim"));
        all.add(createMirrorSealUser("admin"));
        return all;
    }

    private User createMirrorSealUser(final String id) {
        User u = null;
        u = new User() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public void setId(String id) {

            }

            @Override
            public String getFirstName() {
                return id;
            }

            @Override
            public void setFirstName(String firstName) {

            }

            @Override
            public void setLastName(String lastName) {

            }

            @Override
            public String getLastName() {
                return id;
            }

            @Override
            public void setEmail(String email) {

            }

            @Override
            public String getEmail() {
                return "ahmed.ghonim@seal-software.com";
            }

            @Override
            public String getPassword() {
                return "test";
            }

            @Override
            public void setPassword(String string) {
            }

            @Override
            public boolean isPictureSet() {
                return false;
            }
        };
        initializeDefaultPrivileges(id);
        return u;
    }

    void initializeDefaultPrivileges(String userId) {
        List<Privilege> privileges = idService.createPrivilegeQuery().list();
        Map<String, Privilege> privilegeMap = new HashMap<>();
        for (Privilege privilege : privileges) {
            privilegeMap.put(privilege.getName(), privilege);
        }

        Privilege idmAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_IDM, privilegeMap);
        idService.addUserPrivilegeMapping(idmAppPrivilege.getId(), userId);

        Privilege adminAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_ADMIN, privilegeMap);
        idService.addUserPrivilegeMapping(adminAppPrivilege.getId(), userId);

        Privilege modelerAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_MODELER, privilegeMap);
        idService.addUserPrivilegeMapping(modelerAppPrivilege.getId(), userId);

        Privilege taskAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_TASK, privilegeMap);
        idService.addUserPrivilegeMapping(taskAppPrivilege.getId(), userId);
    }

    protected Privilege findOrCreatePrivilege(String privilegeId, Map<String, Privilege> privilegeMap) {
        Privilege privilege = null;
        if (privilegeMap.containsKey(privilegeId)) {
            privilege = privilegeMap.get(privilegeId);
        } else {
            privilege = idService.createPrivilege(privilegeId);
        }

        return privilege;
    }
}
