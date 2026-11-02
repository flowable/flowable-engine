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
package org.flowable.engine.test.bpmn.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.drools.util.StringUtils;
import org.flowable.common.engine.impl.cfg.mail.DefaultMailClientProvider;
import org.flowable.common.engine.impl.cfg.mail.FlowableMailClientCreator;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.engine.test.Deployment;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.api.client.MailClientProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;

/**
 * Tests for the {@link MailClientProvider} integration in the BPMN engine.
 *
 * @author Valentin Zickner
 */
class EmailSendTaskWithMailClientProviderTest extends EmailTestCase {

    protected MailClientProvider initialMailClientProvider;

    @BeforeEach
    void saveProvider() {
        initialMailClientProvider = processEngineConfiguration.getMailClientProvider();
        reinitilizeMailClients();
    }

    @AfterEach
    void restoreProvider() {
        processEngineConfiguration.setMailClientProvider(initialMailClientProvider);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml", tenantId = "providerTenant")
    void testProviderReturnsClientForTenant() {
        FlowableMailClient tenantClient = createMailClient("provider-tenant@flowable.org");
        processEngineConfiguration.setMailClientProvider(requestedTenantId -> {
            if ("providerTenant".equals(requestedTenantId)) {
                return tenantClient;
            }
            return null;
        });

        runtimeService.startProcessInstanceByKeyAndTenantId("simpleTextOnly", "providerTenant");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("provider-tenant@flowable.org");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml", tenantId = "staticTenant")
    void testDefaultProviderResolvesTenantFromStaticConfig() {
        addMailServer("staticTenant", "static-tenant@flowable.org", null);
        reinitilizeMailClients();

        runtimeService.startProcessInstanceByKeyAndTenantId("simpleTextOnly", "staticTenant");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("static-tenant@flowable.org");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml")
    void testProviderReturnsClientForEmptyTenantId() {
        FlowableMailClient defaultProviderClient = createMailClient("provider-default@flowable.org");
        processEngineConfiguration.setMailClientProvider(requestedTenantId -> {
            if (StringUtils.isEmpty(requestedTenantId)) {
                return defaultProviderClient;
            }
            return null;
        });

        runtimeService.startProcessInstanceByKey("simpleTextOnly");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("provider-default@flowable.org");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml")
    void testDefaultProviderFallsBackToDefaultClient() {
        runtimeService.startProcessInstanceByKey("simpleTextOnly");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        // DefaultMailClientProvider should resolve the default mail client (flowable@localhost from test config)
        assertThat(messages.get(0).getEnvelopeSender()).isEqualTo("flowable@localhost");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml")
    void testDefaultMailClientProviderIsSetByDefault() {
        assertThat(processEngineConfiguration.getMailClientProvider()).isInstanceOf(DefaultMailClientProvider.class);

        runtimeService.startProcessInstanceByKey("simpleTextOnly");

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
