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
package org.flowable.cmmn.engine.impl.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.batch.service.impl.persistence.entity.BatchEntityImpl;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityImpl;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityImpl;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityImpl;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.GenericEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntityImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * @author Joram Barrez
 */
public class EntityDependencyOrder {

    public static List<Class<? extends Entity>> DELETE_ORDER = new ArrayList<>();
    public static List<Class<? extends Entity>> INSERT_ORDER;

    static {

        DELETE_ORDER.add(PropertyEntityImpl.class);
        DELETE_ORDER.add(BatchPartEntityImpl.class);
        DELETE_ORDER.add(BatchEntityImpl.class);
        DELETE_ORDER.add(JobEntityImpl.class);
        DELETE_ORDER.add(ExternalWorkerJobEntityImpl.class);
        DELETE_ORDER.add(TimerJobEntityImpl.class);
        DELETE_ORDER.add(SuspendedJobEntityImpl.class);
        DELETE_ORDER.add(DeadLetterJobEntityImpl.class);
        DELETE_ORDER.add(HistoricTaskLogEntryEntityImpl.class);
        DELETE_ORDER.add(HistoryJobEntityImpl.class);
        DELETE_ORDER.add(HistoricEntityLinkEntityImpl.class);
        DELETE_ORDER.add(HistoricIdentityLinkEntityImpl.class);
        DELETE_ORDER.add(HistoricMilestoneInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricTaskInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricCaseInstanceEntityImpl.class);
        DELETE_ORDER.add(VariableInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricVariableInstanceEntityImpl.class);
        DELETE_ORDER.add(SignalEventSubscriptionEntityImpl.class);
        DELETE_ORDER.add(MessageEventSubscriptionEntityImpl.class);
        DELETE_ORDER.add(CompensateEventSubscriptionEntityImpl.class);
        DELETE_ORDER.add(GenericEventSubscriptionEntityImpl.class);
        DELETE_ORDER.add(EventSubscriptionEntityImpl.class);
        DELETE_ORDER.add(EntityLinkEntityImpl.class);
        DELETE_ORDER.add(IdentityLinkEntityImpl.class);
        DELETE_ORDER.add(TaskEntityImpl.class);
        DELETE_ORDER.add(MilestoneInstanceEntityImpl.class);
        DELETE_ORDER.add(SentryPartInstanceEntityImpl.class);
        DELETE_ORDER.add(PlanItemInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricPlanItemInstanceEntityImpl.class);
        DELETE_ORDER.add(CaseInstanceEntityImpl.class);
        DELETE_ORDER.add(CaseDefinitionEntityImpl.class);
        DELETE_ORDER.add(ByteArrayEntityImpl.class);
        DELETE_ORDER.add(CmmnResourceEntityImpl.class);
        DELETE_ORDER.add(CmmnDeploymentEntityImpl.class);
        
        INSERT_ORDER = new ArrayList<>(DELETE_ORDER);
        Collections.reverse(INSERT_ORDER);

    }
    
}
