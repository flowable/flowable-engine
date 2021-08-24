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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;

/**
 * @author Joram Barrez
 * @author Tim Stephenson
 */
public class EmailServiceTaskTest extends EmailTestCase {

    @Test
    @Deployment
    public void testSimpleTextMail() throws Exception {
        String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly").getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList("kermit@activiti.org"), null);
        assertThat(message.getMimeMessage().getContentType()).isEqualTo("text/plain; charset=us-ascii");
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailServiceTaskTest.testSimpleTextMail.bpmn20.xml")
    public void testSimpleTextMailCharset() throws Exception {
        Charset originalCharset = processEngineConfiguration.getMailServerDefaultCharset();

        try {
            processEngineConfiguration.setMailServerDefaultCharset(StandardCharsets.UTF_8);
            String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly").getId();

            List<WiserMessage> messages = wiser.getMessages();
            assertThat(messages).hasSize(1);

            WiserMessage message = messages.get(0);
            assertThat(message.getMimeMessage().getContentType()).isEqualTo("text/plain; charset=UTF-8");
        } finally {
            processEngineConfiguration.setMailServerDefaultCharset(originalCharset);
        }
    }

    @Test
    @Deployment
    public void testSkipExpression() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("skip", true);
        varMap.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly", varMap).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).isEmpty();

        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/mail/EmailServiceTaskTest.testSkipExpression.bpmn20.xml")
    public void testSkipExpressionEnabledFalse() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("skip", true);
        varMap.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", false);
        String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly", varMap).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList("kermit@flowable.org"), null);
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/mail/EmailServiceTaskTest.testSkipExpression.bpmn20.xml")
    public void testSkipExpressionSkipFalse() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("skip", false);
        varMap.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        String procId = runtimeService.startProcessInstanceByKey("simpleTextOnly", varMap).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList("kermit@flowable.org"), null);
        assertProcessEnded(procId);
    }

    @Test
    public void testSimpleTextMailWhenMultiTenant() throws Exception {
        String tenantId = "myEmailTenant";

        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml")
                .tenantId(tenantId).deploy();
        String procId = runtimeService.startProcessInstanceByKeyAndTenantId("simpleTextOnly", tenantId).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "activiti@myTenant.com", Collections.singletonList(
                "kermit@activiti.org"), null);
        assertProcessEnded(procId);

        deleteDeployments();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml", tenantId = "forceToEmailTenant")
    public void testSimpleTextMailWhenMultiTenantWithForceTo() throws Exception {
        String tenantId = "forceToEmailTenant";
        addMailServer(tenantId, "flowable@myTenant.com", "no-reply@myTenant.com");

        String procId = runtimeService.startProcessInstanceByKeyAndTenantId("simpleTextOnly", tenantId).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@myTenant.com", Collections.singletonList(
            "no-reply@myTenant.com"), null);

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@myTenant.com", "no-reply@myTenant.com")
            );

        assertProcessEnded(procId);
    }

    @Test
    public void testSimpleTextMailForNonExistentTenant() throws Exception {
        String tenantId = "nonExistentTenant";

        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml")
                .tenantId(tenantId).deploy();
        String procId = runtimeService.startProcessInstanceByKeyAndTenantId("simpleTextOnly", tenantId).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList(
                "kermit@activiti.org"), null);
        assertProcessEnded(procId);

        deleteDeployments();
    }

    @Test
    public void testSimpleTextMailForNonExistentTenantWithForceTo() throws Exception {
        processEngineConfiguration.setMailServerForceTo("no-reply@flowable.org");
        String tenantId = "nonExistentTenant";

        repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMail.bpmn20.xml")
            .tenantId(tenantId).deploy();
        String procId = runtimeService.startProcessInstanceByKeyAndTenantId("simpleTextOnly", tenantId).getId();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList(
            "no-reply@flowable.org"), null);
        assertProcessEnded(procId);

        deleteDeployments();
    }

    @Test
    @Deployment
    public void testSimpleTextMailMultipleRecipients() {
        runtimeService.startProcessInstanceByKey("simpleTextOnlyMultipleRecipients");

        // 3 recipients == 3 emails in wiser with different receivers
        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages)
                .extracting(WiserMessage::getEnvelopeReceiver)
                .containsExactlyInAnyOrder("fozzie@activiti.org", "kermit@activiti.org", "mispiggy@activiti.org");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailServiceTaskTest.testSimpleTextMailMultipleRecipients.bpmn20.xml")
    public void testSimpleTextMailMultipleRecipientsAndForceTo() {
        processEngineConfiguration.setMailServerForceTo("no-reply@flowable.org, no-reply2@flowable.org");
        runtimeService.startProcessInstanceByKey("simpleTextOnlyMultipleRecipients");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "no-reply@flowable.org"),
                tuple("flowable@localhost", "no-reply2@flowable.org")
            );
    }

    @Test
    @Deployment
    public void testTextMailExpressions() throws Exception {

        String sender = "mispiggy@activiti.org";
        String recipient = "fozziebear@activiti.org";
        String recipientName = "Mr. Fozzie";
        String subject = "Fozzie, you should see this!";

        Map<String, Object> vars = new HashMap<>();
        vars.put("sender", sender);
        vars.put("recipient", recipient);
        vars.put("recipientName", recipientName);
        vars.put("subject", subject);

        runtimeService.startProcessInstanceByKey("textMailExpressions", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, subject, "Hello " + recipientName + ", this is an e-mail", sender, Collections.singletonList(
                recipient), null);
    }

    @Test
    @Deployment
    public void testOnlyBccAddress() throws Exception {
        runtimeService.startProcessInstanceByKey("onlyBccAddress");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", Collections.emptyList(),
            Collections.emptyList());

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "mispiggy@activiti.org")
            );
    }

    @Test
    @Deployment
    public void testOnlyCcAddress() throws Exception {
        runtimeService.startProcessInstanceByKey("onlyCcAddress");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", Collections.emptyList(),
                Collections.singletonList("mispiggy@activiti.org"));

        assertThat(messages)
                .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
                .containsExactlyInAnyOrder(
                        tuple("flowable@localhost", "mispiggy@activiti.org")
                );
    }

    @Test
    @Deployment
    public void testOnlyToAddress() throws Exception {
        runtimeService.startProcessInstanceByKey("onlyToAddress");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", Collections.singletonList("mispiggy@activiti.org"),
                Collections.emptyList());

        assertThat(messages)
                .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
                .containsExactlyInAnyOrder(
                        tuple("flowable@localhost", "mispiggy@activiti.org")
                );
    }

    @Test
    @Deployment
    public void testCcAndBcc() throws Exception {
        runtimeService.startProcessInstanceByKey("ccAndBcc");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", Collections.singletonList(
                "kermit@activiti.org"), Collections.singletonList("fozzie@activiti.org"));

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "kermit@activiti.org"),
                tuple("flowable@localhost", "fozzie@activiti.org"),
                tuple("flowable@localhost", "mispiggy@activiti.org")
            );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailServiceTaskTest.testCcAndBcc.bpmn20.xml")
    public void testCcAndBccWithForceTo() throws Exception {
        processEngineConfiguration.setMailServerForceTo("no-reply@flowable");
        runtimeService.startProcessInstanceByKey("ccAndBcc");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", Collections.singletonList("no-reply@flowable"),
            Collections.singletonList("no-reply@flowable"));

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "no-reply@flowable"),
                tuple("flowable@localhost", "no-reply@flowable"),
                tuple("flowable@localhost", "no-reply@flowable")
            );
    }

    @Test
    @Deployment
    public void testHtmlMail() throws Exception {
        runtimeService.startProcessInstanceByKey("htmlMail", CollectionUtil.singletonMap("gender", "male"));

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertEmailSend(messages.get(0), true, "Test", "Mr. <b>Kermit</b>", "flowable@localhost", Collections.singletonList(
                "kermit@activiti.org"), null);
    }

    @Test
    @Deployment
    public void testHtmlMailWithGetOrDefaultVariableFunctions() throws Exception {
        runtimeService.startProcessInstanceByKey("htmlMail", CollectionUtil.singletonMap("gender", "male"));

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        String expectedMessage = "<html>\n"
                + "                <body>\n"
                + "                <ul>\n"
                + "                  <li><b>Currency:</b>unknown</li>\n"
                + "                  <li><b>Gender:</b>male</li>\n"
                + "                  </ul>\n"
                + "                </body>\n"
                + "              </html>";
        assertEmailSend(messages.get(0), true, "Test", expectedMessage, "flowable@localhost", Collections.singletonList(
                "kermit@flowable.org"), null);
    }

    @Test
    @Deployment
    public void testVariableTemplatedMail() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("gender", "male");
        vars.put("html", "<![CDATA[<html><body>Hello ${gender == 'male' ? 'Mr' : 'Ms' }. <b>Kermit</b><body></html>]]");
        runtimeService.startProcessInstanceByKey("variableTemplatedMail", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertEmailSend(messages.get(0), true, "Test", "Mr. <b>Kermit</b>", "flowable@localhost", Collections.singletonList(
                "kermit@activiti.org"), null);
    }

    @Test
    @Deployment
    public void testTextMailWithFileAttachment() throws Exception {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("attachmentsBean", new AttachmentsBean());
        runtimeService.startProcessInstanceByKey("textMailWithFileAttachment", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);
        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        assertThat(mm.getCount()).isEqualTo(2);
        String attachmentFileName = mm.getBodyPart(1).getDataHandler().getName();
        assertThat(attachmentFileName).isEqualTo(new AttachmentsBean().getFile().getName());
    }

    @Test
    @Deployment
    public void testTextMailWithFileAttachments() throws Exception {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("attachmentsBean", new AttachmentsBean());
        runtimeService.startProcessInstanceByKey("textMailWithFileAttachments", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);
        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        File[] files = new AttachmentsBean().getFiles();
        assertThat(mm.getCount()).isEqualTo(1 + files.length);
        for (int i = 0; i < files.length; i++) {
            String attachmentFileName = mm.getBodyPart(1 + i).getDataHandler().getName();
            assertThat(attachmentFileName).isEqualTo(files[i].getName());
        }
    }

    @Test
    @Deployment
    public void testTextMailWithFileAttachmentsByPath() throws Exception {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("attachmentsBean", new AttachmentsBean());
        runtimeService.startProcessInstanceByKey("textMailWithFileAttachmentsByPath", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);
        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        File[] files = new AttachmentsBean().getFiles();
        assertThat(mm.getCount()).isEqualTo(1 + files.length);
        for (int i = 0; i < files.length; i++) {
            String attachmentFileName = mm.getBodyPart(1 + i).getDataHandler().getName();
            assertThat(attachmentFileName).isEqualTo(files[i].getName());
        }
    }

    @Test
    @Deployment
    public void testTextMailWithDataSourceAttachment() throws Exception {
        String fileName = "file-name-to-be-displayed";
        String fileContent = "This is the file content";
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("attachmentsBean", new AttachmentsBean());
        vars.put("fileContent", fileContent);
        vars.put("fileName", fileName);
        runtimeService.startProcessInstanceByKey("textMailWithDataSourceAttachment", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);
        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        assertThat(mm.getCount()).isEqualTo(2);
        String attachmentFileName = mm.getBodyPart(1).getDataHandler().getName();
        assertThat(attachmentFileName).isEqualTo(fileName);
    }

    @Test
    @Deployment
    public void testTextMailWithNotExistingFileAttachment() throws Exception {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("attachmentsBean", new AttachmentsBean());
        runtimeService.startProcessInstanceByKey("textMailWithNotExistingFileAttachment", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);
        assertThat(message.getMimeMessage().getContent()).isNotExactlyInstanceOf(MimeMultipart.class);
    }

    @Test
    @Deployment
    public void testHtmlMailWithFileAttachment() throws Exception {
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("attachmentsBean", new AttachmentsBean());
        vars.put("gender", "male");
        runtimeService.startProcessInstanceByKey("htmlMailWithFileAttachment", vars);

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);
        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        assertThat(mm.getCount()).isEqualTo(2);
        String attachmentFileName = mm.getBodyPart(1).getDataHandler().getName();
        assertThat(attachmentFileName).isEqualTo(new AttachmentsBean().getFile().getName());
    }

    @Test
    @Deployment
    public void testInvalidAddress() throws Exception {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("invalidAddress").getId())
                .isInstanceOf(FlowableException.class)
                .as("Only a FlowableException is expected here");
    }

    @Test
    @Deployment
    public void testInvalidAddressWithoutException() throws Exception {
        String piId = runtimeService.startProcessInstanceByKey("invalidAddressWithoutException").getId();
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(piId).variableName("emailError").singleResult()).isNotNull();
        }
    }

    @Test
    @Deployment
    public void testInvalidAddressWithoutExceptionVariableName() throws Exception {
        String piId = runtimeService.startProcessInstanceByKey("invalidAddressWithoutException").getId();
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(piId).variableName("emailError").singleResult()).isNull();
        }
    }

    @Test
    @Deployment
    public void testMissingAnyRecipientAddress() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("missingAnyRecipientAddress"))
            .isInstanceOf(FlowableException.class)
            .hasMessage("No recipient could be found for sending email");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailServiceTaskTest.testMissingAnyRecipientAddress.bpmn20.xml")
    public void testMissingAnyRecipientAddressWithForceTo() {
        processEngineConfiguration.setMailServerForceTo("no-reply@flowable.org");
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("missingAnyRecipientAddress"))
            .isInstanceOf(FlowableException.class)
            .hasMessage("No recipient could be found for sending email");
    }

    // Helper

    public static void assertEmailSend(WiserMessage emailMessage, boolean htmlMail, String subject, String message, String from, List<String> to, List<String> cc) throws IOException {
        try {
            MimeMessage mimeMessage = emailMessage.getMimeMessage();
            if (htmlMail) {
                assertThat(mimeMessage.getContentType()).contains("multipart/mixed");
            } else {
                assertThat(mimeMessage.getContentType()).contains("text/plain");
            }

            assertThat(mimeMessage.getHeader("Subject", null)).isEqualTo(subject);
            assertThat(mimeMessage.getHeader("From", null)).isEqualTo(from);
            assertThat(getMessage(mimeMessage)).contains(message);

            for (String t : to) {
                assertThat(mimeMessage.getHeader("To", null)).contains(t);
            }

            if (cc != null) {
                for (String c : cc) {
                    assertThat(mimeMessage.getHeader("Cc", null)).contains(c);
                }
            }

        } catch (MessagingException e) {
            fail(e.getMessage());
        }

    }

    public static String getMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        DataHandler dataHandler = mimeMessage.getDataHandler();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataHandler.writeTo(baos);
        baos.flush();
        return baos.toString().replaceAll("\r\n", "\n");
    }

}
