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
package org.flowable.eventregistry.impl.persistence.entity.data;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.impl.EventDeploymentQueryImpl;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;

/**
 * @author Joram Barrez
 */
public interface EventDeploymentDataManager extends DataManager<EventDeploymentEntity> {

    long findDeploymentCountByQueryCriteria(EventDeploymentQueryImpl deploymentQuery);

    List<EventDeployment> findDeploymentsByQueryCriteria(EventDeploymentQueryImpl deploymentQuery);

    List<String> getDeploymentResourceNames(String deploymentId);

    List<EventDeployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap);

    long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap);

}
