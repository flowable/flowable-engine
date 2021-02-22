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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.scope.ScopeTypes;

/**
 * Storage for reading and writing content.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public interface ContentStorage {

    /**
     * @param contentStream
     * @param metaData
     *            A key-value collection that can be used to change the way the content is stored.
     * @return reads the given {@link InputStream} and stores it. Returns a {@link ContentObject} with a unique id generated - which can be used for reading the content again.
     * @deprecated use {@link #createContentObject(InputStream, ContentObjectStorageMetadata)} instead
     */
    @Deprecated
    ContentObject createContentObject(InputStream contentStream, Map<String, Object> metaData);

    /**
     * Reads the given {@link InputStream} and stores it.
     * @param contentStream the content stream that should be stored
     * @param metaData additional data that can be used to change the way the content is stored
     * @return a {@link ContentObject} with a unique id generated - which can be used for reading the content again
     */
    default ContentObject createContentObject(InputStream contentStream, ContentObjectStorageMetadata metaData) {
        Map<String, Object> mapMetaData = new HashMap<>();
        if (ScopeTypes.TASK.equals(metaData.getScopeType())) {
            mapMetaData.put(ContentMetaDataKeys.TASK_ID, metaData.getScopeId());
        } else if (ScopeTypes.BPMN.equals(metaData.getScopeType())) {
            mapMetaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, metaData.getScopeId());
        } else {
            if (StringUtils.isNotEmpty(metaData.getScopeType())) {
                mapMetaData.put(ContentMetaDataKeys.SCOPE_TYPE, metaData.getScopeType());
            }

            if (StringUtils.isNotEmpty(metaData.getScopeId())) {
                mapMetaData.put(ContentMetaDataKeys.SCOPE_ID, metaData.getScopeId());
            }
        }

        if (StringUtils.isNotEmpty(metaData.getTenantId())) {
            mapMetaData.put("tenantId", metaData.getTenantId());
        }

        return createContentObject(contentStream, mapMetaData);
    }

    /**
     * Update the content with the given id to the content present in the given stream.
     * 
     * @param id
     * @param contentStream
     * @param metaData
     *            A key-value collection that can be used to change the way the content is stored.
     * @return Returns a {@link ContentObject} with a unique id generated - which can br used for reading the content again.
     * @throws ContentStorageException
     *             When an exception occurred while updating the content and the content is not updated.
     * @deprecated use {@link #updateContentObject(String, InputStream, ContentObjectStorageMetadata)} instead
     */
    @Deprecated
    ContentObject updateContentObject(String id, InputStream contentStream, Map<String, Object> metaData);

    /**
     * Update the content with the given id to the content present in the given stream.
     *
     * @param id the id of the content being updated
     * @param contentStream the content stream that should be updated
     * @param metaData additional data that can be used to change the way the content is stored
     * @return Returns a {@link ContentObject} with a unique id generated - which can br used for reading the content again.
     * @throws ContentStorageException
     *             When an exception occurred while updating the content and the content is not updated.
     */
    default ContentObject updateContentObject(String id, InputStream contentStream, ContentObjectStorageMetadata metaData) {
        Map<String, Object> mapMetaData = new HashMap<>();
        if (ScopeTypes.TASK.equals(metaData.getScopeType())) {
            mapMetaData.put(ContentMetaDataKeys.TASK_ID, metaData.getScopeId());
        } else if (ScopeTypes.BPMN.equals(metaData.getScopeType())) {
            mapMetaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, metaData.getScopeId());
        } else {
            if (StringUtils.isNotEmpty(metaData.getScopeType())) {
                mapMetaData.put(ContentMetaDataKeys.SCOPE_TYPE, metaData.getScopeType());
            }

            if (StringUtils.isNotEmpty(metaData.getScopeId())) {
                mapMetaData.put(ContentMetaDataKeys.SCOPE_ID, metaData.getScopeId());
            }
        }

        if (StringUtils.isNotEmpty(metaData.getTenantId())) {
            mapMetaData.put("tenantId", metaData.getTenantId());
        }


        return updateContentObject(id, contentStream, mapMetaData);
    }

    /**
     * @return a {@link ContentObject} with the given id.
     * @throws ContentNotFoundException
     *             When the content with the given id does not exist
     */
    ContentObject getContentObject(String id);

    /**
     * @return Returns the metadata that was passed when creating the {@link ContentObject}
     */
    Map<String, Object> getMetaData();

    /**
     * Deletes the object the given id.
     * 
     * @param id
     * @throws ContentNotFoundException
     *             When the content with the given id does not exist
     * @throws ContentStorageException
     *             When an error occurred while deleting the content.
     */
    void deleteContentObject(String id);

    String getContentStoreName();
}
