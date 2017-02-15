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

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.Page;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.api.UserQueryProperty;
import org.flowable.idm.engine.impl.interceptor.CommandContext;
import org.flowable.idm.engine.impl.interceptor.CommandExecutor;

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

    public UserQueryImpl() {
    }

    public UserQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public UserQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public UserQuery userId(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided id is null");
        }
        this.id = id;
        return this;
    }

    public UserQuery userIds(List<String> ids) {
        if (ids == null) {
            throw new FlowableIllegalArgumentException("Provided ids is null");
        }
        this.ids = ids;
        return this;
    }

    public UserQuery userIdIgnoreCase(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided id is null");
        }
        this.idIgnoreCase = id.toLowerCase();
        return this;
    }

    public UserQuery userFirstName(String firstName) {
        if (firstName == null) {
            throw new FlowableIllegalArgumentException("Provided first name is null");
        }
        this.firstName = firstName;
        return this;
    }

    public UserQuery userFirstNameLike(String firstNameLike) {
        if (firstNameLike == null) {
            throw new FlowableIllegalArgumentException("Provided first name is null");
        }
        this.firstNameLike = firstNameLike;
        return this;
    }

    public UserQuery userFirstNameLikeIgnoreCase(String firstNameLikeIgnoreCase) {
        if (firstNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided first name is null");
        }
        this.firstNameLikeIgnoreCase = firstNameLikeIgnoreCase.toLowerCase();
        return this;
    }

    public UserQuery userLastName(String lastName) {
        if (lastName == null) {
            throw new FlowableIllegalArgumentException("Provided last name is null");
        }
        this.lastName = lastName;
        return this;
    }

    public UserQuery userLastNameLike(String lastNameLike) {
        if (lastNameLike == null) {
            throw new FlowableIllegalArgumentException("Provided last name is null");
        }
        this.lastNameLike = lastNameLike;
        return this;
    }

    public UserQuery userLastNameLikeIgnoreCase(String lastNameLikeIgnoreCase) {
        if (lastNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided last name is null");
        }
        this.lastNameLikeIgnoreCase = lastNameLikeIgnoreCase.toLowerCase();
        return this;
    }

    public UserQuery userFullNameLike(String fullNameLike) {
        if (fullNameLike == null) {
            throw new FlowableIllegalArgumentException("Provided full name is null");
        }
        this.fullNameLike = fullNameLike;
        return this;
    }

    public UserQuery userFullNameLikeIgnoreCase(String fullNameLikeIgnoreCase) {
        if (fullNameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided full name is null");
        }
        this.fullNameLikeIgnoreCase = fullNameLikeIgnoreCase.toLowerCase();
        return this;
    }

    public UserQuery userEmail(String email) {
        if (email == null) {
            throw new FlowableIllegalArgumentException("Provided email is null");
        }
        this.email = email;
        return this;
    }

    public UserQuery userEmailLike(String emailLike) {
        if (emailLike == null) {
            throw new FlowableIllegalArgumentException("Provided emailLike is null");
        }
        this.emailLike = emailLike;
        return this;
    }

    public UserQuery memberOfGroup(String groupId) {
        if (groupId == null) {
            throw new FlowableIllegalArgumentException("Provided groupId is null");
        }
        this.groupId = groupId;
        return this;
    }

    public UserQuery memberOfGroups(List<String> groupIds) {
        if (groupIds == null) {
            throw new FlowableIllegalArgumentException("Provided groupIds is null");
        }
        this.groupIds = groupIds;
        return this;
    }

    // sorting //////////////////////////////////////////////////////////

    public UserQuery orderByUserId() {
        return orderBy(UserQueryProperty.USER_ID);
    }

    public UserQuery orderByUserEmail() {
        return orderBy(UserQueryProperty.EMAIL);
    }

    public UserQuery orderByUserFirstName() {
        return orderBy(UserQueryProperty.FIRST_NAME);
    }

    public UserQuery orderByUserLastName() {
        return orderBy(UserQueryProperty.LAST_NAME);
    }

    // results //////////////////////////////////////////////////////////

    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return commandContext.getUserEntityManager().findUserCountByQueryCriteria(this);
    }

    public List<User> executeList(CommandContext commandContext, Page page) {
        checkQueryOk();
        return commandContext.getUserEntityManager().findUserByQueryCriteria(this, page);
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
