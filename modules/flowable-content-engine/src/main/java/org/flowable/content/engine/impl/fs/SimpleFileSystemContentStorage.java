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

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentMetaDataKeys;
import org.flowable.content.api.ContentObject;
import org.flowable.content.api.ContentStorage;
import org.flowable.content.api.ContentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

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
    private static final List<IOFileFilter> DEFAULT_DIRECTORY_NAMES_FILTER = Collections.unmodifiableList(
        Stream.of(
            TYPE_TASK, TYPE_PROCESS_INSTANCE, TYPE_CASE_INSTANCE, TYPE_UNCATEGORIZED
        )
            .map(NameFileFilter::new)
            .collect(toList())
    );
    public static final String TASK_PREFIX = "task";
    public static final String PROCESS_INSTANCE_PREFIX = "proc";
    public static final String CASE_PREFIX = "case";
    public static final String UNCATEGORIZED_PREFIX = "uncategorized";
    private static final Function<Map<String, Object>, String> GET_SCOPE_ID_FROM_METADATA = metaData -> {
        String scopeId = (String) metaData.get(ContentMetaDataKeys.SCOPE_ID);
        return StringUtils.isNotEmpty(scopeId) ? scopeId : "";
    };
    protected static final BiFunction<String, Map<String, Object>, String> GENERATE_CONTENTID_FOR_CASE = (uuid, metaData) -> CASE_PREFIX + "." + metaData
        .get(ContentMetaDataKeys.SCOPE_ID) + "." + uuid;
    protected static final BiFunction<String, Map<String, Object>, String> GENERATE_CONTENTID_FOR_SCOPE = (uuid, metaData) -> metaData
        .get(ContentMetaDataKeys.SCOPE_TYPE) + "." + metaData
        .get(ContentMetaDataKeys.SCOPE_ID) + "." + uuid;

    protected File contentFolderRoot;
    protected List<ContentTypeHandler> contentTypeHandlers;
    protected ContentTypeHandler uncategorizedContentTypeHandler;

    public SimpleFileSystemContentStorage(File contentFolderRoot) {
        this.contentFolderRoot = contentFolderRoot;
        validateOrCreateSubfolders();
    }

    protected void validateOrCreateSubfolders() {
        this.uncategorizedContentTypeHandler = ContentTypeHandler.of(TYPE_UNCATEGORIZED, validateOrCreateFolder(TYPE_UNCATEGORIZED),
            metaData -> "",
            (uuid, metaData) -> UNCATEGORIZED_PREFIX + "." + uuid
        );
        this.contentTypeHandlers = refreshTypeFolders();
    }

    protected List<ContentTypeHandler> refreshTypeFolders() {
        return Stream.concat(
            Stream.concat(
                // default folders without UNCATEGORIZED
                Stream.of(
                    ContentTypeHandler.of(TASK_PREFIX, validateOrCreateFolder(TYPE_TASK),
                        metaData -> (String) metaData.get(ContentMetaDataKeys.TASK_ID),
                        (uuid, metaData) -> TASK_PREFIX + "." + metaData.get(ContentMetaDataKeys.TASK_ID) + "." + uuid),
                    ContentTypeHandler.of(PROCESS_INSTANCE_PREFIX, validateOrCreateFolder(TYPE_PROCESS_INSTANCE),
                        metaData -> (String) metaData.get(ContentMetaDataKeys.PROCESS_INSTANCE_ID),
                        (uuid, metaData) -> PROCESS_INSTANCE_PREFIX + "." + metaData.get(ContentMetaDataKeys.PROCESS_INSTANCE_ID) + "." + uuid),
                    ContentTypeHandler.of(CASE_PREFIX, validateOrCreateFolder(TYPE_CASE_INSTANCE),
                        GET_SCOPE_ID_FROM_METADATA,
                        GENERATE_CONTENTID_FOR_CASE)
                ),
                // all directories without defaults
                FileUtils.listFilesAndDirs(contentFolderRoot,
                    FalseFileFilter.INSTANCE,
                    new AndFileFilter(
                        DirectoryFileFilter.INSTANCE,
                        new NotFileFilter(
                            new OrFileFilter(
                                DEFAULT_DIRECTORY_NAMES_FILTER
                            )
                        )
                    )
                )
                    .stream()
                    .map(directory -> ContentTypeHandler.of(directory.getName(), directory,
                        GET_SCOPE_ID_FROM_METADATA,
                        GENERATE_CONTENTID_FOR_SCOPE

                    ))
            )
                .sorted(Comparator.comparingInt(contentTypeHandler -> contentTypeHandler.getType().length()))
            ,
            // with uncategorized folder at the end
            Stream.of(this.uncategorizedContentTypeHandler)
        ).collect(toList());
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

    protected File createFolder(String folderName) {
        File subFolder = new File(contentFolderRoot, folderName);
        if (!subFolder.exists()) {
            boolean created = subFolder.mkdir();
            if (created) {
                LOGGER.info("Created content folder in {}", subFolder.getAbsolutePath());
            } else {
                LOGGER.warn("Could not create content folder. This might impact the storage of related content");
            }
        } else {
            throw new FlowableException("Folder already exists [" + folderName + "]");
        }
        return subFolder;
    }

    @Override
    public ContentObject createContentObject(InputStream contentStream, Map<String, Object> metaData) {
        String uuid = UUID_GENERATOR.generate().toString();
        ContentTypeHandler contentTypeHandler = getOrCreateContentTypeHandler(metaData);
        File file = getContentFile(metaData, uuid, contentTypeHandler);
        return storeContentToFile(contentStream, metaData, uuid, contentTypeHandler, file);
    }

    protected ContentObject storeContentToFile(InputStream contentStream, Map<String, Object> metaData, String uuid,
        ContentTypeHandler contentTypeHandler, File file) {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            long length = IOUtils.copy(contentStream, fos);
            String contentId = contentTypeHandler.getGenerateContentId().apply(uuid, metaData);
            return new FileSystemContentObject(file, contentId, length);
        } catch (IOException e) {
            throw new ContentStorageException("Could not write content to " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public ContentObject updateContentObject(String id, InputStream contentStream, Map<String, Object> metaData) {
        String[] ids = id.split("\\.");

        File contentFile = getContentFile(ids, subFolder -> {
                File idFolder = new File(subFolder.getFolder(), ids[1]);
            return new File(idFolder, ids[2]);
            }, () ->
            {
                File subFolder = createFolder(ids[0]);
                refreshTypeFolders();
                File idFolder = new File(subFolder, ids[1]);
                return new File(idFolder, ids[2]);
            }
        );

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
            try (FileOutputStream tempOutputStream = new FileOutputStream(tempContentFile)) {
                length = IOUtils.copy(contentStream, tempOutputStream);
            }

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
        String[] ids = id.split("\\.");
        return new FileSystemContentObject(getContentFile(ids,
            subFolder -> {
                File contentFile = new File(subFolder.getFolder(), ids[1]);
                if (ids.length == 3) {
                    contentFile = new File(contentFile, ids[2]);
                }
                if (!contentFile.exists()) {
                    throw new FlowableObjectNotFoundException("No content found for id " + Arrays.toString(ids));
                }
                return contentFile;
            },
            () -> {
                File contentFile = new File(uncategorizedContentTypeHandler.getFolder(), ids[1]);
                if (!contentFile.exists()) {
                    throw new FlowableObjectNotFoundException("No content found for id " + Arrays.toString(ids));
                }
                return contentFile;
            }), id);
    }

    protected File getContentFile(String[] ids,
        Function<ContentTypeHandler, File> recognizedContentTypeFunction,
        Supplier<File> UnrecognizedContentTypeSupplier) {
        String type = ids[0];

        return this.contentTypeHandlers.stream()
            .filter(typeFolder -> type.equals(typeFolder.getType()))
            .findFirst()
            .map(recognizedContentTypeFunction)
            .orElseGet(UnrecognizedContentTypeSupplier);
    }

    @Override
    public Map<String, Object> getMetaData() {
        // Currently not yet supported
        return null;
    }

    @Override
    public void deleteContentObject(String id) {
        try {
            String[] ids = id.split("\\.");
            File contentFile = getContentFile(ids, contentTypeHandler -> {
                    File localContentFile = new File(contentTypeHandler.getFolder(), ids[1]);
                    if (ids.length == 3) {
                        localContentFile = new File(localContentFile, ids[2]);
                    }
                return localContentFile;
                }, () -> {
                throw new FlowableException("Unrecognized content object ["+id+"]");
            });
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

    protected File getContentFile(Map<String, Object> metaData, String contentId, ContentTypeHandler contentTypeHandler) {
        return new File(createOrGetFolderBasedOnMetaData(metaData, contentTypeHandler), contentId);
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

    protected File createOrGetFolderBasedOnMetaData(Map<String, Object> metaData, ContentTypeHandler contentTypeHandler) {
        return
            internalCreateOrGetFolder(
                contentTypeHandler.getFolder(), contentTypeHandler.getGetIdFromMetadata().apply(metaData)
            );
    }

    protected ContentTypeHandler getOrCreateContentTypeHandler(Map<String, Object> metaData) {
        String type = determineType(metaData);
        return contentTypeHandlers.stream()
            .filter(anyContentTypeHandler -> type.equals(anyContentTypeHandler.getFolder().getName()))
            .findFirst()
            .orElseGet(() -> {
                    File typeFolder = createFolder(type);
                    this.contentTypeHandlers = refreshTypeFolders();
                    return ContentTypeHandler.of(type, typeFolder, GET_SCOPE_ID_FROM_METADATA, GENERATE_CONTENTID_FOR_SCOPE);
                }
            );
    }

    protected File internalCreateOrGetFolder(File parentFolder, String id) {
        File folder = new File(parentFolder, id);
        if (!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    protected static class ContentTypeHandler {
        final protected String type;
        final protected File Folder;
        final protected Function<Map<String, Object>, String> getIdFromMetadata;
        final protected BiFunction<String, Map<String, Object>, String> generateContentId;

        protected ContentTypeHandler(String type, File folder,
            Function<Map<String, Object>, String> getIdFromMetadata,
            BiFunction<String, Map<String, Object>, String> generateContentId) {
            this.type = type;
            Folder = folder;
            this.getIdFromMetadata = getIdFromMetadata;
            this.generateContentId = generateContentId;
        }

        public String getType() {
            return type;
        }
        public File getFolder() {
            return Folder;
        }

        public Function<Map<String, Object>, String> getGetIdFromMetadata() {
            return getIdFromMetadata;
        }

        public BiFunction<String, Map<String, Object>, String> getGenerateContentId() {
            return generateContentId;
        }

        public static ContentTypeHandler of(String type, File folder, Function<Map<String, Object>, String> getIdFromMetadataFunction,
            BiFunction<String, Map<String, Object>, String> generateContentId
            ) {
            return new ContentTypeHandler(type, folder, getIdFromMetadataFunction, generateContentId);
        }

    }
}
