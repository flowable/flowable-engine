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
 * @author Tijs Rademakers
 */
public interface GenericEventListenerInstanceQuery extends Query<GenericEventListenerInstanceQuery, GenericEventListenerInstance> {

    GenericEventListenerInstanceQuery id(String id);
    GenericEventListenerInstanceQuery caseInstanceId(String caseInstanceId);
    GenericEventListenerInstanceQuery caseDefinitionId(String caseDefinitionId);
    GenericEventListenerInstanceQuery elementId(String elementId);
    GenericEventListenerInstanceQuery planItemDefinitionId(String planItemDefinitionId);
    GenericEventListenerInstanceQuery name(String name);
    GenericEventListenerInstanceQuery stageInstanceId(String stageInstanceId);
    GenericEventListenerInstanceQuery stateAvailable();
    GenericEventListenerInstanceQuery stateSuspended();

    GenericEventListenerInstanceQuery orderByName();
    
}