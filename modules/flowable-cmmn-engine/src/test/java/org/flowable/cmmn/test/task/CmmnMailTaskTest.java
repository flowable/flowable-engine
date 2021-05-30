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
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang3.Validate;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * @author Joram Barrez
 */
@Tag("email")
public class CmmnMailTaskTest extends FlowableCmmnTestCase {

    protected static Wiser wiser;

    @BeforeClass
    public static void setupWiser() throws Exception {
        wiser = new Wiser();
        wiser.setPort(5025);

        int counter = 0;
        boolean serverUpAndRunning = false;
        while (!serverUpAndRunning && counter++ < 11) {

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

    @Before
    public void resetMessages() {
        wiser.getMessages().clear();
    }

    @AfterClass
    public static void stopWiser() {
        wiser.stop();
    }

    @Test
    @CmmnDeployment
    public void testSimpleTextMail() throws Exception {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleTextMail").start();
        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello!", "This is a test", "flowable@localhost", Collections.singletonList("test@flowable.org"), null);
        assertThat(message.getMimeMessage().getContentType()).isEqualTo("text/plain; charset=us-ascii");

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnMailTaskTest.testSimpleTextMail.cmmn")
    public void testSimpleTextMailCharset() throws Exception {
        Charset originalCharset = cmmnEngineConfiguration.getMailServerDefaultCharset();

        try {
            cmmnEngineConfiguration.setMailServerDefaultCharset(StandardCharsets.UTF_8);
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testSimpleTextMail")
                    .start();

            List<WiserMessage> messages = wiser.getMessages();
            assertThat(messages).hasSize(1);

            WiserMessage message = messages.get(0);
            assertThat(message.getMimeMessage().getContentType()).isEqualTo("text/plain; charset=UTF-8");
        } finally {
            cmmnEngineConfiguration.setMailServerDefaultCharset(originalCharset);
        }
    }


    @Test
    @CmmnDeployment
    public void testSimpleTextMailMultipleRecipients() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMail").start();
        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(3);

        List<String> recipients = new ArrayList<>();
        for (WiserMessage message : messages) {
            recipients.add(message.getEnvelopeReceiver());
        }
        assertThat(recipients)
                .contains("one@flowable.org", "two@flowable.org", "three@flowable.org");
    }

    @Test
    @CmmnDeployment
    public void testTextMailExpressions() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testMail")
            .variable("toVar", "to@flowable.org")
            .variable("fromVar", "from@flowable.org")
            .variable("ccVar", "cc@flowable.org")
            .variable("bccVar", "bcc@flowable.org")
            .variable("subjectVar", "Testing")
            .variable("bodyVar", "The test body")
            .start();

        List<WiserMessage> messages = wiser.getMessages();

        assertEmailSend(messages.get(0), false, "Hello Testing", "The test body",
            "from@flowable.org",
            Collections.singletonList("to@flowable.org"),
            Collections.singletonList("cc@flowable.org"));

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("from@flowable.org", "to@flowable.org"),
                tuple("from@flowable.org", "cc@flowable.org"),
                tuple("from@flowable.org", "bcc@flowable.org")
            );

    }
    
    @Test
    @CmmnDeployment
    public void testCcBccWithoutTo() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testCcBccWithoutToMail")
            .start();

        List<WiserMessage> messages = wiser.getMessages();

        assertEmailSend(messages.get(0), false, "Hello!", "This is a test",
            "flowable@localhost",
            null,
            Collections.singletonList("cc@flowable.org"));

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "cc@flowable.org"),
                tuple("flowable@localhost", "bcc@flowable.org")
            );

    }

    @Test
    @CmmnDeployment
    public void testHtmlMailWithFileAttachment() throws Exception {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testMail")
            .variable("attachmentsBean", new TestAttachmentBean())
            .variable("gender", "male")
            .start();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);

        assertEmailSend(message, true, "Hello", "Mr. <b>Kermit</b>", "test@flowable.org", Collections.singletonList("test@flowable.org"), null);

        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        assertThat(mm.getCount()).isEqualTo(2);
        String attachmentFileName = mm.getBodyPart(1).getDataHandler().getName();
        assertThat(attachmentFileName).isEqualTo(new TestAttachmentBean().getFile().getName());
    }

    // Helper

    private void assertEmailSend(WiserMessage emailMessage, boolean htmlMail, String subject, String message, String from,
            List<String> to, List<String> cc) {
        try {
            MimeMessage mimeMessage = emailMessage.getMimeMessage();

            if (htmlMail) {
                assertThat(mimeMessage.getContentType()).contains("multipart/mixed");
            } else {
                assertThat(mimeMessage.getContentType()).contains("text/plain");
            }

            assertThat(mimeMessage.getHeader("Subject", null)).isEqualTo(subject);
            assertThat(mimeMessage.getHeader("From", null)).isEqualTo(from);
            assertThat(getMessage(mimeMessage).contains(message));

            if (to != null) {
                for (String t : to) {
                    assertThat(mimeMessage.getHeader("To", null).contains(t));
                }
            }

            if (cc != null) {
                for (String c : cc) {
                    assertThat(mimeMessage.getHeader("Cc", null).contains(c));
                }
            }

        } catch (MessagingException e) {
            fail(e.getMessage());
        }

    }

    protected String getMessage(MimeMessage mimeMessage) {
        try {
            DataHandler dataHandler = mimeMessage.getDataHandler();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dataHandler.writeTo(baos);
            baos.flush();
            return baos.toString();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get message", e);
        }
    }

    public static class TestAttachmentBean implements Serializable {
        public File getFile() {
            String fileName = "src/test/resources/org/flowable/cmmn/test/task/CmmnMailTaskTest.testTextMailExpressions.cmmn";
            File file = new File(fileName);
            Validate.isTrue(file.exists(), "file <" + fileName + "> does not exist ");
            return file;
        }
    }

}
