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
package org.flowable.eventregistry.impl.persistence.deploy;

import java.io.Serializable;

import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;

/**
 * @author Tijs Rademakers
 */
public class EventDefinitionCacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    protected EventDefinitionEntity eventDefinitionEntity;
    protected String eventDefinitionJson;

    public EventDefinitionCacheEntry(EventDefinitionEntity eventDefinitionEntity, String eventDefinitionJson) {
        this.eventDefinitionEntity = eventDefinitionEntity;
        this.eventDefinitionJson = eventDefinitionJson;
    }

    public EventDefinitionEntity getEventDefinitionEntity() {
        return eventDefinitionEntity;
    }

    public void setEventDefinitionEntity(EventDefinitionEntity eventDefinitionEntity) {
        this.eventDefinitionEntity = eventDefinitionEntity;
    }

    public String getEventDefinitionJson() {
        return eventDefinitionJson;
    }

    public void setEventDefinitionJson(String eventDefinitionJson) {
        this.eventDefinitionJson = eventDefinitionJson;
    }
}
