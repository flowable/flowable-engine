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
package org.flowable.app.rest;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademmakers
 */
public final class AppRestUrls {

    public static final String SEGMENT_REPOSITORY_RESOURCES = "app-repository";
    public static final String SEGMENT_DEPLOYMENT_RESOURCES = "deployments";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE = "resources";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT = "resourcedata";
    public static final String SEGMENT_APP_DEFINITIONS_RESOURCES = "app-definitions";
    public static final String SEGMENT_APP_MODEL = "model";
    public static final String SEGMENT_QUERY_RESOURCES = "query";

    /**
     * URL template for an app collection: <i>/app-repository/app-definitions</i>
     */
    public static final String[] URL_APP_DEFINITION_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_APP_DEFINITIONS_RESOURCES };

    /**
     * URL template for a single app: <i>/app-repository/app-definitions/{0:appDefinitionId}</i>
     */
    public static final String[] URL_APP_DEFINITION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_APP_DEFINITIONS_RESOURCES, "{0}" };

    /**
     * URL template for a single app model: <i>/app-repository/app-definitions/{0:appDefinitionId}/model</i>
     */
    public static final String[] URL_APP_DEFINITION_MODEL = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_APP_DEFINITIONS_RESOURCES, "{0}", SEGMENT_APP_MODEL };
    
    /**
     * URL template for the resource of a single app: <i>/app-repository/app-definitions/{0:appDefinitionId}/resourcedata</i>
     */
    public static final String[] URL_APP_DEFINITION_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_APP_DEFINITIONS_RESOURCES, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };

    /**
     * URL template for a deployment collection: <i>/app-repository/deployments</i>
     */
    public static final String[] URL_DEPLOYMENT_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES };

    /**
     * URL template for a single deployment: <i>/app-repository/deployments/{0:deploymentId}</i>
     */
    public static final String[] URL_DEPLOYMENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES, "{0}" };
    
    /**
     * URL template listing deployment resources: <i>app-repository/deployments/{0:deploymentId}/resources</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCES = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE };

    /**
     * URL template for a single deployment resource: <i>app-repository/deployments/{0:deploymentId}/resources/{1}:resourceId</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE, "{1}" };

    /**
     * URL template for the resource of a single deployment: <i>/app-repository/deployments/{0:deploymentId}/resourcedata/{1:resourceId}</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT, "{1}" };

    /**
     * Creates an url based on the passed fragments and replaces any placeholders with the given arguments. The placeholders are following the {@link MessageFormat} convention (eg. {0} is replaced by
     * first argument value).
     */
    public static final String createRelativeResourceUrl(String[] segments, Object... arguments) {
        return MessageFormat.format(StringUtils.join(segments, '/'), arguments);
    }
}
