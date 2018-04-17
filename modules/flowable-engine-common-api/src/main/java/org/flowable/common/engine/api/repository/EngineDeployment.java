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
package org.flowable.common.engine.api.repository;

import java.util.Date;
import java.util.Map;

/**
 * Represents a deployment that is already present in the engine repository.
 * 
 * A deployment is a container for resources such as process definitions, case definitions, images, forms, etc.
 * 
 * @author Tijs Rademakers
 */
public interface EngineDeployment {

    String getId();

    String getName();

    Date getDeploymentTime();

    String getCategory();

    String getKey();
    
    String getDerivedFrom();

    String getDerivedFromRoot();

    String getTenantId();
    
    String getEngineVersion();
    
    boolean isNew();

    Map<String, EngineResource> getResources();
}
