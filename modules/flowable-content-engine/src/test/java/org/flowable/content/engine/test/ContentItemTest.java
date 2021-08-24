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
package org.flowable.content.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentItem;
import org.junit.Test;

public class ContentItemTest extends AbstractFlowableContentTest {

    @Test
    public void createSimpleProcessContentItemNoData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");
        contentItem.setProcessInstanceId("123456");
        contentService.saveContentItem(contentItem);

        assertThat(contentItem.getId()).isNotNull();

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertThat(dbContentItem).isNotNull();
        assertThat(dbContentItem.getId()).isEqualTo(contentItem.getId());

        contentService.deleteContentItem(contentItem.getId());
    }

    @Test
    public void createSimpleProcessContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setProcessInstanceId("123456");
        assertCreateContentWithData(contentItem, "process-instance-content");
    }

    @Test
    public void createSimpleTaskContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setTaskId("123456");
        assertCreateContentWithData(contentItem, "task-content");
    }

    @Test
    public void createSimpleCaseContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setScopeId("123456");
        contentItem.setScopeType("cmmn");
        assertCreateContentWithData(contentItem, "cmmn");
    }

    @Test
    public void createSimpleNewTypeContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setScopeId("123456");
        contentItem.setScopeType("newType");
        assertCreateContentWithData(contentItem, "newType");
    }

    @Test
    public void createSimpleUncategorizedTypeContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setScopeId("123456");
        contentItem.setScopeType("uncategorizedNewType");
        assertCreateContentWithData(contentItem, "uncategorizedNewType");
    }

    @Test
    public void createSimpleUncategorizedContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setScopeId("123456");
        contentItem.setScopeType(null);
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("test.txt")) {
            contentService.saveContentItem(contentItem, in);
        }

        assertThat(contentItem.getId()).isNotNull();
        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
        )).exists();

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertThat(dbContentItem).isNotNull();
        assertThat(dbContentItem.getId()).isEqualTo(contentItem.getId());

        try (InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, StandardCharsets.UTF_8);
            assertThat(contentValue).isEqualTo("hello");
        }

        contentService.deleteContentItem(contentItem.getId());

        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
        )).doesNotExist();

        assertThatThrownBy(() -> contentService.getContentItemData(contentItem.getId()))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }
    
    @Test
    public void updateSimpleUncategorizedContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setScopeId("123456");
        contentItem.setScopeType(null);
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("test.txt")) {
            contentService.saveContentItem(contentItem, in);
        }

        assertThat(contentItem.getId()).isNotNull();
        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
        )).exists();
        
        String contentStoreId = contentItem.getContentStoreId();

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("test2.txt")) {
            contentService.saveContentItem(contentItem, in);
        }
        
        assertThat(contentService.createContentItemQuery().id(contentItem.getId()).singleResult().getContentStoreId()).isEqualTo(contentStoreId);

        try (InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, StandardCharsets.UTF_8);
            assertThat(contentValue).isEqualTo("hello2");
        }

        contentService.deleteContentItem(contentItem.getId());

        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
        )).doesNotExist();

        assertThatThrownBy(() -> contentService.getContentItemData(contentItem.getId()))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    public void createSimpleUncategorizedContentItemWithoutIdWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setScopeId(null);
        contentItem.setScopeType(null);
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");

        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("test.txt")) {
            contentService.saveContentItem(contentItem, in);
        }

        assertThat(contentItem.getId()).isNotNull();
        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
        )).exists();

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertThat(dbContentItem).isNotNull();
        assertThat(dbContentItem.getId()).isEqualTo(contentItem.getId());

        try (InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, StandardCharsets.UTF_8);
            assertThat(contentValue).isEqualTo("hello");
        }

        contentService.deleteContentItem(contentItem.getId());

        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
        )).doesNotExist();

        assertThatThrownBy(() -> contentService.getContentItemData(contentItem.getId()))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    public void createAndDeleteUncategorizedContentTwice() throws Exception {
        createSimpleUncategorizedContentItemWithoutIdWithData();
        createSimpleUncategorizedContentItemWithoutIdWithData();
    }

    protected void assertCreateContentWithData(ContentItem contentItem, String typeDirectory) throws IOException {
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("test.txt")) {
            contentService.saveContentItem(contentItem, in);
        }

        assertThat(contentItem.getId()).isNotNull();
        assertFileExists(typeDirectory, "123456", contentItem.getContentStoreId());

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertThat(dbContentItem).isNotNull();
        assertThat(dbContentItem.getId()).isEqualTo(contentItem.getId());

        try (InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, StandardCharsets.UTF_8);
            assertThat(contentValue).isEqualTo("hello");
        }

        contentService.deleteContentItem(contentItem.getId());

        assertMissingFile(typeDirectory, "123456", contentItem.getContentStoreId());

        assertThatThrownBy(() -> contentService.getContentItemData(contentItem.getId()))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    protected void assertFileExists(String typeFolder, String idFolder, String contentStoreId) {
        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + typeFolder
                + File.separator + idFolder + File.separator +
                contentStoreId.substring(contentStoreId.lastIndexOf('.') + 1)
        )).exists();
    }

    protected void assertMissingFile(String typeFolder, String idFolder, String contentStoreId) {
        assertThat(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + typeFolder
                + File.separator + idFolder + File.separator +
                contentStoreId.substring(contentStoreId.lastIndexOf('.') + 1)
        )).doesNotExist();
    }

    @Test
    public void queryContentItemWithScopeId() {
        createContentItem();
        assertThat(contentService.createContentItemQuery().scopeId("testScopeId").singleResult().getName()).isEqualTo("testScopeItem");
        assertThat(contentService.createContentItemQuery().scopeIdLike("testScope%").singleResult().getName()).isEqualTo("testScopeItem");
        contentService.deleteContentItemsByScopeIdAndScopeType("testScopeId", "testScopeType");
    }

    @Test
    public void queryContentItemWithScopeType() {
        createContentItem();
        assertThat(contentService.createContentItemQuery().scopeType("testScopeType").singleResult().getName()).isEqualTo("testScopeItem");
        assertThat(contentService.createContentItemQuery().scopeTypeLike("testScope%").singleResult().getName()).isEqualTo("testScopeItem");
        contentService.deleteContentItemsByScopeIdAndScopeType("testScopeId", "testScopeType");
    }

    @Test
    public void insertAll() {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testScopeItem1");
        contentItem.setMimeType("application/pdf");
        contentItem.setScopeType("testScopeType");
        contentItem.setScopeId("testScopeId");
        contentService.saveContentItem(contentItem);

        ContentItem contentItem2 = contentService.newContentItem();
        contentItem2.setName("testScopeItem1");
        contentItem2.setMimeType("application/pdf");
        contentItem2.setScopeType("testScopeType");
        contentItem2.setScopeId("testScopeId");
        contentService.saveContentItem(contentItem2);

        assertThat(contentService.createContentItemQuery().scopeTypeLike("testScope%").list()).hasSize(2);

        contentService.deleteContentItemsByScopeIdAndScopeType("testScopeId", "testScopeType");

        assertThat(contentService.createContentItemQuery().scopeTypeLike("testScope%").list()).isEmpty();
    }

    @Test
    public void lastModifiedTimestampUpdateOnContentChange() throws IOException {
        ContentItem initialContentItem = contentService.newContentItem();
        initialContentItem.setName("testItem");
        initialContentItem.setMimeType("text/plain");
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Instant createTime = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(153);
        Date createDate = Date.from(createTime);
        contentEngineConfiguration.getClock().setCurrentTime(createDate);
        contentService.saveContentItem(initialContentItem);
        assertThat(initialContentItem.getId()).isNotNull();
        assertThat(initialContentItem.getLastModified()).isEqualTo(createDate);

        ContentItem storedContentItem = contentService.createContentItemQuery().id(initialContentItem.getId()).singleResult();
        assertThat(storedContentItem).isNotNull();
        assertThat(storedContentItem.getId()).isEqualTo(initialContentItem.getId());
        assertThat(initialContentItem.getLastModified()).isEqualTo(storedContentItem.getLastModified());

        Date updateDate = Date.from(createTime.plusSeconds(557));
        contentEngineConfiguration.getClock().setCurrentTime(updateDate);
        contentService.saveContentItem(storedContentItem, this.getClass().getClassLoader().getResourceAsStream("test.txt"));
        storedContentItem = contentService.createContentItemQuery().id(initialContentItem.getId()).singleResult();
        assertThat(storedContentItem).isNotNull();
        assertThat(storedContentItem.getId()).isEqualTo(initialContentItem.getId());
        InputStream contentStream = contentService.getContentItemData(initialContentItem.getId());
        String contentValue = IOUtils.toString(contentStream, StandardCharsets.UTF_8);
        assertThat(contentValue).isEqualTo("hello");

        assertThat(initialContentItem.getLastModified()).isNotEqualTo(storedContentItem.getLastModified());
        assertThat(storedContentItem.getLastModified()).isEqualTo(updateDate);

        contentService.deleteContentItem(initialContentItem.getId());
    }

    protected void createContentItem() {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testScopeItem");
        contentItem.setMimeType("application/pdf");
        contentItem.setScopeType("testScopeType");
        contentItem.setScopeId("testScopeId");
        contentService.saveContentItem(contentItem);
    }
}
