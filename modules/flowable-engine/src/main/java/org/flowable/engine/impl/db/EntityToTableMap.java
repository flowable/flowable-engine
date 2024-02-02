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
package org.flowable.engine.impl.db;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricFormProperty;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.AttachmentEntity;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailAssignmentEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.persistence.entity.ModelEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class EntityToTableMap {

    public static Map<Class<?>, String> apiTypeToTableNameMap = new HashMap<>();
    public static Map<Class<? extends Entity>, String> entityToTableNameMap = new HashMap<>();

    static {
        // runtime
        entityToTableNameMap.put(TaskEntity.class, "ACT_RU_TASK");
        entityToTableNameMap.put(ExecutionEntity.class, "ACT_RU_EXECUTION");
        entityToTableNameMap.put(IdentityLinkEntity.class, "ACT_RU_IDENTITYLINK");
        entityToTableNameMap.put(VariableInstanceEntity.class, "ACT_RU_VARIABLE");

        entityToTableNameMap.put(JobEntity.class, "ACT_RU_JOB");
        entityToTableNameMap.put(TimerJobEntity.class, "ACT_RU_TIMER_JOB");
        entityToTableNameMap.put(SuspendedJobEntity.class, "ACT_RU_SUSPENDED_JOB");
        entityToTableNameMap.put(DeadLetterJobEntity.class, "ACT_RU_DEADLETTER_JOB");

        entityToTableNameMap.put(EventSubscriptionEntity.class, "ACT_RU_EVENT_SUBSCR");
        entityToTableNameMap.put(CompensateEventSubscriptionEntity.class, "ACT_RU_EVENT_SUBSCR");
        entityToTableNameMap.put(MessageEventSubscriptionEntity.class, "ACT_RU_EVENT_SUBSCR");
        entityToTableNameMap.put(SignalEventSubscriptionEntity.class, "ACT_RU_EVENT_SUBSCR");
        entityToTableNameMap.put(ActivityInstanceEntity.class, "ACT_RU_ACTINST");

        // repository
        entityToTableNameMap.put(DeploymentEntity.class, "ACT_RE_DEPLOYMENT");
        entityToTableNameMap.put(ProcessDefinitionEntity.class, "ACT_RE_PROCDEF");
        entityToTableNameMap.put(ModelEntity.class, "ACT_RE_MODEL");
        entityToTableNameMap.put(ProcessDefinitionInfoEntity.class, "ACT_PROCDEF_INFO");

        // history
        entityToTableNameMap.put(CommentEntity.class, "ACT_HI_COMMENT");

        entityToTableNameMap.put(HistoricActivityInstanceEntity.class, "ACT_HI_ACTINST");
        entityToTableNameMap.put(AttachmentEntity.class, "ACT_HI_ATTACHMENT");
        entityToTableNameMap.put(HistoricProcessInstanceEntity.class, "ACT_HI_PROCINST");
        entityToTableNameMap.put(HistoricVariableInstanceEntity.class, "ACT_HI_VARINST");
        entityToTableNameMap.put(HistoricTaskInstanceEntity.class, "ACT_HI_TASKINST");
        entityToTableNameMap.put(HistoricTaskLogEntryEntity.class, "ACT_HI_TSK_LOG");
        entityToTableNameMap.put(HistoricIdentityLinkEntity.class, "ACT_HI_IDENTITYLINK");

        // a couple of stuff goes to the same table
        entityToTableNameMap.put(HistoricDetailAssignmentEntity.class, "ACT_HI_DETAIL");
        entityToTableNameMap.put(HistoricFormPropertyEntity.class, "ACT_HI_DETAIL");
        entityToTableNameMap.put(HistoricDetailVariableInstanceUpdateEntity.class, "ACT_HI_DETAIL");
        entityToTableNameMap.put(HistoricDetailEntity.class, "ACT_HI_DETAIL");

        // general
        entityToTableNameMap.put(PropertyEntity.class, "ACT_GE_PROPERTY");
        entityToTableNameMap.put(ByteArrayEntity.class, "ACT_GE_BYTEARRAY");
        entityToTableNameMap.put(ResourceEntity.class, "ACT_GE_BYTEARRAY");

        entityToTableNameMap.put(EventLogEntryEntity.class, "ACT_EVT_LOG");

        // and now the map for the API types (does not cover all cases)
        apiTypeToTableNameMap.put(Task.class, "ACT_RU_TASK");
        apiTypeToTableNameMap.put(Execution.class, "ACT_RU_EXECUTION");
        apiTypeToTableNameMap.put(ProcessInstance.class, "ACT_RU_EXECUTION");
        apiTypeToTableNameMap.put(ProcessDefinition.class, "ACT_RE_PROCDEF");
        apiTypeToTableNameMap.put(Deployment.class, "ACT_RE_DEPLOYMENT");
        apiTypeToTableNameMap.put(Job.class, "ACT_RU_JOB");
        apiTypeToTableNameMap.put(Model.class, "ACT_RE_MODEL");

        // history
        apiTypeToTableNameMap.put(HistoricProcessInstance.class, "ACT_HI_PROCINST");
        apiTypeToTableNameMap.put(HistoricActivityInstance.class, "ACT_HI_ACTINST");
        apiTypeToTableNameMap.put(HistoricDetail.class, "ACT_HI_DETAIL");
        apiTypeToTableNameMap.put(HistoricVariableUpdate.class, "ACT_HI_DETAIL");
        apiTypeToTableNameMap.put(HistoricFormProperty.class, "ACT_HI_DETAIL");
        apiTypeToTableNameMap.put(HistoricTaskInstance.class, "ACT_HI_TASKINST");
        apiTypeToTableNameMap.put(HistoricTaskLogEntry.class, "ACT_HI_TSK_LOG");
        apiTypeToTableNameMap.put(HistoricVariableInstance.class, "ACT_HI_VARINST");

        // TODO: Identity skipped for the moment as no SQL injection is provided
        // here
    }

    public static String getTableName(Class<?> entityClass) {
        if (Entity.class.isAssignableFrom(entityClass)) {
            return entityToTableNameMap.get(entityClass);
        } else {
            return apiTypeToTableNameMap.get(entityClass);
        }
    }

}
