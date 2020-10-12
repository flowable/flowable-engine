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

package org.flowable.engine.impl;

import java.util.List;

import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricDetailQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.history.NativeHistoricActivityInstanceQuery;
import org.flowable.engine.history.NativeHistoricDetailQuery;
import org.flowable.engine.history.NativeHistoricProcessInstanceQuery;
import org.flowable.engine.history.ProcessInstanceHistoryLogQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeleteHistoricProcessInstanceCmd;
import org.flowable.engine.impl.cmd.DeleteHistoricTaskInstanceCmd;
import org.flowable.engine.impl.cmd.DeleteHistoricTaskLogEntryByLogNumberCmd;
import org.flowable.engine.impl.cmd.DeleteRelatedDataOfRemovedHistoricProcessInstancesCmd;
import org.flowable.engine.impl.cmd.DeleteTaskAndActivityDataOfRemovedHistoricProcessInstancesCmd;
import org.flowable.engine.impl.cmd.GetHistoricEntityLinkChildrenForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetHistoricEntityLinkChildrenForTaskCmd;
import org.flowable.engine.impl.cmd.GetHistoricEntityLinkChildrenWithSameRootAsProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetHistoricEntityLinkParentsForProcessInstanceCmd;
import org.flowable.engine.impl.cmd.GetHistoricEntityLinkParentsForTaskCmd;
import org.flowable.engine.impl.cmd.GetHistoricIdentityLinksForTaskCmd;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.task.api.history.NativeHistoricTaskLogEntryQuery;
import org.flowable.task.service.history.NativeHistoricTaskInstanceQuery;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.HistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.NativeHistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.NativeHistoricTaskLogEntryQueryImpl;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.flowable.variable.api.history.NativeHistoricVariableInstanceQuery;
import org.flowable.variable.service.impl.HistoricVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.NativeHistoricVariableInstanceQueryImpl;

/**
 * @author Tom Baeyens
 * @author Bernd Ruecker (camunda)
 * @author Christian Stettler
 */
public class HistoryServiceImpl extends CommonEngineServiceImpl<ProcessEngineConfigurationImpl> implements HistoryService {

    public HistoryServiceImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public HistoricProcessInstanceQuery createHistoricProcessInstanceQuery() {
        return new HistoricProcessInstanceQueryImpl(commandExecutor, configuration);
    }

    @Override
    public HistoricActivityInstanceQuery createHistoricActivityInstanceQuery() {
        return new HistoricActivityInstanceQueryImpl(commandExecutor);
    }

    @Override
    public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery() {
        return new HistoricTaskInstanceQueryImpl(commandExecutor, configuration.getDatabaseType(),
                configuration.getTaskServiceConfiguration(), configuration.getVariableServiceConfiguration());
    }

    @Override
    public HistoricDetailQuery createHistoricDetailQuery() {
        return new HistoricDetailQueryImpl(commandExecutor);
    }

    @Override
    public NativeHistoricDetailQuery createNativeHistoricDetailQuery() {
        return new NativeHistoricDetailQueryImpl(commandExecutor);
    }

    @Override
    public HistoricVariableInstanceQuery createHistoricVariableInstanceQuery() {
        return new HistoricVariableInstanceQueryImpl(commandExecutor, configuration.getVariableServiceConfiguration());
    }

    @Override
    public NativeHistoricVariableInstanceQuery createNativeHistoricVariableInstanceQuery() {
        return new NativeHistoricVariableInstanceQueryImpl(commandExecutor, configuration.getVariableServiceConfiguration());
    }

    @Override
    public void deleteHistoricTaskInstance(String taskId) {
        commandExecutor.execute(new DeleteHistoricTaskInstanceCmd(taskId));
    }

    @Override
    public void deleteHistoricProcessInstance(String processInstanceId) {
        commandExecutor.execute(new DeleteHistoricProcessInstanceCmd(processInstanceId));
    }

    @Override
    public void deleteTaskAndActivityDataOfRemovedHistoricProcessInstances() {
        commandExecutor.execute(new DeleteTaskAndActivityDataOfRemovedHistoricProcessInstancesCmd());
    }

    @Override
    public void deleteRelatedDataOfRemovedHistoricProcessInstances() {
        commandExecutor.execute(new DeleteRelatedDataOfRemovedHistoricProcessInstancesCmd());
    }

    @Override
    public NativeHistoricProcessInstanceQuery createNativeHistoricProcessInstanceQuery() {
        return new NativeHistoricProcessInstanceQueryImpl(commandExecutor);
    }

    @Override
    public NativeHistoricTaskInstanceQuery createNativeHistoricTaskInstanceQuery() {
        return new NativeHistoricTaskInstanceQueryImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

    @Override
    public NativeHistoricActivityInstanceQuery createNativeHistoricActivityInstanceQuery() {
        return new NativeHistoricActivityInstanceQueryImpl(commandExecutor);
    }

    @Override
    public List<HistoricIdentityLink> getHistoricIdentityLinksForProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetHistoricIdentityLinksForTaskCmd(null, processInstanceId));
    }

    @Override
    public List<HistoricIdentityLink> getHistoricIdentityLinksForTask(String taskId) {
        return commandExecutor.execute(new GetHistoricIdentityLinksForTaskCmd(taskId, null));
    }
    
    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkChildrenForProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkChildrenForProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkChildrenWithSameRootAsProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkChildrenWithSameRootAsProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkChildrenForTask(String taskId) {
        return commandExecutor.execute(new GetHistoricEntityLinkChildrenForTaskCmd(taskId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkParentsForProcessInstance(String processInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkParentsForProcessInstanceCmd(processInstanceId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkParentsForTask(String taskId) {
        return commandExecutor.execute(new GetHistoricEntityLinkParentsForTaskCmd(taskId));
    }

    @Override
    public ProcessInstanceHistoryLogQuery createProcessInstanceHistoryLogQuery(String processInstanceId) {
        return new ProcessInstanceHistoryLogQueryImpl(commandExecutor, processInstanceId, configuration);
    }

    @Override
    public void deleteHistoricTaskLogEntry(long logNumber) {
        commandExecutor.execute(new DeleteHistoricTaskLogEntryByLogNumberCmd(logNumber));
    }

    @Override
    public HistoricTaskLogEntryBuilder createHistoricTaskLogEntryBuilder(TaskInfo task) {
        return new HistoricTaskLogEntryBuilderImpl(commandExecutor, task, configuration.getTaskServiceConfiguration());
    }

    @Override
    public HistoricTaskLogEntryBuilder createHistoricTaskLogEntryBuilder() {
        return new HistoricTaskLogEntryBuilderImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

    @Override
    public HistoricTaskLogEntryQuery createHistoricTaskLogEntryQuery() {
        return new HistoricTaskLogEntryQueryImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

    @Override
    public NativeHistoricTaskLogEntryQuery createNativeHistoricTaskLogEntryQuery() {
        return new NativeHistoricTaskLogEntryQueryImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

}
