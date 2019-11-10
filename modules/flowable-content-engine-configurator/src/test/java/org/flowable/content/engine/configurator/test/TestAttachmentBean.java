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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.impl.util.CommandContextUtil;

public class TestAttachmentBean {

    public static List<String> TEST_NAMES = Arrays.asList("myDocument.txt", "anotherDocument.docx", "andYetAnother.pdf");
    public static List<String> TEST_MIME_TYPES = Arrays.asList("text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf");

    public ContentItem getAttachment() {
        ContentService contentService = CommandContextUtil.getContentService();
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("myDocument.txt");
        contentItem.setMimeType("text/plain");
        contentService.saveContentItem(contentItem, new ByteArrayInputStream("This is an attachment".getBytes(UTF_8)));
        return contentItem;
    }

    public List<ContentItem> getAttachments() {
        ContentService contentService = CommandContextUtil.getContentService();
        List<ContentItem> contentItems = new ArrayList<>();

        for (int i = 0; i < TEST_NAMES.size(); i++) {
            ContentItem contentItem = contentService.newContentItem();
            contentItem.setName(TEST_NAMES.get(i));
            contentItem.setMimeType(TEST_MIME_TYPES.get(i));
            contentService.saveContentItem(contentItem, new ByteArrayInputStream("This is an attachment".getBytes(UTF_8)));
            contentItems.add(contentItem);
        }

        return contentItems;
    }

}
