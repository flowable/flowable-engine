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
 * @author Falko Menge
 */
public class EmailSendTaskTest extends EmailTestCase {

    @Test
    @Deployment
    public void testSimpleTextMail() throws Exception {
        runtimeService.startProcessInstanceByKey("simpleTextOnly");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);

        WiserMessage message = messages.get(0);
        assertEmailSend(message, false, "Hello Kermit!", "This a text only e-mail.", "flowable@localhost", Collections.singletonList("kermit@activiti.org"), null);
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
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testSimpleTextMailMultipleRecipients.bpmn20.xml")
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
        assertEmailSend(message, false, subject, "Hello " + recipientName + ", this is an e-mail", sender,
                Collections.singletonList(recipient), null);
    }

    @Test
    @Deployment
    public void testCcAndBcc() throws Exception {
        runtimeService.startProcessInstanceByKey("ccAndBcc");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", Collections.singletonList("kermit@activiti.org"),
                Collections.singletonList("fozzie@activiti.org"));

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "kermit@activiti.org"),
                tuple("flowable@localhost", "fozzie@activiti.org"),
                tuple("flowable@localhost", "mispiggy@activiti.org")
            );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testCcAndBcc.bpmn20.xml")
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
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testCcAndBccWithoutTo.bpmn20.xml")
    public void testCcAndBccWithoutTo() throws Exception {
        runtimeService.startProcessInstanceByKey("ccAndBccWithoutTo");

        List<WiserMessage> messages = wiser.getMessages();
        assertEmailSend(messages.get(0), false, "Hello world", "This is the content", "flowable@localhost", null,
            Collections.singletonList("fozzie@activiti.org"));

        assertThat(messages)
            .extracting(WiserMessage::getEnvelopeSender, WiserMessage::getEnvelopeReceiver)
            .containsExactlyInAnyOrder(
                tuple("flowable@localhost", "fozzie@activiti.org"),
                tuple("flowable@localhost", "mispiggy@activiti.org")
            );
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
    public void testHtmlMail() throws Exception {
        runtimeService.startProcessInstanceByKey("htmlMail", CollectionUtil.singletonMap("gender", "male"));

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        assertEmailSend(messages.get(0), true, "Test", "Mr. <b>Kermit</b>", "flowable@localhost", Collections.singletonList("kermit@activiti.org"), null);
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
    public void testMissingAnyRecipientAddress() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("missingAnyRecipientAddress"))
            .isInstanceOf(FlowableException.class)
            .hasMessage("No recipient could be found for sending email");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskTest.testMissingAnyRecipientAddress.bpmn20.xml")
    public void testMissingAnyRecipientAddressWithForceTo() {
        processEngineConfiguration.setMailServerForceTo("no-reply@flowable.org");
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("missingAnyRecipientAddress"))
            .isInstanceOf(FlowableException.class)
            .hasMessage("No recipient could be found for sending email");
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

    // Helper

    private void assertEmailSend(WiserMessage emailMessage, boolean htmlMail, String subject, String message, String from, List<String> to, List<String> cc) throws IOException {
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
            
            if(to != null) {
                for (String t : to) {
                    assertThat(mimeMessage.getHeader("To", null)).contains(t);
                }
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

    protected String getMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        DataHandler dataHandler = mimeMessage.getDataHandler();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataHandler.writeTo(baos);
        baos.flush();
        return baos.toString();
    }

}
