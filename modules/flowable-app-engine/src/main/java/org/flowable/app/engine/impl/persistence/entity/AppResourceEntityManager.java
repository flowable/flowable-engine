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
package org.flowable.app.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;

/**
 * @author Joram Barrez
 */
public interface AppResourceEntityManager extends EntityManager<AppResourceEntity> {

    List<AppResourceEntity> findResourcesByDeploymentId(String deploymentId);

    AppResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName);

    void deleteResourcesByDeploymentId(String deploymentId);

}