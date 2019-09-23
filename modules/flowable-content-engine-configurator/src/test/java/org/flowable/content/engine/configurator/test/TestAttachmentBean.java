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

        List<String> names = Arrays.asList("myDocument.txt", "anotherDocument.docx", "andYetAnother.pdf");
        List<String> mimeTypes = Arrays.asList("text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf");

        for (int i = 0; i < names.size(); i++) {
            ContentItem contentItem = contentService.newContentItem();
            contentItem.setName(names.get(i));
            contentItem.setMimeType(mimeTypes.get(i));
            contentService.saveContentItem(contentItem, new ByteArrayInputStream("This is an attachment".getBytes(UTF_8)));
            contentItems.add(contentItem);
        }

        return contentItems;
    }

}
