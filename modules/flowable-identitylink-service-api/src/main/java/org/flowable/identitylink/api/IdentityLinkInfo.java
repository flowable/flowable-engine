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

package org.flowable.identitylink.api;

public interface IdentityLinkInfo {

    /**
     * Returns the type of link. See for the native supported types.
     */
    String getType();

    /**
     * If the identity link involves a user, then this will be a non-null id of a user. That userId can be used to query for user information through the UserQuery API.
     */
    String getUserId();

    /**
     * If the identity link involves a group, then this will be a non-null id of a group. That groupId can be used to query for user information through the GroupQuery API.
     */
    String getGroupId();

    /**
     * The id of the task associated with this identity link.
     */
    String getTaskId();

    /**
     * The process instance id associated with this identity link.
     */
    String getProcessInstanceId();
    
    /**
     * The scope id associated with this identity link
     */
    String getScopeId();
    
    /**
     * The scope type associated with this identity link
     */
    String getScopeType();
    
    /**
     * The scope definition id associated with this identity link
     */
    String getScopeDefinitionId();
}
