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
package org.flowable.rest.form;

import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;

/**
 * @author Yvo Swillens
 */
public final class FormRestUrls {

    public static final String SEGMENT_FORM_RESOURCES = "form";
    public static final String SEGMENT_REPOSITORY_RESOURCES = "form-repository";
    public static final String SEGMENT_RUNTIME_FORM_DEFINITION = "runtime-form-definition";
    public static final String SEGMENT_COMPLETED_FORM_DEFINITION = "completed-form-definition";
    public static final String SEGMENT_DEPLOYMENT_RESOURCES = "deployments";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT = "resourcedata";
    public static final String SEGMENT_FORMS_RESOURCES = "forms";
    public static final String SEGMENT_FORM_MODEL = "model";
    public static final String SEGMENT_FORM_INSTANCES_RESOURCES = "form-instances";
    public static final String SEGMENT_QUERY_RESOURCES = "query";

    /**
     * URL template for a form collection: <i>/form/runtime-form-definition</i>
     */
    public static final String[] URL_RUNTIME_TASK_FORM = { SEGMENT_FORM_RESOURCES, SEGMENT_RUNTIME_FORM_DEFINITION };

    /**
     * URL template for a form collection: <i>/form/completed-form-definition</i>
     */
    public static final String[] URL_COMPLETED_TASK_FORM = { SEGMENT_FORM_RESOURCES, SEGMENT_COMPLETED_FORM_DEFINITION };

    /**
     * URL template for a form collection: <i>/form-repository/forms/{0:formId}</i>
     */
    public static final String[] URL_FORM_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_FORMS_RESOURCES };

    /**
     * URL template for a single form: <i>/form-repository/forms/{0:formId}</i>
     */
    public static final String[] URL_FORM = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_FORMS_RESOURCES, "{0}" };

    /**
     * URL template for a single form model: <i>/form-repository/forms/{0:formId}/model</i>
     */
    public static final String[] URL_FORM_MODEL = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_FORM_RESOURCES, "{0}", SEGMENT_FORM_MODEL };

    /**
     * URL template for the resource of a single form: <i>/form-repository/forms/{0:formId}/resourcedata</i>
     */
    public static final String[] URL_FORM_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_FORM_RESOURCES, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };

    /**
     * URL template for a deployment collection: <i>/form-repository/deployments</i>
     */
    public static final String[] URL_DEPLOYMENT_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES };

    /**
     * URL template for a single deployment: <i>/form-repository/deployments/{0:deploymentId}</i>
     */
    public static final String[] URL_DEPLOYMENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES, "{0}" };

    /**
     * URL template for the resource of a single deployment: <i>/form-repository/deployments/{0:deploymentId}/resourcedata/{1:resourceId}</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCES, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT, "{1}" };

    /**
     * URL template for the resource of a form instance query: <i>/query/form-instances</i>
     */
    public static final String[] URL_FORM_INSTANCE_QUERY = { SEGMENT_QUERY_RESOURCES, SEGMENT_FORM_INSTANCES_RESOURCES };

    /**
     * Creates an url based on the passed fragments and replaces any placeholders with the given arguments. The placeholders are following the {@link MessageFormat} convention (eg. {0} is replaced by
     * first argument value).
     */
    public static final String createRelativeResourceUrl(String[] segments, Object... arguments) {
        return MessageFormat.format(StringUtils.join(segments, '/'), arguments);
    }
}
