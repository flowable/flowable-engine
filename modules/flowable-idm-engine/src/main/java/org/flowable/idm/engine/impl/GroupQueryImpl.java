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
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.GroupQueryProperty;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class GroupQueryImpl extends AbstractQuery<GroupQuery, Group> implements GroupQuery {

    private static final long serialVersionUID = 1L;
    protected String id;
    protected List<String> ids;
    protected String name;
    protected String nameLike;
    protected String nameLikeIgnoreCase;
    protected String type;
    protected String userId;
    protected List<String> userIds;

    public GroupQueryImpl() {
    }

    public GroupQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public GroupQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public GroupQuery groupId(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided id is null");
        }
        this.id = id;
        return this;
    }

    @Override
    public GroupQuery groupIds(List<String> ids) {
        if (ids == null) {
            throw new FlowableIllegalArgumentException("Provided id list is null");
        }
        this.ids = ids;
        return this;
    }

    @Override
    public GroupQuery groupName(String name) {
        if (name == null) {
            throw new FlowableIllegalArgumentException("Provided name is null");
        }
        this.name = name;
        return this;
    }

    @Override
    public GroupQuery groupNameLike(String nameLike) {
        if (nameLike == null) {
            throw new FlowableIllegalArgumentException("Provided name is null");
        }
        this.nameLike = nameLike;
        return this;
    }

    @Override
    public GroupQuery groupNameLikeIgnoreCase(String nameLikeIgnoreCase) {
        if (nameLikeIgnoreCase == null) {
            throw new FlowableIllegalArgumentException("Provided name is null");
        }
        this.nameLikeIgnoreCase = nameLikeIgnoreCase.toLowerCase();
        return this;
    }

    @Override
    public GroupQuery groupType(String type) {
        if (type == null) {
            throw new FlowableIllegalArgumentException("Provided type is null");
        }
        this.type = type;
        return this;
    }

    @Override
    public GroupQuery groupMember(String userId) {
        if (userId == null) {
            throw new FlowableIllegalArgumentException("Provided userId is null");
        }
        this.userId = userId;
        return this;
    }

    @Override
    public GroupQuery groupMembers(List<String> userIds) {
        if (userIds == null) {
            throw new FlowableIllegalArgumentException("Provided userIds is null");
        }
        this.userIds = userIds;
        return this;
    }

    // sorting ////////////////////////////////////////////////////////

    @Override
    public GroupQuery orderByGroupId() {
        return orderBy(GroupQueryProperty.GROUP_ID);
    }

    @Override
    public GroupQuery orderByGroupName() {
        return orderBy(GroupQueryProperty.NAME);
    }

    @Override
    public GroupQuery orderByGroupType() {
        return orderBy(GroupQueryProperty.TYPE);
    }

    // results ////////////////////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getGroupEntityManager(commandContext).findGroupCountByQueryCriteria(this);
    }

    @Override
    public List<Group> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getGroupEntityManager(commandContext).findGroupByQueryCriteria(this);
    }

    // getters ////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public List<String> getIds() {
        return ids;
    }

    public String getName() {
        return name;
    }

    public String getNameLike() {
        return nameLike;
    }

    public String getNameLikeIgnoreCase() {
        return nameLikeIgnoreCase;
    }

    public String getType() {
        return type;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

}
