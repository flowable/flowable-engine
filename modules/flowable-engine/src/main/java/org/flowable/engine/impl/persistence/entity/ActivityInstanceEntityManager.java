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

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.runtime.ActivityInstance;

/**
 * @author martin.grofcik
 */
public interface ActivityInstanceEntityManager extends EntityManager<ActivityInstanceEntity> {

    List<ActivityInstanceEntity> findUnfinishedActivityInstancesByExecutionAndActivityId(String executionId, String activityId);
    
    List<ActivityInstanceEntity> findActivityInstancesByExecutionAndActivityId(String executionId, String activityId);

    List<ActivityInstanceEntity> findUnfinishedActivityInstancesByProcessInstanceId(String processInstanceId);

    long findActivityInstanceCountByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery);

    List<ActivityInstance> findActivityInstancesByQueryCriteria(ActivityInstanceQueryImpl activityInstanceQuery);

    List<ActivityInstance> findActivityInstancesByNativeQuery(Map<String, Object> parameterMap);

    long findActivityInstanceCountByNativeQuery(Map<String, Object> parameterMap);

    void deleteActivityInstancesByProcessInstanceId(String processInstanceId);

    void deleteActivityInstancesByProcessDefinitionId(String processDefinitionId);
}