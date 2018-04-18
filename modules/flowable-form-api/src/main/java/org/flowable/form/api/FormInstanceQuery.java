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

package org.flowable.form.api;

import java.util.Date;
import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link FormInstance}s.
 * 
 * @author Tijs Rademakers
 */
public interface FormInstanceQuery extends Query<FormInstanceQuery, FormInstance> {

    /**
     * Only select submitted forms with the given id.
     */
    FormInstanceQuery id(String id);

    /**
     * Only select submitted forms with the given ids.
     */
    FormInstanceQuery ids(Set<String> ids);

    /**
     * Only select submitted forms with the given form definition id.
     */
    FormInstanceQuery formDefinitionId(String formDefinitionId);

    /**
     * Only select submitted forms with a form definition id like the given string.
     */
    FormInstanceQuery formDefinitionIdLike(String formDefinitionIdLike);

    /**
     * Only select submitted forms with the given task id.
     */
    FormInstanceQuery taskId(String taskId);

    /**
     * Only select submitted forms with a task id like the given string.
     */
    FormInstanceQuery taskIdLike(String taskIdLike);

    /**
     * Only select submitted forms with the given process instance id.
     */
    FormInstanceQuery processInstanceId(String processInstanceId);

    /**
     * Only select submitted forms with a process instance id like the given string.
     */
    FormInstanceQuery processInstanceIdLike(String processInstanceIdLike);

    /**
     * Only select submitted forms with the given process definition id.
     */
    FormInstanceQuery processDefinitionId(String processDefinitionId);

    /**
     * Only select submitted forms with a process definition id like the given string.
     */
    FormInstanceQuery processDefinitionIdLike(String processDefinitionIdLike);
    
    /**
     * Only select submitted forms with the given scope id.
     */
    FormInstanceQuery scopeId(String scopeId);
    
    /**
     * Only select submitted forms with the given scope type.
     */
    FormInstanceQuery scopeType(String scopeType);
    
    /**
     * Only select submitted forms with the given scope definition id.
     */
    FormInstanceQuery scopeDefinitionId(String scopeDefinitionId);

    /**
     * Only select submitted forms submitted on the given time
     */
    FormInstanceQuery submittedDate(Date submittedDate);

    /**
     * Only select submitted forms submitted before the given time
     */
    FormInstanceQuery submittedDateBefore(Date beforeTime);

    /**
     * Only select submitted forms submitted after the given time
     */
    FormInstanceQuery submittedDateAfter(Date afterTime);

    /**
     * Only select submitted forms with the given submitted by value.
     */
    FormInstanceQuery submittedBy(String submittedBy);

    /**
     * Only select submitted forms with a submitted by like the given string.
     */
    FormInstanceQuery submittedByLike(String submittedByLike);

    /**
     * Only select submitted forms that have the given tenant id.
     */
    FormInstanceQuery deploymentTenantId(String tenantId);

    /**
     * Only select submitted forms with a tenant id like the given one.
     */
    FormInstanceQuery deploymentTenantIdLike(String tenantIdLike);

    /**
     * Only select submitted forms that do not have a tenant id.
     */
    FormInstanceQuery deploymentWithoutTenantId();

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by submitted date (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormInstanceQuery orderBySubmittedDate();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormInstanceQuery orderByTenantId();
}
