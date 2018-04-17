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
package org.flowable.ui.admin.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Bassam Al-Sarori
 */
public class AttachmentResponseInfo extends ResponseInfo {

    protected String fileName;
    protected byte[] bytes;

    public AttachmentResponseInfo(String fileName, byte[] bytes) {
        super(200);
        this.fileName = fileName;
        this.bytes = bytes;
    }

    public AttachmentResponseInfo(int statusCode, JsonNode content) {
        super(statusCode, content);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getBytes() {
        return bytes;
    }

}
