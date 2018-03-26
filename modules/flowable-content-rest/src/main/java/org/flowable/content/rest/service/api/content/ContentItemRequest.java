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
package org.flowable.content.rest.service.api.content;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class ContentItemRequest {

    protected String name;
    protected String mimeType;
    protected String taskId;
    protected String processInstanceId;
    protected String contentStoreId;
    protected String contentStoreName;
    protected String field;
    protected String tenantId;
    protected String createdBy;
    protected String lastModifiedBy;

    @ApiModelProperty(value = "Name of the content item", example = "Simple content item")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Mime type of the content item, optional", example = "application/pdf")
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @ApiModelProperty(value = "Task identifier for the content item, optional", example = "12345")
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @ApiModelProperty(value = "Process instance identifier for the content item, optional", example = "1234")
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @ApiModelProperty(value = "The identifier of the content item in an external content store, optional.", example = "5678")
    public String getContentStoreId() {
        return contentStoreId;
    }

    public void setContentStoreId(String contentStoreId) {
        this.contentStoreId = contentStoreId;
    }

    @ApiModelProperty(value = "The name of an external content store, optional", example = "myFileStore")
    public String getContentStoreName() {
        return contentStoreName;
    }

    public void setContentStoreName(String contentStoreName) {
        this.contentStoreName = contentStoreName;
    }

    @ApiModelProperty(value = "The form field for the content item, optional", example = "uploadField")
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    @ApiModelProperty(value = "The tenant identifier of the content item, optional.", example = "myTenantId")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(value = "The user identifier that created the content item, optional", example = "johndoe")
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @ApiModelProperty(value = "The user identifier that last modified the content item, optional", example = "johndoe")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
