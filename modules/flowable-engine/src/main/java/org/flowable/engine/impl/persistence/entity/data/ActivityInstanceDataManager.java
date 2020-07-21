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
package org.flowable.engine.impl.persistence.entity.data;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.runtime.ActivityInstance;

/**
 * @author martin.grofcik
 */
public interface ActivityInstanceDataManager extends DataManager<ActivityInstanceEntity> {

    List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId);
    
    List<ActivityInstanceEntity> findActivityInstancesByExecutionIdAndActivityId(String executionId, String activityId);

    ActivityInstanceEntity findActivityInstanceByTaskId(String taskId);
    
    List<ActivityInstanceEntity> findActivityInstancesByProcessInstanceId(String processInstanceId, boolean includeDeleted);

    void deleteActivityInstancesByProcessInstanceId(String processInstanceId);

    long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery);

    List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery);

    List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap);

    long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap);
}
