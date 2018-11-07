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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentItem;
import org.junit.Ignore;
import org.junit.Test;

public class ContentItemTest extends AbstractFlowableContentTest {

    @Test
    public void createSimpleProcessContentItemNoData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");
        contentItem.setProcessInstanceId("123456");
        contentService.saveContentItem(contentItem);

        assertNotNull(contentItem.getId());

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertNotNull(dbContentItem);
        assertEquals(contentItem.getId(), dbContentItem.getId());

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

        assertNotNull(contentItem.getId());
        assertTrue(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
            ).exists()
        );

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertNotNull(dbContentItem);
        assertEquals(contentItem.getId(), dbContentItem.getId());

        try (InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, "utf-8");
            assertEquals("hello", contentValue);
        }

        contentService.deleteContentItem(contentItem.getId());

        assertFalse(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
            ).exists()
        );
        try {
            contentService.getContentItemData(contentItem.getId());
            fail("Expected not found exception");

        } catch (FlowableObjectNotFoundException e) {
            // expected

        } catch (Exception e) {
            fail("Expected not found exception, not " + e);
        }

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

        assertNotNull(contentItem.getId());
        assertTrue(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
            ).exists()
        );

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertNotNull(dbContentItem);
        assertEquals(contentItem.getId(), dbContentItem.getId());

        try (InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, "utf-8");
            assertEquals("hello", contentValue);
        }

        contentService.deleteContentItem(contentItem.getId());

        assertFalse(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + "uncategorized" + File.separator +
                contentItem.getContentStoreId().substring(contentItem.getContentStoreId().lastIndexOf('.') + 1)
            ).exists()
        );
        try {
            contentService.getContentItemData(contentItem.getId());
            fail("Expected not found exception");

        } catch (FlowableObjectNotFoundException e) {
            // expected

        } catch (Exception e) {
            fail("Expected not found exception, not " + e);
        }

    }

    @Test
    public void createAndDeleteUncategorizedContentTwice() throws Exception {
        createSimpleUncategorizedContentItemWithoutIdWithData();
        createSimpleUncategorizedContentItemWithoutIdWithData();
    }

    protected void assertCreateContentWithData(ContentItem contentItem, String typeDirectory) throws IOException {
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");
        try(InputStream in = this.getClass().getClassLoader().getResourceAsStream("test.txt")) {
            contentService.saveContentItem(contentItem, in);
        }

        assertNotNull(contentItem.getId());
        assertFileExists(typeDirectory, "123456", contentItem.getContentStoreId());

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertNotNull(dbContentItem);
        assertEquals(contentItem.getId(), dbContentItem.getId());

        try(InputStream contentStream = contentService.getContentItemData(contentItem.getId())) {
            String contentValue = IOUtils.toString(contentStream, "utf-8");
            assertEquals("hello", contentValue);
        }

        contentService.deleteContentItem(contentItem.getId());

        assertMissingFile(typeDirectory, "123456", contentItem.getContentStoreId());
        try {
            contentService.getContentItemData(contentItem.getId());
            fail("Expected not found exception");

        } catch (FlowableObjectNotFoundException e) {
            // expected

        } catch (Exception e) {
            fail("Expected not found exception, not " + e);
        }
    }

    protected void assertFileExists(String typeFolder, String idFolder, String contentStoreId) {
        assertTrue(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + typeFolder
                + File.separator + idFolder +File.separator+
                contentStoreId.substring(contentStoreId.lastIndexOf('.') + 1)
            ).exists()
        );
    }

    protected void assertMissingFile(String typeFolder, String idFolder, String contentStoreId) {
        assertFalse(new File(contentEngineConfiguration.getContentRootFolder() + File.separator + typeFolder
                + File.separator + idFolder +File.separator+
                contentStoreId.substring(contentStoreId.lastIndexOf('.') + 1)
            ).exists()
        );
    }

    @Test
    public void queryContentItemWithScopeId() {
        createContentItem();
        assertEquals("testScopeItem", contentService.createContentItemQuery().scopeId("testScopeId").singleResult().getName());
        assertEquals("testScopeItem", contentService.createContentItemQuery().scopeIdLike("testScope%").singleResult().getName());
        contentService.deleteContentItemsByScopeIdAndScopeType("testScopeId", "testScopeType");
    }

    @Test
    public void queryContentItemWithScopeType() {
        createContentItem();
        assertEquals("testScopeItem", contentService.createContentItemQuery().scopeType("testScopeType").singleResult().getName());
        assertEquals("testScopeItem", contentService.createContentItemQuery().scopeTypeLike("testScope%").singleResult().getName());
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

        assertEquals(2, contentService.createContentItemQuery().scopeTypeLike("testScope%").list().size());

        contentService.deleteContentItemsByScopeIdAndScopeType("testScopeId", "testScopeType");

        assertEquals(0, contentService.createContentItemQuery().scopeTypeLike("testScope%").list().size());
    }

    @Test
    public void lastModifiedTimestampUpdateOnContentChange() throws IOException {
        ContentItem initialContentItem = contentService.newContentItem();
        initialContentItem.setName("testItem");
        initialContentItem.setMimeType("text/plain");
        contentService.saveContentItem(initialContentItem);
        long initialTS = System.currentTimeMillis();
        assertNotNull(initialContentItem.getId());
        assertNotNull(initialContentItem.getLastModified());
        assertTrue(initialContentItem.getLastModified().getTime() <= System.currentTimeMillis());

        ContentItem storedContentItem = contentService.createContentItemQuery().id(initialContentItem.getId()).singleResult();
        assertNotNull(storedContentItem);
        assertEquals(initialContentItem.getId(), storedContentItem.getId());
        assertTrue(initialContentItem.getLastModified().getTime() == storedContentItem.getLastModified().getTime());

        long storeTS = System.currentTimeMillis();
        contentService.saveContentItem(storedContentItem, this.getClass().getClassLoader().getResourceAsStream("test.txt"));
        storedContentItem = contentService.createContentItemQuery().id(initialContentItem.getId()).singleResult();
        assertNotNull(storedContentItem);
        assertEquals(initialContentItem.getId(), storedContentItem.getId());
        InputStream contentStream = contentService.getContentItemData(initialContentItem.getId());
        String contentValue = IOUtils.toString(contentStream, "utf-8");
        assertEquals("hello", contentValue);

        assertTrue(initialContentItem.getLastModified().getTime() < storedContentItem.getLastModified().getTime());

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
