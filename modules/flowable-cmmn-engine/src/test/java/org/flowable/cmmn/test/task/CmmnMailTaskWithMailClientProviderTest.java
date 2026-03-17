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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.impl.cfg.mail.FlowableMailClientCreator;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.common.engine.impl.cfg.mail.DefaultMailClientProvider;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.api.client.MailClientProvider;
import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;

/**
 * Tests for the {@link MailClientProvider} integration in the CMMN engine.
 *
 * @author Valentin Zickner
 */
public class CmmnMailTaskWithMailClientProviderTest extends CmmnEmailTestCase {

    private static final String CMMN_RESOURCE = "org/flowable/cmmn/test/task/CmmnMailTaskWithMailClientProviderTest.testSimpleTextMail.cmmn";

    @Test
    @CmmnDeployment(resources = CMMN_RESOURCE)
    public void testProviderReturnsClientForNullTenantId() {
        FlowableMailClient providerClient = createMailClient("provider-default@flowable.org");
        cmmnEngineConfiguration.setMailClientProvider(requestedTenantId -> {
            if (requestedTenantId == null) {
                return providerClient;
            }
            return null;
        });

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSimpleTextMail")
                .start();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("provider-default@flowable.org");
    }

    @Test
    public void testProviderReturnsClientForTenant() {
        String tenantId = "providerTenant";

        FlowableMailClient tenantClient = createMailClient("provider-tenant@flowable.org");
        cmmnEngineConfiguration.setMailClientProvider(requestedTenantId -> {
            if (tenantId.equals(requestedTenantId)) {
                return tenantClient;
            }
            return null;
        });

        String deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource(CMMN_RESOURCE)
                .tenantId(tenantId)
                .deploy()
                .getId();

        try {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testSimpleTextMail")
                    .tenantId(tenantId)
                    .start();

            List<WiserMessage> messages = wiser.getMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("provider-tenant@flowable.org");
        } finally {
            cmmnRepositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    @CmmnDeployment(resources = CMMN_RESOURCE)
    public void testDefaultProviderFallsBackToDefaultClient() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSimpleTextMail")
                .start();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        // DefaultMailClientProvider should resolve the default mail client
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("flowable@localhost");
    }

    @Test
    public void testDefaultProviderResolvesTenantFromStaticConfig() {
        String tenantId = "staticTenant";
        addMailServer(tenantId, "static-tenant@flowable.org", null);
        reinitializeMailClients();

        String deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource(CMMN_RESOURCE)
                .tenantId(tenantId)
                .deploy()
                .getId();

        try {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testSimpleTextMail")
                    .tenantId(tenantId)
                    .start();

            List<WiserMessage> messages = wiser.getMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("static-tenant@flowable.org");
        } finally {
            cmmnRepositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    @CmmnDeployment(resources = CMMN_RESOURCE)
    public void testDefaultMailClientProviderIsSetByDefault() {
        assertThat(cmmnEngineConfiguration.getMailClientProvider()).isInstanceOf(DefaultMailClientProvider.class);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSimpleTextMail")
                .start();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("flowable@localhost");
    }

    protected FlowableMailClient createMailClient(String defaultFrom) {
        MailServerInfo mailServerInfo = new MailServerInfo();
        mailServerInfo.setMailServerHost("localhost");
        mailServerInfo.setMailServerPort(5025);
        mailServerInfo.setMailServerUseSSL(false);
        mailServerInfo.setMailServerUseTLS(false);
        mailServerInfo.setMailServerDefaultFrom(defaultFrom);
        mailServerInfo.setMailServerUsername(defaultFrom);
        mailServerInfo.setMailServerPassword("password");
        return FlowableMailClientCreator.createHostClient("localhost", mailServerInfo);
    }

}
