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

/**
 * An identity link is used to associate a task with a certain identity.
 * 
 * For example: - a user can be an assignee (= identity link type) for a task - a group can be a candidate-group (= identity link type) for a task
 * 
 * @author Joram Barrez
 */
public interface IdentityLink extends IdentityLinkInfo {

    /**
     * The process definition id associated with this identity link.
     */
    String getProcessDefinitionId();

}
