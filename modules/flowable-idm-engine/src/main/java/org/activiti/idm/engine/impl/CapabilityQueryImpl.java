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

package org.activiti.idm.engine.impl;

import java.util.List;

import org.activiti.engine.common.impl.Page;
import org.activiti.idm.api.Capability;
import org.activiti.idm.api.CapabilityQuery;
import org.activiti.idm.engine.impl.interceptor.CommandContext;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;

/**
 * @author Joram Barrez
 */
public class CapabilityQueryImpl extends AbstractQuery<CapabilityQuery, Capability> implements CapabilityQuery {

  private static final long serialVersionUID = 1L;
  
  protected String id;
  protected String capabilityName;
  protected String userId;
  protected String groupId;
  protected List<String> groupIds;

  public CapabilityQueryImpl() {
  }

  public CapabilityQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public CapabilityQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }
  
  @Override
  public CapabilityQuery capabilityId(String id) {
    this.id = id;
    return this;
  }

  @Override
  public CapabilityQuery capabilityName(String capabilityName) {
    this.capabilityName = capabilityName;
    return this;
  }

  @Override
  public CapabilityQuery userId(String userId) {
    this.userId = userId;
    return this;
  }

  @Override
  public CapabilityQuery groupId(String groupId) {
    this.groupId = groupId;
    return this;
  }
  
  @Override
  public CapabilityQuery groupIds(List<String> groupIds) {
    this.groupIds = groupIds;
    return this;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCapabilityName() {
    return capabilityName;
  }

  public void setCapabilityName(String capabilityName) {
    this.capabilityName = capabilityName;
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
    return commandContext.getCapabilityEntityManager().findCapabilityCountByQueryCriteria(this);
  }

  @Override
  public List<Capability> executeList(CommandContext commandContext, Page page) {
    return commandContext.getCapabilityEntityManager().findCapabilityByQueryCriteria(this, page);
  }

}
