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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.mail.internet.MimeMultipart;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.content.api.ContentEngineConfigurationApi;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.subethamail.wiser.WiserMessage;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
@MockitoSettings
public class EmailSendTaskWithContentItemsTest extends EmailTestCase {

    @Mock
    protected ContentEngineConfigurationApi contentEngineConfiguration;

    @Mock
    protected ContentService contentService;

    @BeforeEach
    void initializeMocks() {
        Map engineConfigurations = processEngineConfiguration.getEngineConfigurations();
        engineConfigurations.put(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG, contentEngineConfiguration);
    }

    @AfterEach
    void resetMocks() {
        processEngineConfiguration.getEngineConfigurations().remove(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskWithContentItemsTest.sendMailWithContentItem.bpmn20.xml")
    public void testSendMailWithContentItemAttachment(@Mock ContentItem attachment) throws Exception {

        when(contentEngineConfiguration.getContentService()).thenReturn(contentService);
        when(attachment.getId()).thenReturn("contentId");
        when(attachment.getName()).thenReturn("myDocument.txt");
        when(attachment.getMimeType()).thenReturn("text/plain");
        when(contentService.getContentItemData("contentId"))
                .thenAnswer(invocation -> new ByteArrayInputStream("This is an attachment".getBytes(UTF_8)));

        processEngine.getRuntimeService()
                .createProcessInstanceBuilder()
                .processDefinitionKey("testSendEmailWithAttachment")
                .transientVariable("attachment", attachment)
                .start();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);

        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        String attachmentFileName = mm.getBodyPart(1).getDataHandler().getName();
        assertThat(attachmentFileName).isEqualTo("myDocument.txt");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/mail/EmailSendTaskWithContentItemsTest.sendMailWithContentItem.bpmn20.xml")
    public void testSendMailWithContentItemAttachments(
            @Mock ContentItem attachment1,
            @Mock ContentItem attachment2,
            @Mock ContentItem attachment3

    ) throws Exception {

        when(attachment1.getId()).thenReturn("content1");
        when(attachment1.getName()).thenReturn("myDocument.txt");
        when(attachment1.getMimeType()).thenReturn("text/plain");

        when(attachment2.getId()).thenReturn("content2");
        when(attachment2.getName()).thenReturn("anotherDocument.docx");
        when(attachment2.getMimeType()).thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        when(attachment3.getId()).thenReturn("content3");
        when(attachment3.getName()).thenReturn("andYetAnother.pdf");
        when(attachment3.getMimeType()).thenReturn("application/pdf");

        when(contentEngineConfiguration.getContentService()).thenReturn(contentService);
        when(contentService.getContentItemData(any()))
                .thenAnswer(invocation -> new ByteArrayInputStream("This is an attachment".getBytes(UTF_8)));

        List<ContentItem> attachments = List.of(attachment1, attachment2, attachment3);
        processEngine.getRuntimeService()
                .createProcessInstanceBuilder()
                .processDefinitionKey("testSendEmailWithAttachment")
                .transientVariable("attachment", attachments)
                .start();

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);

        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();

        Set<String> contentTypes = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < mm.getCount(); i++) {
            String contentTypeHeader = mm.getBodyPart(i).getHeader("Content-Type")[0];

            if (contentTypeHeader.contains("name")) {
                contentTypeHeader = contentTypeHeader
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("\t", "");

                int index = contentTypeHeader.indexOf("name=");
                int semicolonIndex = contentTypeHeader.indexOf(";");
                contentTypes.add(contentTypeHeader.substring(0, Math.min(semicolonIndex, index)));
                names.add(contentTypeHeader.substring(index + 5));
            }
        }

        assertThat(names).containsExactlyInAnyOrder("myDocument.txt", "anotherDocument.docx", "andYetAnother.pdf");
        assertThat(contentTypes)
                .containsExactlyInAnyOrder("text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf");
    }
}
