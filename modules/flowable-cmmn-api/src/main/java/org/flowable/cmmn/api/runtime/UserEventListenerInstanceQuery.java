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
package org.flowable.cmmn.api.runtime;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Dennis Federico
 */
public interface UserEventListenerInstanceQuery extends Query<UserEventListenerInstanceQuery, UserEventListenerInstance> {

    UserEventListenerInstanceQuery id(String id);
    UserEventListenerInstanceQuery caseInstanceId(String caseInstanceId);
    UserEventListenerInstanceQuery caseDefinitionId(String caseDefinitionId);
    UserEventListenerInstanceQuery elementId(String elementId);
    UserEventListenerInstanceQuery planItemDefinitionId(String planItemDefinitionId);
    UserEventListenerInstanceQuery name(String name);
    UserEventListenerInstanceQuery stageInstanceId(String stageInstanceId);
    UserEventListenerInstanceQuery stateAvailable();
    UserEventListenerInstanceQuery stateSuspended();

    UserEventListenerInstanceQuery orderByName();
    
}