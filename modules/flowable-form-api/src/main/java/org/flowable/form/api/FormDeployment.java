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
 * Represents a deployment that is already present in the form repository.
 * 
 * A deployment is a container for resources such as form definitions, image, etc.
 * 
 * When a deployment is 'deployed', the Flowable engine will recognize certain of such resource types and act upon them (eg form definitions
 * will be parsed to an executable Java artifact).
 * 
 * To create a Deployment, use the {@link org.flowable.form.api.FormDeploymentBuilder}. A Deployment on itself is a <b>read-only</b> object and its content cannot be changed after deployment
 * (hence the builder that needs to be used).
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface FormDeployment {

    String getId();

    String getName();

    Date getDeploymentTime();

    String getCategory();

    String getTenantId();

    String getParentDeploymentId();
}
