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
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;

/**
 * Contains the possible properties that can be used in a {@link HistoricTaskLogEntryQuery}.
 * 
 * @author martin.grofcik
 */
public class HistoricTaskLogEntryQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, HistoricTaskLogEntryQueryProperty> properties = new HashMap<>();

    public static final HistoricTaskLogEntryQueryProperty LOG_NUMBER = new HistoricTaskLogEntryQueryProperty("RES.ID_");
    public static final HistoricTaskLogEntryQueryProperty TYPE = new HistoricTaskLogEntryQueryProperty("RES.TYPE_");
    public static final HistoricTaskLogEntryQueryProperty TASK_ID_ = new HistoricTaskLogEntryQueryProperty("RES.TASK_ID_");
    public static final HistoricTaskLogEntryQueryProperty TIME_STAMP = new HistoricTaskLogEntryQueryProperty("RES.TIME_STAMP_");
    public static final HistoricTaskLogEntryQueryProperty USER_ID = new HistoricTaskLogEntryQueryProperty("RES.USER_ID");
    public static final HistoricTaskLogEntryQueryProperty DATA = new HistoricTaskLogEntryQueryProperty("RES.DATA_");
    public static final HistoricTaskLogEntryQueryProperty EXECUTION_ID = new HistoricTaskLogEntryQueryProperty("RES.EXECUTION_ID_");
    public static final HistoricTaskLogEntryQueryProperty PROCESS_INSTANCE_ID = new HistoricTaskLogEntryQueryProperty("RES.PROC_INST_ID_");
    public static final HistoricTaskLogEntryQueryProperty PROCESS_DEFINITION_ID = new HistoricTaskLogEntryQueryProperty("RES.PROC_DEF_ID_");
    public static final HistoricTaskLogEntryQueryProperty SCOPE_ID = new HistoricTaskLogEntryQueryProperty("RES.SCOPE_ID_");
    public static final HistoricTaskLogEntryQueryProperty SCOPE_DEFINITION_ID = new HistoricTaskLogEntryQueryProperty("RES.SCOPE_DEFINITION_ID_");
    public static final HistoricTaskLogEntryQueryProperty SUB_SCOPE_ID = new HistoricTaskLogEntryQueryProperty("RES.SUB_SCOPE_ID_");
    public static final HistoricTaskLogEntryQueryProperty SCOPE_TYPE = new HistoricTaskLogEntryQueryProperty("RES.SCOPE_TYPE_");
    public static final HistoricTaskLogEntryQueryProperty TENANT_ID = new HistoricTaskLogEntryQueryProperty("RES.TENANT_ID_");

    private String name;

    public HistoricTaskLogEntryQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static HistoricTaskLogEntryQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
