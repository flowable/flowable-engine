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
package org.activiti.engine.impl.bpmn.mail;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.content.api.ContentService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.impl.BaseMailActivityDelegate;

public class BpmnMailActivityDelegate extends BaseMailActivityDelegate<DelegateExecution>
        implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        prepareAndExecuteRequest(execution);
    }

    @Override
    protected FlowableMailClient getMailClient(DelegateExecution execution) {
        ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
        String tenantId = execution.getTenantId();
        FlowableMailClient mailClient = null;
        if (StringUtils.isNotBlank(tenantId)) {
            mailClient = processEngineConfiguration.getMailClient(tenantId);
        }

        if (mailClient == null) {
            mailClient = processEngineConfiguration.getDefaultMailClient();
        }

        return mailClient;
    }

    @Override
    protected Expression createExpression(String expressionText) {
        return Context.getProcessEngineConfiguration().getExpressionManager().createExpression(expressionText);
    }

    @Override
    protected ContentService getContentService() {
        return null;
    }

}
