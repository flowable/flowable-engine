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
package org.flowable.common.engine.api.tenant;

import java.util.Set;

/**
 * Container interface to return the result of a change in a tenant id operation
 */
public interface ChangeTenantIdResult {

    /**
     * The number of instances that were changed for the given entity type.
     */
    long getChangedInstances(String entityType);

    /**
     * The types of the changed entities
     */
    Set<String> getChangedEntityTypes();

}
