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
package org.flowable.task.service;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.task.api.TaskQueryInterceptor;
import org.flowable.task.api.history.HistoricTaskQueryInterceptor;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.HistoricTaskServiceImpl;
import org.flowable.task.service.impl.TaskServiceImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManagerImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityManagerImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManagerImpl;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskInstanceDataManager;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskLogEntryDataManager;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;
import org.flowable.task.service.impl.persistence.entity.data.impl.MyBatisHistoricTaskLogEntryDataManager;
import org.flowable.task.service.impl.persistence.entity.data.impl.MybatisHistoricTaskInstanceDataManager;
import org.flowable.task.service.impl.persistence.entity.data.impl.MybatisTaskDataManager;

public class TaskServiceConfiguration extends AbstractServiceConfiguration<TaskServiceConfiguration> {

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/task/service/db/mapping/mappings.xml";

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected TaskService taskService = new TaskServiceImpl(this);
    protected HistoricTaskService historicTaskService = new HistoricTaskServiceImpl(this);

    protected IdmIdentityService idmIdentityService;

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected TaskDataManager taskDataManager;
    protected HistoricTaskInstanceDataManager historicTaskInstanceDataManager;
    protected HistoricTaskLogEntryDataManager historicTaskLogDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected TaskEntityManager taskEntityManager;
    protected HistoricTaskInstanceEntityManager historicTaskInstanceEntityManager;
    protected HistoricTaskLogEntryEntityManager historicTaskLogEntryEntityManager;

    protected InternalTaskVariableScopeResolver internalTaskVariableScopeResolver;
    protected InternalHistoryTaskManager internalHistoryTaskManager;
    protected InternalTaskLocalizationManager internalTaskLocalizationManager;
    protected InternalTaskAssignmentManager internalTaskAssignmentManager;

    protected boolean enableTaskRelationshipCounts;
    protected boolean enableLocalization;

    protected TaskQueryInterceptor taskQueryInterceptor;
    protected HistoricTaskQueryInterceptor historicTaskQueryInterceptor;

    protected TaskPostProcessor taskPostProcessor;

    // Events
    protected boolean enableHistoricTaskLogging;

    public TaskServiceConfiguration(String engineName) {
        super(engineName);
    }

    @Override
    protected TaskServiceConfiguration getService() {
        return this;
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        configuratorsBeforeInit();

        initDataManagers();
        initEntityManagers();
        initTaskPostProcessor();

        configuratorsAfterInit();
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (taskDataManager == null) {
            taskDataManager = new MybatisTaskDataManager(this);
        }
        if (historicTaskInstanceDataManager == null) {
            historicTaskInstanceDataManager = new MybatisHistoricTaskInstanceDataManager(this);
        }
        if (historicTaskLogDataManager == null) {
            historicTaskLogDataManager = new MyBatisHistoricTaskLogEntryDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (taskEntityManager == null) {
            taskEntityManager = new TaskEntityManagerImpl(this, taskDataManager);
        }
        if (historicTaskInstanceEntityManager == null) {
            historicTaskInstanceEntityManager = new HistoricTaskInstanceEntityManagerImpl(this, historicTaskInstanceDataManager);
        }
        if (historicTaskLogEntryEntityManager == null) {
            historicTaskLogEntryEntityManager = new HistoricTaskLogEntryEntityManagerImpl(this, historicTaskLogDataManager);
        }
    }

    public void initTaskPostProcessor() {
        if (taskPostProcessor == null) {
            taskPostProcessor = taskBuilder -> taskBuilder;
        }
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public TaskServiceConfiguration setTaskService(TaskService taskService) {
        this.taskService = taskService;
        return this;
    }

    public HistoricTaskService getHistoricTaskService() {
        return historicTaskService;
    }

    public TaskServiceConfiguration setHistoricTaskService(HistoricTaskService historicTaskService) {
        this.historicTaskService = historicTaskService;
        return this;
    }

    public IdmIdentityService getIdmIdentityService() {
        return idmIdentityService;
    }

    public void setIdmIdentityService(IdmIdentityService idmIdentityService) {
        this.idmIdentityService = idmIdentityService;
    }

    public TaskServiceConfiguration getTaskServiceConfiguration() {
        return this;
    }

    public TaskDataManager getTaskDataManager() {
        return taskDataManager;
    }

    public TaskServiceConfiguration setTaskDataManager(TaskDataManager taskDataManager) {
        this.taskDataManager = taskDataManager;
        return this;
    }

    public HistoricTaskInstanceDataManager getHistoricTaskInstanceDataManager() {
        return historicTaskInstanceDataManager;
    }

    public TaskServiceConfiguration setHistoricTaskInstanceDataManager(HistoricTaskInstanceDataManager historicTaskInstanceDataManager) {
        this.historicTaskInstanceDataManager = historicTaskInstanceDataManager;
        return this;
    }

    public TaskEntityManager getTaskEntityManager() {
        return taskEntityManager;
    }

    public TaskServiceConfiguration setTaskEntityManager(TaskEntityManager taskEntityManager) {
        this.taskEntityManager = taskEntityManager;
        return this;
    }

    public HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
        return historicTaskInstanceEntityManager;
    }

    public TaskServiceConfiguration setHistoricTaskInstanceEntityManager(HistoricTaskInstanceEntityManager historicTaskInstanceEntityManager) {
        this.historicTaskInstanceEntityManager = historicTaskInstanceEntityManager;
        return this;
    }

    public HistoricTaskLogEntryEntityManager getHistoricTaskLogEntryEntityManager() {
        return historicTaskLogEntryEntityManager;
    }

    public TaskServiceConfiguration setHistoricTaskLogEntryEntityManager(HistoricTaskLogEntryEntityManager historicTaskLogEntryEntityManager) {
        this.historicTaskLogEntryEntityManager = historicTaskLogEntryEntityManager;
        return this;
    }

    public InternalTaskVariableScopeResolver getInternalTaskVariableScopeResolver() {
        return internalTaskVariableScopeResolver;
    }

    public void setInternalTaskVariableScopeResolver(InternalTaskVariableScopeResolver internalTaskVariableScopeResolver) {
        this.internalTaskVariableScopeResolver = internalTaskVariableScopeResolver;
    }

    public InternalHistoryTaskManager getInternalHistoryTaskManager() {
        return internalHistoryTaskManager;
    }

    public void setInternalHistoryTaskManager(InternalHistoryTaskManager internalHistoryTaskManager) {
        this.internalHistoryTaskManager = internalHistoryTaskManager;
    }

    public InternalTaskLocalizationManager getInternalTaskLocalizationManager() {
        return internalTaskLocalizationManager;
    }

    public void setInternalTaskLocalizationManager(InternalTaskLocalizationManager internalTaskLocalizationManager) {
        this.internalTaskLocalizationManager = internalTaskLocalizationManager;
    }

    public InternalTaskAssignmentManager getInternalTaskAssignmentManager() {
        return internalTaskAssignmentManager;
    }

    public void setInternalTaskAssignmentManager(InternalTaskAssignmentManager internalTaskAssignmentManager) {
        this.internalTaskAssignmentManager = internalTaskAssignmentManager;
    }

    public boolean isEnableTaskRelationshipCounts() {
        return enableTaskRelationshipCounts;
    }

    public TaskServiceConfiguration setEnableTaskRelationshipCounts(boolean enableTaskRelationshipCounts) {
        this.enableTaskRelationshipCounts = enableTaskRelationshipCounts;
        return this;
    }

    public boolean isEnableLocalization() {
        return enableLocalization;
    }

    public TaskServiceConfiguration setEnableLocalization(boolean enableLocalization) {
        this.enableLocalization = enableLocalization;
        return this;
    }

    public TaskQueryInterceptor getTaskQueryInterceptor() {
        return taskQueryInterceptor;
    }

    public TaskServiceConfiguration setTaskQueryInterceptor(TaskQueryInterceptor taskQueryInterceptor) {
        this.taskQueryInterceptor = taskQueryInterceptor;
        return this;
    }

    public HistoricTaskQueryInterceptor getHistoricTaskQueryInterceptor() {
        return historicTaskQueryInterceptor;
    }

    public TaskServiceConfiguration setHistoricTaskQueryInterceptor(HistoricTaskQueryInterceptor historicTaskQueryInterceptor) {
        this.historicTaskQueryInterceptor = historicTaskQueryInterceptor;
        return this;
    }

    public boolean isEnableHistoricTaskLogging() {
        return enableHistoricTaskLogging;
    }

    public TaskServiceConfiguration setEnableHistoricTaskLogging(boolean enableHistoricTaskLogging) {
        this.enableHistoricTaskLogging = enableHistoricTaskLogging;
        return this;
    }

    @Override
    public TaskServiceConfiguration setEnableEventDispatcher(boolean enableEventDispatcher) {
        this.enableEventDispatcher = enableEventDispatcher;
        return this;
    }

    @Override
    public TaskServiceConfiguration setEventDispatcher(FlowableEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        return this;
    }

    @Override
    public TaskServiceConfiguration setEventListeners(List<FlowableEventListener> eventListeners) {
        this.eventListeners = eventListeners;
        return this;
    }

    @Override
    public TaskServiceConfiguration setTypedEventListeners(Map<String, List<FlowableEventListener>> typedEventListeners) {
        this.typedEventListeners = typedEventListeners;
        return this;
    }

    public TaskPostProcessor getTaskPostProcessor() {
        return taskPostProcessor;
    }

    public TaskServiceConfiguration setTaskPostProcessor(TaskPostProcessor processor) {
        this.taskPostProcessor = processor;
        return this;
    }
}
