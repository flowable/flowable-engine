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

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.mail.common.api.client.MailClientProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.subethamail.wiser.Wiser;

/**
 * @author Valentin Zickner
 */
@Tag("email")
public abstract class CmmnEmailTestCase extends FlowableCmmnTestCase {

    protected static Wiser wiser;
    private MailClientProvider initialMailClientProvider;
    private Map<String, MailServerInfo> initialMailServers;

    @BeforeAll
    public static void setupWiser() throws Exception {
        int counter = 0;
        boolean serverUpAndRunning = false;
        while (!serverUpAndRunning && counter++ < 11) {
            wiser = Wiser.port(5025);
            try {
                wiser.start();
                serverUpAndRunning = true;
            } catch (RuntimeException e) { // Fix for slow port-closing Jenkins
                if (e.getMessage().toLowerCase().contains("bindexception")) {
                    Thread.sleep(250L);
                }
            }
        }
    }

    @BeforeEach
    public void setUp() {
        wiser.getMessages().clear();
        initialMailClientProvider = cmmnEngineConfiguration.getMailClientProvider();
        Map<String, MailServerInfo> mailServers = cmmnEngineConfiguration.getMailServers();
        initialMailServers = mailServers == null ? null : new HashMap<>(mailServers);
    }

    @AfterEach
    public void tearDown() {
        cmmnEngineConfiguration.setMailClientProvider(initialMailClientProvider);
        if (initialMailServers != null) {
            cmmnEngineConfiguration.getMailServers().clear();
            cmmnEngineConfiguration.getMailServers().putAll(initialMailServers);
        }
        reinitializeMailClients();
    }

    @AfterAll
    public static void stopWiser() {
        wiser.stop();
    }

    protected void reinitializeMailClients() {
        cmmnEngineConfiguration.setDefaultMailClient(null);
        cmmnEngineConfiguration.getMailClients().clear();
        cmmnEngineConfiguration.setMailClientProvider(null);
        cmmnEngineConfiguration.initMailClients();
    }

    protected void addMailServer(String tenantId, String defaultFrom, String forceTo) {
        MailServerInfo mailServerInfo = new MailServerInfo();
        mailServerInfo.setMailServerHost("localhost");
        mailServerInfo.setMailServerPort(5025);
        mailServerInfo.setMailServerUseSSL(false);
        mailServerInfo.setMailServerUseTLS(false);
        mailServerInfo.setMailServerDefaultFrom(defaultFrom);
        mailServerInfo.setMailServerForceTo(forceTo);
        mailServerInfo.setMailServerUsername(defaultFrom);
        mailServerInfo.setMailServerPassword("password");

        cmmnEngineConfiguration.getMailServers().put(tenantId, mailServerInfo);
    }

}
