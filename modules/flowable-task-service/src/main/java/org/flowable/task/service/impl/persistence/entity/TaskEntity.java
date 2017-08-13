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
package org.flowable.task.service.impl.persistence.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.db.HasRevision;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.Task;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.variable.service.delegate.VariableScope;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface TaskEntity extends VariableScope, Task, DelegateTask, Entity, HasRevision {

    void setExecutionId(String executionId);

    List<IdentityLinkEntity> getIdentityLinks();

    void setCreateTime(Date createTime);

    void setProcessDefinitionId(String processDefinitionId);

    void setEventName(String eventName);

    //void setCurrentFlowableListener(FlowableListener currentListener);

    //ExecutionEntity getProcessInstance();

    void setProcessInstanceId(String processInstanceId);

    int getSuspensionState();

    void setSuspensionState(int suspensionState);

    void setTaskDefinitionKey(String taskDefinitionKey);

    Map<String, VariableInstanceEntity> getVariableInstanceEntities();

    void forceUpdate();

    boolean isDeleted();

    void setDeleted(boolean isDeleted);

    boolean isCanceled();

    void setCanceled(boolean isCanceled);

    Date getClaimTime();

    void setClaimTime(Date claimTime);
}
