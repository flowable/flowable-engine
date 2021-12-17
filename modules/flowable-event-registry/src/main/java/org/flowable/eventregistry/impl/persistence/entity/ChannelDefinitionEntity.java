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
package org.flowable.eventregistry.impl.persistence.entity;

import java.util.Date;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.eventregistry.api.ChannelDefinition;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface ChannelDefinitionEntity extends ChannelDefinition, Entity {

    void setKey(String key);

    void setName(String name);
    
    void setVersion(int version);

    void setDescription(String description);

    void setType(String type);

    void setImplementation(String implementation);

    void setDeploymentId(String deploymentId);
    
    void setCreateTime(Date createTime);

    void setResourceName(String resourceName);

    void setTenantId(String tenantId);

    void setCategory(String category);

}
