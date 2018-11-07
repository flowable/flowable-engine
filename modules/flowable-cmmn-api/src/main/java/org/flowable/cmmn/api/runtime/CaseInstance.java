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

import java.util.Date;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public interface CaseInstance {

    String getId();
    String getParentId();
    String getBusinessKey();
    String getName();
    String getCaseDefinitionId();
    String getState();
    Date getStartTime();
    String getStartUserId();
    String getCallbackId();
    String getCallbackType();
    boolean isCompleteable();
    String getTenantId();

    /**
     * Returns the case variables if requested in the case instance query
     */
    Map<String, Object> getCaseVariables();

}
