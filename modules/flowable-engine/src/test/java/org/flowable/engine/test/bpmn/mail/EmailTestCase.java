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

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.cfg.MailServerInfo;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.subethamail.wiser.Wiser;

/**
 * @author Joram Barrez
 */
@Tag("email")
public abstract class EmailTestCase extends PluggableFlowableTestCase {

    protected Wiser wiser;
    private String initialForceTo;
    private Map<String, MailServerInfo> initialMailServers;

    @BeforeEach
    protected void setUp() throws Exception {

        initialForceTo = processEngineConfiguration.getMailServerForceTo();
        Map<String, MailServerInfo> mailServers = processEngineConfiguration.getMailServers();
        initialMailServers = mailServers == null ? null : new HashMap<>(mailServers);
        boolean serverUpAndRunning = false;
        while (!serverUpAndRunning) {
            wiser = new Wiser();
            wiser.setPort(5025);

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

    @AfterEach
    protected void tearDown() throws Exception {
        wiser.stop();

        // Fix for slow Jenkins
        Thread.sleep(250L);

        processEngineConfiguration.setMailServerForceTo(initialForceTo);
        processEngineConfiguration.setMailServers(initialMailServers);
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

        processEngineConfiguration.getMailServers().put(tenantId, mailServerInfo);
    }

}
