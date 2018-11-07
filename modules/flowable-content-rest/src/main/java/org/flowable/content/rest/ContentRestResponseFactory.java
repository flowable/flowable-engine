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
package org.flowable.content.rest;

import java.util.ArrayList;
import java.util.List;

import org.flowable.content.api.ContentItem;
import org.flowable.content.rest.service.api.content.ContentItemResponse;

/**
 *
 * Default implementation of a {@link ContentRestResponseFactory}.
 *
 * @author Tijs Rademakers
 */
public class ContentRestResponseFactory {

    public ContentItemResponse createContentItemResponse(ContentItem contentItem) {
        return createContentItemResponse(contentItem, createUrlBuilder());
    }

    public ContentItemResponse createContentItemResponse(ContentItem contentItem, ContentRestUrlBuilder urlBuilder) {
        ContentItemResponse response = new ContentItemResponse(contentItem,
                urlBuilder.buildUrl(ContentRestUrls.URL_CONTENT_ITEM, contentItem.getId()));
        return response;
    }

    public List<ContentItemResponse> createContentItemResponseList(List<ContentItem> contentItems) {
        ContentRestUrlBuilder urlBuilder = createUrlBuilder();
        List<ContentItemResponse> responseList = new ArrayList<>();
        for (ContentItem contentItem : contentItems) {
            responseList.add(createContentItemResponse(contentItem, urlBuilder));
        }
        return responseList;
    }

    protected ContentRestUrlBuilder createUrlBuilder() {
        return ContentRestUrlBuilder.fromCurrentRequest();
    }
}
