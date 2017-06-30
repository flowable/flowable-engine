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
package org.flowable.engine.impl.persistence.entity;

import java.util.Date;

import org.flowable.engine.runtime.Job;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface AbstractRuntimeJobEntity extends Job, AbstractJobEntity {

    void setExecution(ExecutionEntity execution);

    void setExecutionId(String executionId);

    void setProcessInstanceId(String processInstanceId);

    void setProcessDefinitionId(String processDefinitionId);
    
    void setDuedate(Date duedate);
    
    void setExclusive(boolean isExclusive);
    
    String getRepeat();

    void setRepeat(String repeat);

    Date getEndDate();

    void setEndDate(Date endDate);
    
    int getMaxIterations();

    void setMaxIterations(int maxIterations);
    
    void setJobType(String jobType);
    
    void setCreateTime(Date createTime);

}
