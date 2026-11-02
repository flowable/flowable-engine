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

package org.flowable.app.api.repository;

import org.flowable.app.api.AppRepositoryService;

/**
 * Allows programmatic querying of {@link AppDeployment}s.
 * 
 * Note that it is impossible to retrieve the deployment resources through the results of this operation, 
 * since that would cause a huge transfer of (possibly) unneeded bytes over the wire.
 * 
 * To retrieve the actual bytes of a deployment resource use the operations on the 
 * {@link AppRepositoryService#getDeploymentResourceNames(String)} and
 * {@link AppRepositoryService#getResourceAsStream(String, String)}
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface AppDeploymentQuery extends AppDeploymentBaseQuery<AppDeploymentQuery, AppDeployment> {
    // Add new methods to the AppDefinitionBaseQuery and not here
}
