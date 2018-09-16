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
package org.flowable.engine.test.bpmn.async;

import java.util.Collections;
import java.util.List;

import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.bpmn.mail.EmailServiceTaskTest;
import org.flowable.engine.test.bpmn.mail.EmailTestCase;
import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;

/**
 * 
 * 
 * @author Daniel Meyer
 */
public class AsyncEmailTaskTest extends EmailTestCase {

    @Test
    @Deployment
    public void testSimpleTextMail() throws Exception {
        String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly").getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertEquals(0, messages.size());

        waitForJobExecutorToProcessAllJobs(7000L, 25L);

        messages = wiser.getMessages();
        assertEquals(1, messages.size());

        WiserMessage message = messages.get(0);
        EmailServiceTaskTest.assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList("kermit@activiti.org"), null);
        assertProcessEnded(procId);
    }

    @Test
    @Deployment
    public void testSimpleTextMailSendTask() throws Exception {
        runtimeService.startProcessInstanceByKey("simpleTextOnly");

        List<WiserMessage> messages = wiser.getMessages();
        assertEquals(0, messages.size());

        waitForJobExecutorToProcessAllJobs(7000L, 25L);

        messages = wiser.getMessages();
        assertEquals(1, messages.size());

        WiserMessage message = messages.get(0);
        EmailServiceTaskTest.assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList(
                "kermit@activiti.org"), null);
    }

}
