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
package org.flowable.task.service.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractNativeQuery;
import org.flowable.task.api.history.HistoricTaskLogEntry;
import org.flowable.task.api.history.NativeHistoricTaskLogEntryQuery;
import org.flowable.task.service.TaskServiceConfiguration;

/**
 * @author martin.grofcik
 */
public class NativeHistoricTaskLogEntryQueryImpl extends AbstractNativeQuery<NativeHistoricTaskLogEntryQuery, HistoricTaskLogEntry> implements
        NativeHistoricTaskLogEntryQuery {

    private static final long serialVersionUID = 1L;
    
    protected TaskServiceConfiguration taskServiceConfiguration;

    public NativeHistoricTaskLogEntryQueryImpl(CommandContext commandContext, TaskServiceConfiguration taskServiceConfiguration) {
        super(commandContext);
        this.taskServiceConfiguration = taskServiceConfiguration;
    }

    public NativeHistoricTaskLogEntryQueryImpl(CommandExecutor commandExecutor, TaskServiceConfiguration taskServiceConfiguration) {
        super(commandExecutor);
        this.taskServiceConfiguration = taskServiceConfiguration;
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<HistoricTaskLogEntry> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return taskServiceConfiguration.getHistoricTaskLogEntryEntityManager().findHistoricTaskLogEntriesByNativeQueryCriteria(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return taskServiceConfiguration.getHistoricTaskLogEntryEntityManager().findHistoricTaskLogEntriesCountByNativeQueryCriteria(parameterMap);
    }

}
