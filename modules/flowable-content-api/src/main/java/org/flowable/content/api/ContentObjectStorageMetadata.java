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
package org.flowable.content.api;

/**
 * The metadata passed when creating a {@link ContentObject}
 *
 * @author Filip Hrisafov
 */
public interface ContentObjectStorageMetadata {

    /**
     * The name of the content.
     */
    String getName();

    /**
     * The scope id of the content.
     */
    String getScopeId();

    /**
     * The scope type of the content
     */
    String getScopeType();

    /**
     * The mime type of the content.
     */
    String getMimeType();

    /**
     * The tenant id of the content
     */
    String getTenantId();

    /**
     * The object being stored.
     */
    Object getStoredObject();
}
