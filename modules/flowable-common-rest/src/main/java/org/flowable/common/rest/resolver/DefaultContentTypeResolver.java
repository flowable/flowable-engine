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

package org.flowable.common.rest.resolver;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

/**
 * Default implementation of a {@link ContentTypeResolver}, resolving a limited set of well-known content types used in the engine.
 * 
 * @author Tijs Rademakers
 * @author Yvo Swillens
 * @author Tim Stephenson
 */
public class DefaultContentTypeResolver implements ContentTypeResolver {

    protected final Map<String, String> fileExtensionToContentType;
    protected String unknownFileContentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    public DefaultContentTypeResolver() {
        this.fileExtensionToContentType = new HashMap<>();
        this.fileExtensionToContentType.put("png", MediaType.IMAGE_PNG_VALUE);

        this.fileExtensionToContentType.put("txt", MediaType.TEXT_PLAIN_VALUE);

        this.fileExtensionToContentType.put("xml", MediaType.TEXT_XML_VALUE);
        this.fileExtensionToContentType.put("bpmn", MediaType.TEXT_XML_VALUE);
        this.fileExtensionToContentType.put("cmmn", MediaType.TEXT_XML_VALUE);
        this.fileExtensionToContentType.put("dmn", MediaType.TEXT_XML_VALUE);

        this.fileExtensionToContentType.put("app", MediaType.APPLICATION_JSON_VALUE);
        this.fileExtensionToContentType.put("event", MediaType.APPLICATION_JSON_VALUE);
        this.fileExtensionToContentType.put("form", MediaType.APPLICATION_JSON_VALUE);
        this.fileExtensionToContentType.put("channel", MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    public String resolveContentType(String resourceName) {
        if (resourceName != null && !resourceName.isEmpty()) {
            String lowerResourceName = resourceName.toLowerCase();
            String fileExtension = StringUtils.substringAfterLast(lowerResourceName, '.');
            return fileExtensionToContentType.getOrDefault(fileExtension, unknownFileContentType);
        }
        return null;
    }

    public void addFileExtensionMapping(String fileExtension, String contentType) {
        this.fileExtensionToContentType.put(fileExtension, contentType);
    }

    public void setUnknownFileContentType(String unknownFileContentType) {
        this.unknownFileContentType = unknownFileContentType;
    }
}
