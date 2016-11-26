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

package org.activiti.idm.api;

import java.util.List;

import org.activiti.engine.common.api.query.Query;

/**
 * Allows programmatic querying of {@link Capability}
 * 
 * @author Joram Barrez
 */
public interface CapabilityQuery extends Query<CapabilityQuery, Capability> {
  
  /** Only select {@link Capability}s with the given id/ */
  CapabilityQuery capabilityId(String id);
  
  /** Only select {@link Capability}s with the given name */
  CapabilityQuery capabilityName(String capabilityName);
  
  /** Only select {@link Capability}s with the given user id. */
  CapabilityQuery userId(String userId);
  
  /** Only select {@link Capability}s with the given group id. */
  CapabilityQuery groupId(String groupId);
  
  /** Only select {@link Capability}s with the given group ids. */
  CapabilityQuery groupIds(List<String> groupIds);

}
