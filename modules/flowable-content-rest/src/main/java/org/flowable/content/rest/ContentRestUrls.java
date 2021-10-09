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

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Tijs Rademakers
 */
public final class ContentRestUrls {

    public static final String SEGMENT_CONTENT_SERVICE_RESOURCES = "content-service";
    public static final String SEGMENT_CONTENT_ITEMS_RESOURCE = "content-items";
    public static final String SEGMENT_QUERY_RESOURCE = "query";
    public static final String SEGMENT_CONTENT_ITEM_DATA = "data";

    /**
     * URL template for a content item collection: <i>/content-service/content-items</i>
     */
    public static final String[] URL_CONTENT_ITEM_COLLECTION = { SEGMENT_CONTENT_SERVICE_RESOURCES, SEGMENT_CONTENT_ITEMS_RESOURCE };

    /**
     * URL template for a single content item: <i>/content-service/content-items/{0:contentId}</i>
     */
    public static final String[] URL_CONTENT_ITEM = { SEGMENT_CONTENT_SERVICE_RESOURCES, SEGMENT_CONTENT_ITEMS_RESOURCE, "{0}" };

    /**
     * URL template for the data of a content item: <i>/content-service/content-items/{0:contentId}/data</i>
     */
    public static final String[] URL_CONTENT_ITEM_DATA = { SEGMENT_CONTENT_SERVICE_RESOURCES, SEGMENT_CONTENT_ITEMS_RESOURCE, "{0}", SEGMENT_CONTENT_ITEM_DATA };

    /**
     * URL template for a content item query resource: <i>/query/content-items</i>
     */
    public static final String[] URL_QUERY_CONTENT_ITEM = { SEGMENT_QUERY_RESOURCE, SEGMENT_CONTENT_ITEMS_RESOURCE };

    /**
     * Creates an url based on the passed fragments and replaces any placeholders with the given arguments. The placeholders are following the {@link MessageFormat} convention (eg. {0} is replaced by
     * first argument value).
     */
    public static String createRelativeResourceUrl(String[] segments, Object... arguments) {
        return MessageFormat.format(StringUtils.join(segments, '/'), arguments);
    }
}
