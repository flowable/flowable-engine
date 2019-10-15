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
package org.flowable.common.engine.api.eventregistry;

import java.util.Collection;
import java.util.Map;

import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;

/**
 * @author Joram Barrez
 */
public interface EventProcessingContext {

    String getRawEvent();

    String getChannelKey();

    EventDefinition getEventDefinition();

    Map<String, Object> getPayload();

    void addProcessingData(String key, Object data);
    <T> T getProcessingData(String key, Class<T> clazz);

    Collection<EventInstance> getEventInstances();

}
