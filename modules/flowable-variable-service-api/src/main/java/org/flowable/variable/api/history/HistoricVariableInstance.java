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

package org.flowable.variable.api.history;

import java.util.Date;

import org.flowable.common.engine.api.history.HistoricData;

/**
 * A single process variable containing the last value when its process instance has finished.
 * 
 * @author Christian Lipphardt (camunda)
 * @author ruecker
 * @author Joram Barrez
 */
public interface HistoricVariableInstance extends HistoricData {

    /** The unique DB id */
    String getId();

    String getVariableName();

    String getVariableTypeName();

    Object getValue();

    /** The process instance reference. */
    String getProcessInstanceId();

    /**
     * @return the task id of the task, in case this variable instance has been set locally on a task. Returns null, if this variable is not related to a task.
     */
    String getTaskId();

    /**
     * Returns the time when the variable was created.
     */
    Date getCreateTime();

    /**
     * Returns the time when the value of the variable was last updated. Note that a {@link HistoricVariableInstance} only contains the latest value of the variable.
     */
    Date getLastUpdatedTime();
    
    String getScopeId();
    
    String getSubScopeId();
    
    String getScopeType();
}
