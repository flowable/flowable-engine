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

/**
 * An object structure representing a submitted form.
 * 
 * @author Tijs Rademakers
 * @author Joram Barez
 */
public interface FormInstance {

    /** unique identifier */
    String getId();

    /**
     * Reference to the form definition of this form instance
     */
    String getFormDefinitionId();

    /**
     * Reference to the task for which the form instance was created
     */
    String getTaskId();

    /**
     * Reference to the process instance for which the form instance was created
     */
    String getProcessInstanceId();

    /**
     * Reference to the process definition for which the form instance was created
     */
    String getProcessDefinitionId();
    
    /**
     * Reference to the scope instance for which the form instance was created
     */
    String getScopeId();
    
    /**
     * Type of the scope instance for which the form instance was created
     */
    String getScopeType();
    
    /**
     * Reference to the scope instance definition for which the form instance was created
     */
    String getScopeDefinitionId();

    /**
     * Submitted date for the form instance
     */
    Date getSubmittedDate();

    /**
     * Reference to the user that submitted the form instance
     */
    String getSubmittedBy();

    /**
     * Reference to the JSON document id that contains the submitted form values
     */
    String getFormValuesId();

    /** 
     * The tenant identifier of this form instance
     */
    String getTenantId();

    /** 
     * The JSON document that contains the submitted form values
     */
    byte[] getFormValueBytes();

}
