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
package org.flowable.content.engine.configurator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeMultipart;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * @author Joram Barrez
 */
public class ContentEngineConfiguratorTest {

    protected static ProcessEngine processEngine;
    protected static ContentEngineConfiguration contentEngineConfiguration;
    protected static ContentService contentService;

    protected static Wiser wiser;

    @BeforeClass
    public static void setup() throws Exception{
        if (processEngine == null) {
            processEngine = ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("flowable.cfg.xml")
                .buildProcessEngine();
            contentEngineConfiguration = (ContentEngineConfiguration) processEngine.getProcessEngineConfiguration()
                .getEngineConfigurations().get(EngineConfigurationConstants.KEY_CONTENT_ENGINE_CONFIG);
            contentService = contentEngineConfiguration.getContentService();
        }

        wiser = new Wiser();
        wiser.setPort(5025);
        try {
            wiser.start();
        } catch (RuntimeException e) { // Fix for slow port-closing Jenkins
            if (e.getMessage().toLowerCase().contains("bindexception")) {
                Thread.sleep(250L);
            }
        }
    }

    @AfterClass
    public static void stopWiser() {
        wiser.stop();
    }

    @After
    public void cleanup() {
        contentService.createContentItemQuery().list().forEach(c -> contentService.deleteContentItem(c.getId()));
        processEngine.getRepositoryService().createDeploymentQuery().list()
            .forEach(d -> processEngine.getRepositoryService().deleteDeployment(d.getId(), true));

        wiser.getMessages().clear();
    }

    @Test
    public void testSendMailWithContentItemAttachment() throws Exception {

        processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("org/flowable/content/engine/configurator/test/ContentEngineConfiguratorTest.testSendMailWithContentItemAttachment.bpmn20.xml")
            .deploy();

        processEngine.getRuntimeService().startProcessInstanceByKey("testSendEmailWithAttachment");

        List<WiserMessage> messages = wiser.getMessages();
        assertThat(messages).hasSize(1);
        WiserMessage message = messages.get(0);

        MimeMultipart mm = (MimeMultipart) message.getMimeMessage().getContent();
        String attachmentFileName = mm.getBodyPart(1).getDataHandler().getName();
        assertThat(attachmentFileName).isEqualTo("myDocument.txt");
    }

    @Test
    public void testSendMailWithContentItemAttachments() throws Exception {

        processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("org/flowable/content/engine/configurator/test/ContentEngineConfiguratorTest.testSendMailWithContentItemAttachments.bpmn20.xml")
            .deploy();

        processEngine.getRuntimeService().startProcessInstanceByKey("testSendEmailWithAttachments");

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

        assertThat(names).containsAll(TestAttachmentBean.TEST_NAMES);
        assertThat(contentTypes).containsAll(TestAttachmentBean.TEST_MIME_TYPES);
    }


}
