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
package org.flowable.ui.common.security;

import java.util.Set;

/**
 * Security scope that can be used for passing the needed security scope accessibility to the Java API.
 *
 * @author Filip Hrisafov
 */
public interface SecurityScope {

    /**
     * The id of the user for which the security scope is meant for
     *
     * @return the user id
     */
    String getUserId();

    /**
     * The group ids for which the security scope is meant for
     *
     * @return the group keys
     */
    Set<String> getGroupIds();

    /**
     * The tenant id for which the security scope is meant for
     *
     * @return the tenant id
     */
    String getTenantId();

    /**
     * Check if the security scope has the given authority.
     *
     * @param authority the authority to be checked
     * @return {@code true} if the security scope has the given authority, {@code false} otherwise
     */
    boolean hasAuthority(String authority);

}
