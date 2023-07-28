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
package org.flowable.cmmn.engine.impl.behavior.impl.mail;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.content.api.ContentService;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.impl.BaseMailActivityDelegate;

/**
 * @author Filip Hrisafov
 */
public class CmmnMailActivityDelegate extends BaseMailActivityDelegate<DelegatePlanItemInstance> implements PlanItemJavaDelegate {

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        prepareAndExecuteRequest(planItemInstance);
    }

    @Override
    protected FlowableMailClient getMailClient(DelegatePlanItemInstance planItemInstance) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        String tenantId = planItemInstance.getTenantId();
        FlowableMailClient mailClient = null;
        if (StringUtils.isNotBlank(tenantId)) {
            mailClient = cmmnEngineConfiguration.getMailClient(tenantId);
        }

        if (mailClient == null) {
            mailClient = cmmnEngineConfiguration.getDefaultMailClient();
        }

        return mailClient;
    }

    @Override
    protected Expression createExpression(String expressionText) {
        return CommandContextUtil.getCmmnEngineConfiguration().getExpressionManager().createExpression(expressionText);
    }

    @Override
    protected ContentService getContentService() {
        return CommandContextUtil.getContentService();
    }
}
