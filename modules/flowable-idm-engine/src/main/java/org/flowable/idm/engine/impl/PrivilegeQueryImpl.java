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

import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.PrivilegeQuery;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 */
public class PrivilegeQueryImpl extends AbstractQuery<PrivilegeQuery, Privilege> implements PrivilegeQuery {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String userId;
    protected String groupId;
    protected List<String> groupIds;

    public PrivilegeQueryImpl() {
    }

    public PrivilegeQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public PrivilegeQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public PrivilegeQuery privilegeId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public PrivilegeQuery privilegeName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public PrivilegeQuery userId(String userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public PrivilegeQuery groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public PrivilegeQuery groupIds(List<String> groupIds) {
        this.groupIds = groupIds;
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<String> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<String> groupIds) {
        this.groupIds = groupIds;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getPrivilegeEntityManager(commandContext).findPrivilegeCountByQueryCriteria(this);
    }

    @Override
    public List<Privilege> executeList(CommandContext commandContext) {
        return CommandContextUtil.getPrivilegeEntityManager(commandContext).findPrivilegeByQueryCriteria(this);
    }

}
