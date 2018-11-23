/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine;

import java.util.List;

import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricDetailQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.history.NativeHistoricActivityInstanceQuery;
import org.flowable.engine.history.NativeHistoricDetailQuery;
import org.flowable.engine.history.NativeHistoricProcessInstanceQuery;
import org.flowable.engine.history.ProcessInstanceHistoryLog;
import org.flowable.engine.history.ProcessInstanceHistoryLogQuery;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.history.NativeHistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.flowable.variable.api.history.NativeHistoricVariableInstanceQuery;

/**
 * Service exposing information about ongoing and past process instances. This is different from the runtime information in the sense that this runtime information only contains the actual runtime
 * state at any given moment and it is optimized for runtime process execution performance. The history information is optimized for easy querying and remains permanent in the persistent storage.
 * 
 * @author Christian Stettler
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface HistoryService {

    /**
     * Creates a new programmatic query to search for {@link HistoricProcessInstance}s.
     */
    HistoricProcessInstanceQuery createHistoricProcessInstanceQuery();

    /**
     * Creates a new programmatic query to search for {@link HistoricActivityInstance}s.
     */
    HistoricActivityInstanceQuery createHistoricActivityInstanceQuery();

    /**
     * Creates a new programmatic query to search for {@link HistoricTaskInstance}s.
     */
    HistoricTaskInstanceQuery createHistoricTaskInstanceQuery();

    /** Creates a new programmatic query to search for {@link HistoricDetail}s. */
    HistoricDetailQuery createHistoricDetailQuery();

    /**
     * Returns a new {@link org.flowable.common.engine.api.query.NativeQuery} for process definitions.
     */
    NativeHistoricDetailQuery createNativeHistoricDetailQuery();

    /**
     * Creates a new programmatic query to search for {@link HistoricVariableInstance}s.
     */
    HistoricVariableInstanceQuery createHistoricVariableInstanceQuery();

    /**
     * Returns a new {@link org.flowable.common.engine.api.query.NativeQuery} for process definitions.
     */
    NativeHistoricVariableInstanceQuery createNativeHistoricVariableInstanceQuery();

    /**
     * Deletes historic task instance. This might be useful for tasks that are {@link TaskService#newTask() dynamically created} and then {@link TaskService#complete(String) completed}. If the
     * historic task instance doesn't exist, no exception is thrown and the method returns normal.
     */
    void deleteHistoricTaskInstance(String taskId);

    /**
     * Deletes historic process instance. All historic activities, historic task and historic details (variable updates, form properties) are deleted as well.
     */
    void deleteHistoricProcessInstance(String processInstanceId);

    /**
     * creates a native query to search for {@link HistoricProcessInstance}s via SQL
     */
    NativeHistoricProcessInstanceQuery createNativeHistoricProcessInstanceQuery();

    /**
     * creates a native query to search for {@link HistoricTaskInstance}s via SQL
     */
    NativeHistoricTaskInstanceQuery createNativeHistoricTaskInstanceQuery();

    /**
     * creates a native query to search for {@link HistoricActivityInstance}s via SQL
     */
    NativeHistoricActivityInstanceQuery createNativeHistoricActivityInstanceQuery();

    /**
     * Retrieves the {@link HistoricIdentityLink}s associated with the given task. Such an {@link IdentityLink} informs how a certain identity (eg. group or user) is associated with a certain task
     * (eg. as candidate, assignee, etc.), even if the task is completed as opposed to {@link IdentityLink}s which only exist for active tasks.
     */
    List<HistoricIdentityLink> getHistoricIdentityLinksForTask(String taskId);

    /**
     * Retrieves the {@link HistoricIdentityLink}s associated with the given process instance. Such an {@link IdentityLink} informs how a certain identity (eg. group or user) is associated with a
     * certain process instance, even if the instance is completed as opposed to {@link IdentityLink}s which only exist for active instances.
     */
    List<HistoricIdentityLink> getHistoricIdentityLinksForProcessInstance(String processInstanceId);
    
    /**
     * Retrieves the {@link HistoricEntityLink}s associated with the given process instance.
     */
    List<HistoricEntityLink> getHistoricEntityLinkChildrenForProcessInstance(String processInstanceId);

    /**
     * Retrieves the {@link HistoricEntityLink}s associated with the given task.
     */
    List<HistoricEntityLink> getHistoricEntityLinkChildrenForTask(String taskId);

    /**
     * Retrieves the {@link HistoricEntityLink}s where the given process instance is referenced.
     */
    List<HistoricEntityLink> getHistoricEntityLinkParentsForProcessInstance(String processInstanceId);

    /**
     * Retrieves the {@link HistoricEntityLink}s where the given task is referenced.
     */
    List<HistoricEntityLink> getHistoricEntityLinkParentsForTask(String taskId);

    /**
     * Allows to retrieve the {@link ProcessInstanceHistoryLog} for one process instance.
     */
    ProcessInstanceHistoryLogQuery createProcessInstanceHistoryLogQuery(String processInstanceId);

}
