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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentItem;
import org.junit.Test;

public class ContentItemTest extends AbstractFlowableContentTest {

    @Test
    public void createSimpleContentItemNoData() throws Exception {
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
    public void createSimpleContentItemWithData() throws Exception {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");
        contentItem.setProcessInstanceId("123456");
        contentService.saveContentItem(contentItem, this.getClass().getClassLoader().getResourceAsStream("test.txt"));

        assertNotNull(contentItem.getId());

        ContentItem dbContentItem = contentService.createContentItemQuery().id(contentItem.getId()).singleResult();
        assertNotNull(dbContentItem);
        assertEquals(contentItem.getId(), dbContentItem.getId());

        InputStream contentStream = contentService.getContentItemData(contentItem.getId());
        String contentValue = IOUtils.toString(contentStream, "utf-8");
        assertEquals("hello", contentValue);

        contentService.deleteContentItem(contentItem.getId());

        try {
            contentStream = contentService.getContentItemData(contentItem.getId());
            fail("Expected not found exception");

        } catch (FlowableObjectNotFoundException e) {
            // expected

        } catch (Exception e) {
            fail("Expected not found exception, not " + e);
        }
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
    
    protected void createContentItem() {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testScopeItem");
        contentItem.setMimeType("application/pdf");
        contentItem.setScopeType("testScopeType");
        contentItem.setScopeId("testScopeId");
        contentService.saveContentItem(contentItem);
    }
}
