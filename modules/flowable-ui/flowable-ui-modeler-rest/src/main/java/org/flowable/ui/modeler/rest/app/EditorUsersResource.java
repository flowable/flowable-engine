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
package org.flowable.ui.modeler.rest.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EditorUsersResource implements InitializingBean {

    private static final int MAX_USER_SIZE = 100;

    @Autowired(required = false)
    protected RemoteIdmService remoteIdmService;

    @Autowired(required = false)
    protected IdmIdentityService identityService;

    @Override
    public void afterPropertiesSet() {
        if (remoteIdmService == null && identityService == null) {
            throw new FlowableIllegalStateException("No remoteIdmService or identityService have been provided");
        }
    }

    @GetMapping(value = "/rest/editor-users")
    public ResultListDataRepresentation getUsers(@RequestParam(value = "filter", required = false) String filter) {
        List<? extends User> matchingUsers;
        if (remoteIdmService != null) {
            matchingUsers = remoteIdmService.findUsersByNameFilter(filter);
        } else {
            UserQuery userQuery = identityService.createUserQuery();
            if (StringUtils.isNotEmpty(filter)) {
                userQuery.userFullNameLikeIgnoreCase("%" + filter + "%");
            }

            matchingUsers = userQuery.listPage(0, MAX_USER_SIZE);
        }
        List<UserRepresentation> userRepresentations = new ArrayList<>(matchingUsers.size());
        for (User user : matchingUsers) {
            userRepresentations.add(new UserRepresentation(user));
        }
        return new ResultListDataRepresentation(userRepresentations);
    }

}
