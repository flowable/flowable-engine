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

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.task.api.TaskLogEntryQuery;

/**
 * Contains the possible properties that can be used in a {@link TaskLogEntryQuery}.
 * 
 * @author martin.grofcik
 */
public class TaskLogEntryQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, TaskLogEntryQueryProperty> properties = new HashMap<>();

    public static final TaskLogEntryQueryProperty LOG_NUMBER = new TaskLogEntryQueryProperty("RES.ID_");
    public static final TaskLogEntryQueryProperty TYPE = new TaskLogEntryQueryProperty("RES.TYPE_");
    public static final TaskLogEntryQueryProperty TASK_ID_ = new TaskLogEntryQueryProperty("RES.TASK_ID_");
    public static final TaskLogEntryQueryProperty TIME_STAMP = new TaskLogEntryQueryProperty("RES.TIME_STAMP_");
    public static final TaskLogEntryQueryProperty USER_ID = new TaskLogEntryQueryProperty("RES.USER_ID");
    public static final TaskLogEntryQueryProperty DATA = new TaskLogEntryQueryProperty("RES.DATA_");
    public static final TaskLogEntryQueryProperty EXECUTION_ID = new TaskLogEntryQueryProperty("RES.EXECUTION_ID_");
    public static final TaskLogEntryQueryProperty PROCESS_INSTANCE_ID = new TaskLogEntryQueryProperty("RES.PROC_INST_ID_");
    public static final TaskLogEntryQueryProperty PROCESS_DEFINITION_ID = new TaskLogEntryQueryProperty("RES.PROC_DEF_ID_");
    public static final TaskLogEntryQueryProperty SCOPE_ID = new TaskLogEntryQueryProperty("RES.SCOPE_ID_");
    public static final TaskLogEntryQueryProperty SCOPE_DEFINITION_ID = new TaskLogEntryQueryProperty("RES.SCOPE_DEFINITION_ID_");
    public static final TaskLogEntryQueryProperty SUB_SCOPE_ID = new TaskLogEntryQueryProperty("RES.SUB_SCOPE_ID_");
    public static final TaskLogEntryQueryProperty SCOPE_TYPE = new TaskLogEntryQueryProperty("RES.SCOPE_TYPE_");
    public static final TaskLogEntryQueryProperty TENANT_ID = new TaskLogEntryQueryProperty("RES.TENANT_ID_");

    private String name;

    public TaskLogEntryQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static TaskLogEntryQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
