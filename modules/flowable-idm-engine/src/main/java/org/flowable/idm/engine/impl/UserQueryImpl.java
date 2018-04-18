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

package org.flowable.idm.engine.impl;

import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.api.UserQueryProperty;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class UserQueryImpl extends AbstractQuery<UserQuery, User> implements UserQuery {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected List<String> ids;
    protected String idIgnoreCase;
    protected String firstName;
    protected String firstNameLike;
    protected String firstNameLikeIgnoreCase;
    protected String lastName;
    protected String lastNameLike;
    protected String lastNameLikeIgnoreCase;
    protected String fullNameLike;
    protected String fullNameLikeIgnoreCase;
    protected String email;
    protected String emailLike;
    protected String groupId;
    protected List<String> groupIds;
    protected String tenantId;

    public UserQueryImpl() {
    }

    public UserQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public UserQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public UserQuery userId(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided id is null");
        }
        this.id = id;
        return this;
    }

    @Override
    public UserQuery userIds(List<String> ids) {
        if (ids == null) {
            throw new FlowableIllegalArgumentException("Provided ids is null");
        }
        this.ids = ids;
        return this;
    }

    @Override
    public UserQuery userIdIgnoreCase(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided id is null");
        }
        this.idIgnoreCase = id.toLowerCase();
        return this;
    }

    @Override
    public UserQuery userFirstName(String firstName) {
        if (firstName == null) {
            throw new FlowableIllegalArgumentException("Provided first name is null");
        }
        this.firstName = firstName;
        return this;
    }

    @Override
    public UserQuery userFirstNameLike(String firstNameLike) {
        if (firstNameLike == null) {
            throw new FlowableIllegalArgumentException("Provided first name is null");
        }
        this.firstNameLike = firstNameLike;
        return this;
    }

    @Override
    public UserQuery userFirstNameLikeIgnoreCase(String firstNameLikeIgnoreCase) {
        if (firstNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided first name is null");
        }
        this.firstNameLikeIgnoreCase = firstNameLikeIgnoreCase.toLowerCase();
        return this;
    }

    @Override
    public UserQuery userLastName(String lastName) {
        if (lastName == null) {
            throw new FlowableIllegalArgumentException("Provided last name is null");
        }
        this.lastName = lastName;
        return this;
    }

    @Override
    public UserQuery userLastNameLike(String lastNameLike) {
        if (lastNameLike == null) {
            throw new FlowableIllegalArgumentException("Provided last name is null");
        }
        this.lastNameLike = lastNameLike;
        return this;
    }

    @Override
    public UserQuery userLastNameLikeIgnoreCase(String lastNameLikeIgnoreCase) {
        if (lastNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided last name is null");
        }
        this.lastNameLikeIgnoreCase = lastNameLikeIgnoreCase.toLowerCase();
        return this;
    }

    @Override
    public UserQuery userFullNameLike(String fullNameLike) {
        if (fullNameLike == null) {
            throw new FlowableIllegalArgumentException("Provided full name is null");
        }
        this.fullNameLike = fullNameLike;
        return this;
    }

    @Override
    public UserQuery userFullNameLikeIgnoreCase(String fullNameLikeIgnoreCase) {
        if (fullNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided full name is null");
        }
        this.fullNameLikeIgnoreCase = fullNameLikeIgnoreCase.toLowerCase();
        return this;
    }

    @Override
    public UserQuery userEmail(String email) {
        if (email == null) {
            throw new FlowableIllegalArgumentException("Provided email is null");
        }
        this.email = email;
        return this;
    }

    @Override
    public UserQuery userEmailLike(String emailLike) {
        if (emailLike == null) {
            throw new FlowableIllegalArgumentException("Provided emailLike is null");
        }
        this.emailLike = emailLike;
        return this;
    }

    @Override
    public UserQuery memberOfGroup(String groupId) {
        if (groupId == null) {
            throw new FlowableIllegalArgumentException("Provided groupId is null");
        }
        this.groupId = groupId;
        return this;
    }

    @Override
    public UserQuery memberOfGroups(List<String> groupIds) {
        if (groupIds == null) {
            throw new FlowableIllegalArgumentException("Provided groupIds is null");
        }
        this.groupIds = groupIds;
        return this;
    }

    @Override
    public UserQuery tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("TenantId is null");
        }
        this.tenantId = tenantId;
        return this;
    }

    // sorting //////////////////////////////////////////////////////////

    @Override
    public UserQuery orderByUserId() {
        return orderBy(UserQueryProperty.USER_ID);
    }

    @Override
    public UserQuery orderByUserEmail() {
        return orderBy(UserQueryProperty.EMAIL);
    }

    @Override
    public UserQuery orderByUserFirstName() {
        return orderBy(UserQueryProperty.FIRST_NAME);
    }

    @Override
    public UserQuery orderByUserLastName() {
        return orderBy(UserQueryProperty.LAST_NAME);
    }

    // results //////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getUserEntityManager(commandContext).findUserCountByQueryCriteria(this);
    }

    @Override
    public List<User> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getUserEntityManager(commandContext).findUserByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public List<String> getIds() {
        return ids;
    }

    public String getIdIgnoreCase() {
        return idIgnoreCase;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFirstNameLike() {
        return firstNameLike;
    }

    public String getFirstNameLikeIgnoreCase() {
        return firstNameLikeIgnoreCase;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLastNameLike() {
        return lastNameLike;
    }

    public String getLastNameLikeIgnoreCase() {
        return lastNameLikeIgnoreCase;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailLike() {
        return emailLike;
    }

    public String getGroupId() {
        return groupId;
    }

    public List<String> getGroupIds() {
        return groupIds;
    }

    public String getFullNameLike() {
        return fullNameLike;
    }

    public String getFullNameLikeIgnoreCase() {
        return fullNameLikeIgnoreCase;
    }

}
