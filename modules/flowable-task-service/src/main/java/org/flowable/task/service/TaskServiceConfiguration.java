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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.HistoricTaskServiceImpl;
import org.flowable.task.service.impl.TaskServiceImpl;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManagerImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManagerImpl;
import org.flowable.task.service.impl.persistence.entity.data.HistoricTaskInstanceDataManager;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;
import org.flowable.task.service.impl.persistence.entity.data.impl.MybatisHistoricTaskInstanceDataManager;
import org.flowable.task.service.impl.persistence.entity.data.impl.MybatisTaskDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskServiceConfiguration extends AbstractServiceConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TaskServiceConfiguration.class);

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/task/service/db/mapping/mappings.xml";

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected TaskService taskService = new TaskServiceImpl(this);
    protected HistoricTaskService historicTaskService = new HistoricTaskServiceImpl(this);
    
    protected IdmIdentityService idmIdentityService;

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected TaskDataManager taskDataManager;
    protected HistoricTaskInstanceDataManager historicTaskInstanceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    protected TaskEntityManager taskEntityManager;
    protected HistoricTaskInstanceEntityManager historicTaskInstanceEntityManager;
    
    protected InternalTaskVariableScopeResolver internalTaskVariableScopeResolver;
    protected InternalHistoryTaskManager internalHistoryTaskManager;
    protected InternalTaskLocalizationManager internalTaskLocalizationManager;
    protected InternalTaskAssignmentManager internalTaskAssignmentManager;
    
    protected boolean enableTaskRelationshipCounts;
    protected boolean enableLocalization;
    
    protected int taskQueryLimit;
    protected int historicTaskQueryLimit;
    
    protected IdGenerator idGenerator;

    protected TaskPostProcessor taskPostProcessor;

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        checkIdGenerator();
        initDataManagers();
        initEntityManagers();
        initTaskPostProcessor();
    }

    protected void checkIdGenerator() {
        if (this.idGenerator == null) {
            throw new FlowableException("Id generator for task configuration must be initialized");
        }
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (taskDataManager == null) {
            taskDataManager = new MybatisTaskDataManager();
        }
        if (historicTaskInstanceDataManager == null) {
            historicTaskInstanceDataManager = new MybatisHistoricTaskInstanceDataManager();
        }
    }

    public void initEntityManagers() {
        if (taskEntityManager == null) {
            taskEntityManager = new TaskEntityManagerImpl(this, taskDataManager);
        }
        if (historicTaskInstanceEntityManager == null) {
            historicTaskInstanceEntityManager = new HistoricTaskInstanceEntityManagerImpl(this, historicTaskInstanceDataManager);
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

    public int getTaskQueryLimit() {
        return taskQueryLimit;
    }

    public TaskServiceConfiguration setTaskQueryLimit(int taskQueryLimit) {
        this.taskQueryLimit = taskQueryLimit;
        return this;
    }

    public int getHistoricTaskQueryLimit() {
        return historicTaskQueryLimit;
    }

    public TaskServiceConfiguration setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
        this.historicTaskQueryLimit = historicTaskQueryLimit;
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

    public TaskServiceConfiguration setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        return this;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public TaskPostProcessor getTaskPostProcessor() {
        return taskPostProcessor;
    }

    public TaskServiceConfiguration setTaskPostProcessor(TaskPostProcessor processor) {
        this.taskPostProcessor = processor;
        return this;
    }
}
