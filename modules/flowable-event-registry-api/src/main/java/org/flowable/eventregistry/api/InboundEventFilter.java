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
package org.flowable.eventregistry.api;

/**
 * This interface must be implemented by a custom filtering bean to hook into the default inbound event processing
 * pipeline in order to have a very effective way to filter out events which should not be processed any further and
 * thus preventing expensive processing time like DB lookups and the full consumer invocation.
 *
 * @param <T> the type of the expected payload (e.g. a JsonNode)
 *
 * @author Micha Kiener
 */
@FunctionalInterface
public interface InboundEventFilter<T> {

    /**
     * Returns true, if the event should be further processed
     * or false if the event should be ignored and will not be processed any further.
     *
     * @param eventDefinitionKey the key for the inbound event
     * @param event the inbound event information
     * @return true, if the event should continue to be processed, false, if the pipeline will ignore the event and stop any
     *      further processing
     */
    boolean retain(String eventDefinitionKey, FlowableEventInfo<T> event);

}
