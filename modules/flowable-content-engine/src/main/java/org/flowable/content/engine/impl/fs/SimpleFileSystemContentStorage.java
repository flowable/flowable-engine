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
package org.flowable.content.engine.impl.fs;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentMetaDataKeys;
import org.flowable.content.api.ContentObject;
import org.flowable.content.api.ContentStorage;
import org.flowable.content.api.ContentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * (Very) simple implementation of the {@link ContentStorage} that relies on the passed metadata to store content.
 *
 * Under a root folder, a division between 'task' and 'process-instance' content is made. New content gets a new UUID assigned and is placed in one of these folders.
 *
 * The id of the returned {@link ContentObject} indicates in which folder it is stored.
 *
 * @author Joram Barrez
 */
public class SimpleFileSystemContentStorage implements ContentStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFileSystemContentStorage.class);

    private static TimeBasedGenerator UUID_GENERATOR = Generators.timeBasedGenerator(EthernetAddress.fromInterface());

    private static final String TEMP_SUFFIX = "_TEMP";
    private static final String OLD_SUFFIX = "_OLD";

    public static final String TYPE_TASK = "task-content";
    public static final String TYPE_PROCESS_INSTANCE = "process-instance-content";
    public static final String TYPE_CASE_INSTANCE = "cmmn";
    public static final String TYPE_UNCATEGORIZED = "uncategorized";

    public static final String TASK_PREFIX = "task";
    public static final String PROCESS_INSTANCE_PREFIX = "proc";
    public static final String CASE_PREFIX = "case";
    public static final String UNCATEGORIZED_PREFIX = "uncategorized";

    protected File contentFolderRoot;
    protected File taskFolder;
    protected File processInstanceFolder;
    protected File caseFolder;
    protected File uncategorizedFolder;

    public SimpleFileSystemContentStorage(File contentFolderRoot) {
        this.contentFolderRoot = contentFolderRoot;
        validateOrCreateSubfolders();
    }

    protected void validateOrCreateSubfolders() {
        taskFolder = validateOrCreateFolder(TYPE_TASK);
        processInstanceFolder = validateOrCreateFolder(TYPE_PROCESS_INSTANCE);
        caseFolder = validateOrCreateFolder(TYPE_CASE_INSTANCE);
        uncategorizedFolder = validateOrCreateFolder(TYPE_UNCATEGORIZED);
    }

    protected File validateOrCreateFolder(String folderName) {
        File subFolder = new File(contentFolderRoot, folderName);
        if (!subFolder.exists()) {
            boolean created = subFolder.mkdir();
            if (created) {
                LOGGER.info("Created content folder in {}", subFolder.getAbsolutePath());
            } else {
                LOGGER.warn("Could not create content folder. This might impact the storage of related content");
            }
        }
        return subFolder;
    }

    @Override
    public ContentObject createContentObject(InputStream contentStream, Map<String, Object> metaData) {
        String uuid = UUID_GENERATOR.generate().toString();
        File file = getContentFile(metaData, uuid);
        try {
            long length = IOUtils.copy(contentStream, new FileOutputStream(file, false));
            String contentId = generateContentId(uuid, metaData);
            return new FileSystemContentObject(file, contentId, length);
        } catch (IOException e) {
            throw new ContentStorageException("Could not write content to " + file.getAbsolutePath(), e);
        }
    }

    protected String generateContentId(String uuid, Map<String, Object> metaData) {
        String contentId = "";
        switch (determineType(metaData)) {
        case TYPE_PROCESS_INSTANCE:
            String processInstanceId = (String) metaData.get(ContentMetaDataKeys.PROCESS_INSTANCE_ID);
            contentId = PROCESS_INSTANCE_PREFIX + "." + processInstanceId;
            break;

        case TYPE_TASK:
            String taskId = (String) metaData.get(ContentMetaDataKeys.TASK_ID);
            contentId = TASK_PREFIX + "." + taskId;
            break;

        case TYPE_CASE_INSTANCE:
            String caseId = (String) metaData.get(ContentMetaDataKeys.SCOPE_ID);
            contentId = CASE_PREFIX + "." + caseId;
            break;

        default:
            contentId = UNCATEGORIZED_PREFIX;
            break;
        }
        contentId += "." + uuid;
        return contentId;
    }

    @Override
    public ContentObject updateContentObject(String id, InputStream contentStream, Map<String, Object> metaData) {
        File contentFile = getContentFile(id);

        // Write stream to a temporary file and rename when content is read
        // successfully to prevent overriding existing file and failing or keeping existing file
        File tempContentFile = new File(contentFile.getParentFile(), id + TEMP_SUFFIX);
        File oldContentFile = new File(contentFile.getParentFile(), id + OLD_SUFFIX);
        boolean tempFileCreated = false;
        long length = -1;

        try {
            if (!tempContentFile.createNewFile()) {
                // File already exists, being updated by another thread
                throw new ContentStorageException("Cannot update content with id: " + id + ", being updated by another user");
            }

            tempFileCreated = true;

            // Write the actual content to the file
            FileOutputStream tempOutputStream = new FileOutputStream(tempContentFile);
            length = IOUtils.copy(contentStream, tempOutputStream);
            IOUtils.closeQuietly(tempOutputStream);

            // Rename the content file first
            if (contentFile.renameTo(oldContentFile)) {
                if (tempContentFile.renameTo(contentFile)) {
                    // Rename was successful
                    oldContentFile.delete();
                } else {
                    // Rename failed, restore previous content
                    oldContentFile.renameTo(contentFile);
                    throw new ContentStorageException("Error while renaming new content file, content not updated");
                }
            } else {
                throw new ContentStorageException("Error while renaming existing content file, content not updated");
            }
        } catch (IOException ioe) {
            throw new ContentStorageException("Error while updating content with id: " + id, ioe);

        } finally {
            if (tempFileCreated) {
                try {
                    tempContentFile.delete();
                } catch (Throwable t) {
                    // No need to throw, shouldn't cause an error if the temp file cannot
                    // be deleted
                }
            }
        }

        return new FileSystemContentObject(contentFile, id, length);
    }

    @Override
    public ContentObject getContentObject(String id) {
        return new FileSystemContentObject(getContentFile(id), id);
    }

    protected File getContentFile(String id) {
        String[] ids = id.split("\\.");
        String type = ids[0];
        if (PROCESS_INSTANCE_PREFIX.equals(type) || TASK_PREFIX.equals(type) || CASE_PREFIX.equals(type)) {
            File subFolder=null;
            if (PROCESS_INSTANCE_PREFIX.equals(type)) {
                subFolder = processInstanceFolder;
            } else if(TASK_PREFIX.equals(type)) {
                subFolder = taskFolder;
            } else if (CASE_PREFIX.equals(type)) {
                subFolder = caseFolder;
            }
            File idFolder = new File(subFolder, ids[1]);
            File contentFile = new File(idFolder, ids[2]);
            return contentFile;

        } else if (UNCATEGORIZED_PREFIX.equals(type)) {
            File contentFile = new File(uncategorizedFolder, ids[1]);
            return contentFile;
        }

        throw new FlowableObjectNotFoundException("No content found for id " + id);
    }

    @Override
    public Map<String, Object> getMetaData() {
        // Currently not yet supported
        return null;
    }

    @Override
    public void deleteContentObject(String id) {
        try {
            File contentFile = getContentFile(id);
            File parentFile = contentFile.getParentFile();
            contentFile.delete();

            if (parentFile.listFiles().length == 0) {
                parentFile.delete();
            }
        } catch (Exception e) {
            throw new ContentStorageException("Error while deleting content", e);
        }
    }

    @Override
    public String getContentStoreName() {
        return "file";
    }

    protected File getContentFile(Map<String, Object> metaData, String contentId) {
        return new File(createOrGetFolderBasedOnMetaData(metaData), contentId);
    }

    protected String determineType(Map<String, Object> metaData) {
        String processInstanceId = (String) metaData.get(ContentMetaDataKeys.PROCESS_INSTANCE_ID);
        if (StringUtils.isNotEmpty(processInstanceId)) {
            return TYPE_PROCESS_INSTANCE;
        }

        String taskId = (String) metaData.get(ContentMetaDataKeys.TASK_ID);
        if (StringUtils.isNotEmpty(taskId)) {
            return TYPE_TASK;
        }

        String scopeType = (String) metaData.get(ContentMetaDataKeys.SCOPE_TYPE);
        if (StringUtils.isNotEmpty(scopeType)) {
            return scopeType;
        }

        return TYPE_UNCATEGORIZED;
    }

    protected File createOrGetFolderBasedOnMetaData(Map<String, Object> metaData) {
        switch (determineType(metaData)) {
        case TYPE_PROCESS_INSTANCE:
            String processInstanceId = (String) metaData.get(ContentMetaDataKeys.PROCESS_INSTANCE_ID);
            return internalCreateOrGetFolder(processInstanceFolder, processInstanceId);

        case TYPE_TASK:
            String taskId = (String) metaData.get(ContentMetaDataKeys.TASK_ID);
            return internalCreateOrGetFolder(taskFolder, taskId);

        case TYPE_CASE_INSTANCE:
            String caseId = (String) metaData.get(ContentMetaDataKeys.SCOPE_ID);
            return internalCreateOrGetFolder(caseFolder, caseId);

        default:
            return uncategorizedFolder;
        }
    }

    protected File internalCreateOrGetFolder(File parentFolder, String id) {
        File folder = new File(parentFolder, id);
        if (!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

}
