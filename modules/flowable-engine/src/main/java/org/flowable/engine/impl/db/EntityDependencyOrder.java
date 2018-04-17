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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityImpl;
import org.flowable.engine.impl.persistence.entity.ByteArrayEntityImpl;
import org.flowable.engine.impl.persistence.entity.CommentEntityImpl;
import org.flowable.engine.impl.persistence.entity.CompensateEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityImpl;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailAssignmentEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricFormPropertyEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricScopeInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ModelEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityImpl;
import org.flowable.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.engine.impl.persistence.entity.ResourceEntityImpl;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayEntityImpl;
import org.flowable.job.service.impl.persistence.entity.JobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityImpl;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * Maintains a list of all the entities in order of dependency.
 */
public class EntityDependencyOrder {

    public static List<Class<? extends Entity>> DELETE_ORDER = new ArrayList<>();
    public static List<Class<? extends Entity>> INSERT_ORDER = new ArrayList<>();

    static {

        /*
         * In the comments below:
         * 
         * 'FK to X' : X should be BELOW the entity
         * 
         * 'FK from X': X should be ABOVE the entity
         * 
         */

        /* No FK */
        DELETE_ORDER.add(PropertyEntityImpl.class);

        /* No FK */
        DELETE_ORDER.add(AttachmentEntityImpl.class);

        /* No FK */
        DELETE_ORDER.add(CommentEntityImpl.class);

        /* No FK */
        DELETE_ORDER.add(EventLogEntryEntityImpl.class);

        /*
         * FK to Deployment FK to ByteArray
         */
        DELETE_ORDER.add(ModelEntityImpl.class);

        /*
         * FK to ByteArray
         */
        DELETE_ORDER.add(JobEntityImpl.class);
        DELETE_ORDER.add(TimerJobEntityImpl.class);
        DELETE_ORDER.add(SuspendedJobEntityImpl.class);
        DELETE_ORDER.add(DeadLetterJobEntityImpl.class);
        
        /*
         * FK to ByteArray
         */
        DELETE_ORDER.add(HistoryJobEntityImpl.class);

        /*
         * FK to ByteArray FK to Execution
         */
        DELETE_ORDER.add(VariableInstanceEntityImpl.class);

        /*
         * FK to ByteArray FK to ProcessDefinition
         */
        DELETE_ORDER.add(ProcessDefinitionInfoEntityImpl.class);

        /*
         * FK from ModelEntity FK from JobEntity FK from VariableInstanceEntity
         * 
         * FK to DeploymentEntity
         */
        DELETE_ORDER.add(ByteArrayEntityImpl.class);
        DELETE_ORDER.add(VariableByteArrayEntityImpl.class);
        DELETE_ORDER.add(JobByteArrayEntityImpl.class);

        /*
         * FK from ModelEntity FK from JobEntity FK from VariableInstanceEntity
         * 
         * FK to DeploymentEntity
         */
        DELETE_ORDER.add(ResourceEntityImpl.class);

        /*
         * FK from ByteArray
         */
        DELETE_ORDER.add(DeploymentEntityImpl.class);

        /*
         * FK to Execution
         */
        DELETE_ORDER.add(EventSubscriptionEntityImpl.class);

        /*
         * FK to Execution
         */
        DELETE_ORDER.add(CompensateEventSubscriptionEntityImpl.class);

        /*
         * FK to Execution
         */
        DELETE_ORDER.add(MessageEventSubscriptionEntityImpl.class);

        /*
         * FK to Execution
         */
        DELETE_ORDER.add(SignalEventSubscriptionEntityImpl.class);

        /*
         * FK to process definition FK to Execution FK to Task
         */
        DELETE_ORDER.add(IdentityLinkEntityImpl.class);

        /*
         * FK from IdentityLink
         * 
         * FK to Execution FK to process definition
         */
        DELETE_ORDER.add(TaskEntityImpl.class);

        /*
         * FK from VariableInstance FK from EventSubscription FK from IdentityLink FK from Task
         * 
         * FK to ProcessDefinition
         */
        DELETE_ORDER.add(ExecutionEntityImpl.class);

        /*
         * FK from Task FK from IdentityLink FK from execution
         */
        DELETE_ORDER.add(ProcessDefinitionEntityImpl.class);

        // History entities have no FK's

        DELETE_ORDER.add(HistoricIdentityLinkEntityImpl.class);

        DELETE_ORDER.add(HistoricActivityInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricProcessInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricTaskInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricScopeInstanceEntityImpl.class);

        DELETE_ORDER.add(HistoricVariableInstanceEntityImpl.class);

        DELETE_ORDER.add(HistoricDetailAssignmentEntityImpl.class);
        DELETE_ORDER.add(HistoricDetailVariableInstanceUpdateEntityImpl.class);
        DELETE_ORDER.add(HistoricFormPropertyEntityImpl.class);
        DELETE_ORDER.add(HistoricDetailEntityImpl.class);

        INSERT_ORDER = new ArrayList<>(DELETE_ORDER);
        Collections.reverse(INSERT_ORDER);

    }
    
}
