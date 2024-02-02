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
package org.flowable.eventregistry.rest.service.api;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

public final class EventRestUrls {

    /**
     * Base segment for all repository-related resources: <i>repository</i>
     */
    public static final String SEGMENT_REPOSITORY_RESOURCES = "event-registry-repository";
    public static final String SEGMENT_RUNTIME_RESOURCES = "event-registry-runtime";
    public static final String SEGMENT_MANAGEMENT_RESOURCES = "event-registry-management";

    public static final String SEGMENT_DEPLOYMENT_RESOURCE = "deployments";
    public static final String SEGMENT_EVENT_DEFINITION_RESOURCE = "event-definitions";
    public static final String SEGMENT_CHANNEL_DEFINITION_RESOURCE = "channel-definitions";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE = "resources";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT = "resourcedata";
    public static final String SEGMENT_MODEL = "model";

    public static final String SEGMENT_EVENT_INSTANCE_RESOURCE = "event-instances";
    public static final String SEGMENT_TABLES = "tables";
    public static final String SEGMENT_COLUMNS = "columns";
    public static final String SEGMENT_DATA = "data";
    public static final String SEGMENT_ENGINE_INFO = "engine";

    /**
     * URL template for the deployment collection: <i>event-registry-repository/deployments</i>
     */
    public static final String[] URL_DEPLOYMENT_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE };

    /**
     * URL template for a single deployment: <i>event-registry-repository/deployments/{0:deploymentId}</i>
     */
    public static final String[] URL_DEPLOYMENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}" };

    /**
     * URL template listing deployment resources: <i>event-registry-repository/deployments/{0:deploymentId}/resources</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCES = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE };

    /**
     * URL template for a single deployment resource: <i>event-registry-repository/deployments/{0:deploymentId}/resources/{1}:resourceId</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE, "{1}" };

    /**
     * URL template for a single deployment resource content: <i>event-registry-repository/deployments /{0:deploymentId}/resourcedata/{1}:resourceId</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT, "{1}" };

    /**
     * URL template for the event definition collection: <i>event-registry-repository/event-definitions</i>
     */
    public static final String[] URL_EVENT_DEFINITION_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_EVENT_DEFINITION_RESOURCE };

    /**
     * URL template for a single event definition: <i>event-registry-repository/event-definitions/{0:eventDefinitionId}</i>
     */
    public static final String[] URL_EVENT_DEFINITION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_EVENT_DEFINITION_RESOURCE, "{0}" };

    /**
     * URL template for the resource of a single event definition: <i>event-registry-repository/event-definitions/{0:eventDefinitionId}/resourcedata</i>
     */
    public static final String[] URL_EVENT_DEFINITION_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_EVENT_DEFINITION_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };
    
    /**
     * URL template for the model of a single event definition: <i>event-registry-repository/event-definitions/{0:eventDefinitionId}/model</i>
     */
    public static final String[] URL_EVENT_DEFINITION_MODEL = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_EVENT_DEFINITION_RESOURCE, "{0}", SEGMENT_MODEL };
    
    /**
     * URL template for the channel definition collection: <i>event-registry-repository/channel-definitions</i>
     */
    public static final String[] URL_CHANNEL_DEFINITION_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CHANNEL_DEFINITION_RESOURCE };

    /**
     * URL template for a single channel definition: <i>event-registry-repository/channel-definitions/{0:channelDefinitionId}</i>
     */
    public static final String[] URL_CHANNEL_DEFINITION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CHANNEL_DEFINITION_RESOURCE, "{0}" };

    /**
     * URL template for the resource of a single channel definition: <i>event-registry-repository/channel-definitions/{0:channelDefinitionId}/resourcedata</i>
     */
    public static final String[] URL_CHANNEL_DEFINITION_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CHANNEL_DEFINITION_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };
    
    /**
     * URL template for the model of a single channel definition: <i>event-registry-repository/channel-definitions/{0:channelDefinitionId}/model</i>
     */
    public static final String[] URL_CHANNEL_DEFINITION_MODEL = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_CHANNEL_DEFINITION_RESOURCE, "{0}", SEGMENT_MODEL };

    /**
     * URL template for event instance collection: <i>event-registry-runtime/event-instances</i>
     */
    public static final String[] URL_EVENT_INSTANCE_COLLECTION = { SEGMENT_RUNTIME_RESOURCES, SEGMENT_EVENT_INSTANCE_RESOURCE };

    /**
     * URL template for the collection of properties: <i>event-registry-management/properties</i>
     */
    public static final String[] URL_ENGINE_INFO = { SEGMENT_MANAGEMENT_RESOURCES, SEGMENT_ENGINE_INFO };

    /**
     * Creates an url based on the passed fragments and replaces any placeholders with the given arguments. The placeholders are following the {@link MessageFormat} convention (eg. {0} is replaced by
     * first argument value).
     */
    public static String createRelativeResourceUrl(String[] segments, Object... arguments) {
        return MessageFormat.format(StringUtils.join(segments, '/'), arguments);
    }
}
