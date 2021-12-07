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
package org.flowable.dmn.rest.service.api;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Yvo Swillens
 */
public final class DmnRestUrls {

    public static final String SEGMENT_REPOSITORY_RESOURCES = "dmn-repository";
    public static final String SEGMENT_RULES_RESOURCES = "dmn-rule";
    public static final String SEGMENT_HISTORY_RESOURCES = "dmn-history";

    public static final String SEGMENT_DEPLOYMENT_RESOURCE = "deployments";
    public static final String SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT = "resourcedata";
    public static final String SEGMENT_DECISION_TABLE_RESOURCE = "decision-tables";
    public static final String SEGMENT_DECISION_RESOURCE = "decisions";
    public static final String SEGMENT_HISTORIC_DECISION_EXECUTION_RESOURCE = "historic-decision-executions";
    public static final String SEGMENT_HISTORIC_DECISION_EXECUTION_AUDITDATA = "auditdata";
    public static final String SEGMENT_DECISION_MODEL = "model";
    public static final String SEGMENT_EXECUTE_RESOURCE = "execute";
    public static final String SEGMENT_EXECUTE_DECISION_RESOURCE = "execute-decision";
    public static final String SEGMENT_EXECUTE_DECISION_SERVICE_RESOURCE = "execute-decision-service";
    public static final String SEGMENT_EXECUTE_SINGLE_RESULT_RESOURCE = "single-result";

    /**
     * URL template for a decision table collection: <i>/dmn-repository/decision-tables/{0:decisionTableId}</i>
     *
     * @deprecated use {@link #URL_DECISION_COLLECTION} instead.
     */
    @Deprecated
    public static final String[] URL_DECISION_TABLE_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_TABLE_RESOURCE };

    /**
     * URL template for a single decision table: <i>/dmn-repository/decision-tables/{0:decisionTableId}</i>
     *
     * @deprecated use {@link #URL_DECISION} instead.
     */
    @Deprecated
    public static final String[] URL_DECISION_TABLE = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_TABLE_RESOURCE, "{0}" };

    /**
     * URL template for a single decision table model: <i>/dmn-repository/decision-tables/{0:decisionTableId}/model</i>
     *
     * @deprecated use {@link #URL_DECISION_MODEL} instead.
     */
    @Deprecated
    public static final String[] URL_DECISION_TABLE_MODEL = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_TABLE_RESOURCE, "{0}", SEGMENT_DECISION_MODEL };

    /**
     * URL template for the resource of a single decision table: <i>/dmn-repository/decision-tables/{0:decisionId}/resourcedata</i>
     *
     * @deprecated use {@link #URL_DECISION_RESOURCE_CONTENT} instead.
     */
    @Deprecated
    public static final String[] URL_DECISION_TABLE_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_TABLE_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };

    /**
     * URL template for a decision collection: <i>/dmn-repository/decisions</i>
     */
    public static final String[] URL_DECISION_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_RESOURCE };

    /**
     * URL template for a single decision: <i>/dmn-repository/decisions/{0:decisionId}</i>
     */
    public static final String[] URL_DECISION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_RESOURCE, "{0}" };

    /**
     * URL template for a single decision model: <i>/dmn-repository/decisions/{0:decisionId}/model</i>
     */
    public static final String[] URL_DECISION_MODEL = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_RESOURCE, "{0}", SEGMENT_DECISION_MODEL };

    /**
     * URL template for the resource of a single decision: <i>/dmn-repository/decisions/{0:decisionId}/resourcedata</i>
     */
    public static final String[] URL_DECISION_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DECISION_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT };

    /**
     * URL template for a deployment collection: <i>/dmn-repository/deployments</i>
     */
    public static final String[] URL_DEPLOYMENT_COLLECTION = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE };

    /**
     * URL template for a single deployment: <i>/dmn-repository/deployments/{0:deploymentId}</i>
     */
    public static final String[] URL_DEPLOYMENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}" };

    /**
     * URL template for the resource of a single deployment: <i>/dmn-repository/deployments/{0:deploymentId}/resourcedata/{1:resourceId}</i>
     */
    public static final String[] URL_DEPLOYMENT_RESOURCE_CONTENT = { SEGMENT_REPOSITORY_RESOURCES, SEGMENT_DEPLOYMENT_RESOURCE, "{0}", SEGMENT_DEPLOYMENT_ARTIFACT_RESOURCE_CONTENT, "{1}" };

    /**
     * URL template for a decision executor: <i>/dmn-rule/execute</i>
     */
    public static final String[] URL_RULE_SERVICE_EXECUTE = { SEGMENT_RULES_RESOURCES, SEGMENT_EXECUTE_RESOURCE};

    /**
     * URL template for a decision executor: <i>/dmn-rule/execute/single-result</i>
     */
    public static final String[] URL_RULE_SERVICE_EXECUTE_SINGLE_RESULT = { SEGMENT_RULES_RESOURCES, SEGMENT_EXECUTE_RESOURCE, SEGMENT_EXECUTE_SINGLE_RESULT_RESOURCE};

    /**
     * URL template for a decision executor: <i>/dmn-decision/execute-decision</i>
     */
    public static final String[] URL_RULE_SERVICE_EXECUTE_DECISION = { SEGMENT_RULES_RESOURCES, SEGMENT_EXECUTE_DECISION_RESOURCE};

    /**
     * URL template for a decision executor: <i>/dmn-decision/execute-decision/single-result</i>
     */
    public static final String[] URL_RULE_SERVICE_EXECUTE_DECISION_SINGLE_RESULT = { SEGMENT_RULES_RESOURCES, SEGMENT_EXECUTE_DECISION_RESOURCE, SEGMENT_EXECUTE_SINGLE_RESULT_RESOURCE};

    /**
     * URL template for a decision executor: <i>/dmn-decision/execute-decision-service</i>
     */
    public static final String[] URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE = { SEGMENT_RULES_RESOURCES, SEGMENT_EXECUTE_DECISION_SERVICE_RESOURCE};

    /**
     * URL template for a decision executor: <i>/dmn-decision/execute-decision-service/single-result</i>
     */
    public static final String[] URL_RULE_SERVICE_EXECUTE_DECISION_SERVICE_SINGLE_RESULT = { SEGMENT_RULES_RESOURCES, SEGMENT_EXECUTE_DECISION_SERVICE_RESOURCE, SEGMENT_EXECUTE_SINGLE_RESULT_RESOURCE};

    /**
     * URL template for a historic decision execution collection: <i>/dmn-history/historic-decision-executions</i>
     */
    public static final String[] URL_HISTORIC_DECISION_EXECUTION_COLLECTION = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_DECISION_EXECUTION_RESOURCE };
    
    /**
     * URL template for a historic decision execution: <i>/dmn-history/historic-decision-executions/{0:historicDecisionExecution}</i>
     */
    public static final String[] URL_HISTORIC_DECISION_EXECUTION = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_DECISION_EXECUTION_RESOURCE, "{0}" };
    
    /**
     * URL template for a historic decision execution audit data: <i>/dmn-history/historic-decision-executions/{0:historicDecisionExecution}/auditdata</i>
     */
    public static final String[] URL_HISTORIC_DECISION_EXECUTION_AUDITDATA = { SEGMENT_HISTORY_RESOURCES, SEGMENT_HISTORIC_DECISION_EXECUTION_RESOURCE, "{0}", SEGMENT_HISTORIC_DECISION_EXECUTION_AUDITDATA };

    /**
     * Creates an url based on the passed fragments and replaces any placeholders with the given arguments. The placeholders are following the {@link MessageFormat} convention (eg. {0} is replaced by
     * first argument value).
     */
    public static String createRelativeResourceUrl(String[] segments, Object... arguments) {
        return MessageFormat.format(StringUtils.join(segments, '/'), arguments);
    }
}
