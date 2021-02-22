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
package org.flowable.ui.task.rest.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.SecurityScope;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.exception.NotPermittedException;
import org.flowable.ui.common.service.idm.cache.UserCache;
import org.flowable.ui.task.model.component.SimpleContentTypeMapper;
import org.flowable.ui.task.model.runtime.ContentItemRepresentation;
import org.flowable.ui.task.service.runtime.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

public abstract class AbstractRelatedContentResource {

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected SimpleContentTypeMapper simpleTypeMapper;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected UserCache userCache;

    public ResultListDataRepresentation getContentItemsForTask(String taskId) {
        permissionService.validateReadPermissionOnTask(SecurityUtils.getAuthenticatedSecurityScope(), taskId);
        return createResultRepresentation(contentService.createContentItemQuery().taskId(taskId).list());
    }

    public ResultListDataRepresentation getContentItemsForCase(String caseInstanceId) {
        permissionService.hasReadPermissionOnCase(SecurityUtils.getAuthenticatedSecurityScope(), caseInstanceId);
        return createResultRepresentation(contentService.createContentItemQuery().scopeType("cmmn").scopeId(caseInstanceId).list());
    }

    public ResultListDataRepresentation getContentItemsForProcessInstance(String processInstanceId) {
        // TODO: check if process exists
        if (!permissionService.hasReadPermissionOnProcessInstance(SecurityUtils.getAuthenticatedSecurityScope(), processInstanceId)) {
            throw new NotPermittedException("You are not allowed to read the process with id: " + processInstanceId);
        }
        return createResultRepresentation(contentService.createContentItemQuery().processInstanceId(processInstanceId).list());
    }

    public ContentItemRepresentation createContentItemOnTask(String taskId, MultipartFile file) {
        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new NotFoundException("Task not found or already completed: " + taskId);
        }

        if (!permissionService.canAddRelatedContentToTask(user, taskId)) {
            throw new NotPermittedException("You are not allowed to read the task with id: " + taskId);
        }
        return uploadFile(user, file, taskId, task.getProcessInstanceId(), null);
    }

    public ContentItemRepresentation createContentItemOnTask(String taskId, ContentItemRepresentation contentItem) {
        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new NotFoundException("Task not found or already completed: " + taskId);
        }

        if (!permissionService.canAddRelatedContentToTask(user, taskId)) {
            throw new NotPermittedException("You are not allowed to read the task with id: " + taskId);
        }

        return addContentItem(contentItem, taskId, task.getProcessInstanceId(), true);
    }

    public ContentItemRepresentation createContentItemOnProcessInstance(String processInstanceId, ContentItemRepresentation contentItem) {
        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();

        if (!permissionService.canAddRelatedContentToProcessInstance(user, processInstanceId)) {
            throw new NotPermittedException("You are not allowed to read the process with id: " + processInstanceId);
        }

        return addContentItem(contentItem, null, processInstanceId, true);
    }

    public ContentItemRepresentation createContentItemOnProcessInstance(String processInstanceId, MultipartFile file) {
        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();

        if (!permissionService.canAddRelatedContentToProcessInstance(user, processInstanceId)) {
            throw new NotPermittedException("You are not allowed to read the process with id: " + processInstanceId);
        }
        return uploadFile(user, file, null, processInstanceId, null);
    }

    public ContentItemRepresentation createContentItemOnCase(String caseId, MultipartFile file) {
        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();

        if (!permissionService.canAddRelatedContentToCase(user, caseId)) {
            throw new NotPermittedException("You are not allowed to read the case with id: " + caseId);
        }
        return uploadFile(user, file, null, null, caseId);
    }

    public ContentItemRepresentation createTemporaryRawContentItem(MultipartFile file) {
        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();
        return uploadFile(user, file, null, null, null);
    }

    public ContentItemRepresentation createTemporaryContentItem(ContentItemRepresentation contentItem) {
        return addContentItem(contentItem, null, null, false);
    }

    public void deleteContent(String contentId, HttpServletResponse response) {
        ContentItem contentItem = contentService.createContentItemQuery().id(contentId).singleResult();

        if (contentItem == null) {
            throw new NotFoundException("No content found with id: " + contentId);
        }

        if (!permissionService.hasWritePermissionOnRelatedContent(SecurityUtils.getAuthenticatedSecurityScope(), contentItem)) {
            throw new NotPermittedException("You are not allowed to delete the content with id: " + contentId);
        }

        if (contentItem.getField() != null) {
            // Not allowed to delete content that has been added as part of a form
            throw new NotPermittedException("You are not allowed to delete the content with id: " + contentId);
        }

        contentService.deleteContentItem(contentItem.getId());
    }

    public ContentItemRepresentation getContent(String contentId) {
        ContentItem contentItem = contentService.createContentItemQuery().id(contentId).singleResult();

        if (contentItem == null) {
            throw new NotFoundException("No content found with id: " + contentId);
        }

        if (!permissionService.canDownloadContent(SecurityUtils.getAuthenticatedSecurityScope(), contentItem)) {
            throw new NotPermittedException("You are not allowed to view the content with id: " + contentId);
        }

        return createContentItemResponse(contentItem);
    }

    public void getRawContent(String contentId, HttpServletResponse response) {
        ContentItem contentItem = contentService.createContentItemQuery().id(contentId).singleResult();

        if (contentItem == null) {
            throw new NotFoundException("No content found with id: " + contentId);
        }
        if (!contentItem.isContentAvailable()) {
            throw new NotFoundException("Raw content not yet available for id: " + contentId);
        }

        if (!permissionService.canDownloadContent(SecurityUtils.getAuthenticatedSecurityScope(), contentItem)) {
            throw new NotPermittedException("You are not allowed to read the content with id: " + contentId);
        }

        // Set correct mine-type
        if (contentItem.getMimeType() != null) {
            response.setContentType(contentItem.getMimeType());
        }

        // Write content response
        try (InputStream inputstream = contentService.getContentItemData(contentId)) {
            IOUtils.copy(inputstream, response.getOutputStream());

        } catch (IOException e) {
            throw new InternalServerErrorException("Error while writing raw content data for content: " + contentId, e);
        }
    }

    protected ContentItemRepresentation uploadFile(SecurityScope user, MultipartFile file, String taskId, String processInstanceId, String caseId) {
        if (file != null && file.getName() != null) {
            try {
                String contentType = file.getContentType();

                // temp additional content type check for IE9 flash uploads
                if (StringUtils.equals(file.getContentType(), "application/octet-stream")) {
                    contentType = getContentTypeForFileExtension(file);
                }

                ContentItem contentItem = contentService.newContentItem();
                contentItem.setName(getFileName(file));
                contentItem.setProcessInstanceId(processInstanceId);
                contentItem.setTaskId(taskId);
                if (StringUtils.isNotEmpty(caseId)) {
                    contentItem.setScopeType("cmmn");
                    contentItem.setScopeId(caseId);
                }
                contentItem.setMimeType(contentType);
                contentItem.setCreatedBy(user.getUserId());
                contentItem.setLastModifiedBy(user.getUserId());
                contentService.saveContentItem(contentItem, file.getInputStream());

                return createContentItemResponse(contentItem);

            } catch (IOException e) {
                throw new BadRequestException("Error while reading file data", e);
            }

        } else {
            throw new BadRequestException("File to upload is missing");
        }
    }

    protected ContentItemRepresentation addContentItem(ContentItemRepresentation contentItemBody, String taskId, String processInstanceId, boolean isRelatedContent) {

        if (contentItemBody.getContentStoreId() == null || contentItemBody.getContentStoreName() == null || contentItemBody.getName() == null) {
            throw new BadRequestException("Name, source and sourceId are required parameters");
        }

        SecurityScope user = SecurityUtils.getAuthenticatedSecurityScope();

        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName(contentItemBody.getName());
        contentItem.setProcessInstanceId(processInstanceId);
        contentItem.setTaskId(taskId);
        contentItem.setContentStoreId(contentItemBody.getContentStoreId());
        contentItem.setContentStoreName(contentItemBody.getContentStoreName());
        contentItem.setMimeType(contentItemBody.getMimeType());
        contentItem.setCreatedBy(user.getUserId());
        contentItem.setLastModifiedBy(user.getUserId());
        contentService.saveContentItem(contentItem);

        return createContentItemResponse(contentItem);
    }

    protected String getFileName(MultipartFile file) {
        return file.getOriginalFilename() != null ? file.getOriginalFilename() : "Nameless file";
    }

    protected ResultListDataRepresentation createResultRepresentation(List<ContentItem> results) {
        List<ContentItemRepresentation> resultList = new ArrayList<>(results.size());

        for (ContentItem content : results) {
            resultList.add(createContentItemResponse(content));
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        result.setTotal((long) results.size());
        return result;
    }

    protected ContentItemRepresentation createContentItemResponse(ContentItem contentItem) {
        ContentItemRepresentation contentItemResponse = new ContentItemRepresentation(contentItem, simpleTypeMapper);
        return contentItemResponse;
    }

    protected String getContentTypeForFileExtension(MultipartFile file) {

        String fileName = file.getOriginalFilename();
        String contentType = null;
        if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            contentType = "image/jpeg";
        } else if (fileName.endsWith("gif")) {
            contentType = "image/gif";
        } else if (fileName.endsWith("png")) {
            contentType = "image/png";
        } else if (fileName.endsWith("bmp")) {
            contentType = "image/bmp";
        } else if (fileName.endsWith("tif") || fileName.endsWith(".tiff")) {
            contentType = "image/tiff";
        } else if (fileName.endsWith("doc")) {
            contentType = "application/msword";
        } else if (fileName.endsWith("docx")) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (fileName.endsWith("docm")) {
            contentType = "application/vnd.ms-word.document.macroenabled.12";
        } else if (fileName.endsWith("dotm")) {
            contentType = "application/vnd.ms-word.template.macroenabled.12";
        } else if (fileName.endsWith("odt")) {
            contentType = "application/vnd.oasis.opendocument.text";
        } else if (fileName.endsWith("ott")) {
            contentType = "application/vnd.oasis.opendocument.text-template";
        } else if (fileName.endsWith("rtf")) {
            contentType = "application/rtf";
        } else if (fileName.endsWith("txt")) {
            contentType = "application/text";
        } else if (fileName.endsWith("xls")) {
            contentType = "application/vnd.ms-excel";
        } else if (fileName.endsWith("xlsx")) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (fileName.endsWith("xlsb")) {
            contentType = "application/vnd.ms-excel.sheet.binary.macroenabled.12";
        } else if (fileName.endsWith("xltx")) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.template";
        } else if (fileName.endsWith("ods")) {
            contentType = "application/vnd.oasis.opendocument.spreadsheet";
        } else if (fileName.endsWith("ppt")) {
            contentType = "application/vnd.ms-powerpoint";
        } else if (fileName.endsWith("pptx")) {
            contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (fileName.endsWith("ppsm")) {
            contentType = "application/vnd.ms-powerpoint.slideshow.macroenabled.12";
        } else if (fileName.endsWith("ppsx")) {
            contentType = "application/vnd.openxmlformats-officedocument.presentationml.slideshow";
        } else if (fileName.endsWith("odp")) {
            contentType = "application/vnd.oasis.opendocument.presentation";
        } else {
            // We've done what we could
            return file.getContentType();
        }

        return contentType;
    }
}
