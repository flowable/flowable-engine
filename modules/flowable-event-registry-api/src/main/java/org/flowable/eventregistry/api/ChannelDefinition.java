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

import java.util.Date;

import org.flowable.common.engine.api.repository.FlowableDefinition;

/**
 * An object structure representing a channel definition
 * 
 * @author Tijs Rademakers
 * @author Joram Barez
 */
public interface ChannelDefinition extends FlowableDefinition {

    /** category name of the channel definition */
    String getCategory();

    /** description of this channel definition **/
    String getDescription();

    /**
     * The type of the channel.
     * e.g.inbound, outbound
     */
    String getType();

    /**
     * The implementation type of the channel.
     * e.g. jms, kafka, rabbit
     */
    String getImplementation();

    /** create time for this channel definition */
    Date getCreateTime();

}
