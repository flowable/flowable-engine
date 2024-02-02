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

/**
 * An object structure representing a channel definition
 * 
 * @author Tijs Rademakers
 * @author Joram Barez
 */
public interface ChannelDefinition {

    /** unique identifier */
    String getId();

    /** category name of the channel definition */
    String getCategory();

    /** label used for display purposes */
    String getName();

    /** unique name for all versions this channel definition */
    String getKey();
    
    /** version of this channel definition */
    int getVersion();

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

    /** name of {@link EventRepositoryService#getResourceAsStream(String, String) the resource} of this channel definition. */
    String getResourceName();

    /** The deployment in which this channel definition is contained. */
    String getDeploymentId();
    
    /** create time for this channel definition */
    Date getCreateTime();

    /** The tenant identifier of this channel definition */
    String getTenantId();

}
