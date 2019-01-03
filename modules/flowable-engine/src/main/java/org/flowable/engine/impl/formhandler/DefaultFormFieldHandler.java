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
package org.flowable.engine.impl.formhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormFieldTypes;
import org.flowable.form.model.SimpleFormModel;

/**
 * @author Tijs Rademakers
 */
public class DefaultFormFieldHandler implements FormFieldHandler {
    
    /**
     * When content is uploaded for a field, it is uploaded as a 'temporary related content'. Now that the task is completed, we need to associate the field/taskId/processInstanceId with the related
     * content so we can retrieve it later.
     */
    @Override
    public void handleFormFieldsOnSubmit(FormInfo formInfo, String taskId, String processInstanceId, String scopeId, 
                    String scopeType, Map<String, Object> variables, String tenantId) {
        
        ContentService contentService = CommandContextUtil.getContentService();
        if (contentService == null || formInfo == null) {
            return;
        }

        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        if (formModel != null && formModel.getFields() != null) {
            for (FormField formField : formModel.getFields()) {
                if (FormFieldTypes.UPLOAD.equals(formField.getType())) {

                    String variableName = formField.getId();
                    if (variables.containsKey(variableName)) {
                        String variableValue = (String) variables.get(variableName);
                        if (StringUtils.isNotEmpty(variableValue)) {
                            String[] contentItemIds = StringUtils.split(variableValue, ",");
                            Set<String> contentItemIdSet = new HashSet<>();
                            Collections.addAll(contentItemIdSet, contentItemIds);

                            List<ContentItem> contentItems = contentService.createContentItemQuery().ids(contentItemIdSet).list();

                            for (ContentItem contentItem : contentItems) {
                                contentItem.setTaskId(taskId);
                                contentItem.setProcessInstanceId(processInstanceId);
                                contentItem.setScopeId(scopeId);
                                contentItem.setScopeType(scopeType);
                                contentItem.setField(formField.getId());
                                contentItem.setTenantId(tenantId);
                                contentService.saveContentItem(contentItem);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void enrichFormFields(FormInfo formInfo) {
        ContentService contentService = CommandContextUtil.getContentService();
        if (contentService == null) {
            return;
        }

        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        if (formModel.getFields() != null) {
            for (FormField formField : formModel.getFields()) {
                if (FormFieldTypes.UPLOAD.equals(formField.getType())) {

                    List<String> contentItemIds = null;
                    if (formField.getValue() instanceof List) {
                        contentItemIds = (List<String>) formField.getValue();

                    } else if (formField.getValue() instanceof String) {
                        String[] splittedString = ((String) formField.getValue()).split(",");
                        contentItemIds = new ArrayList<>();
                        Collections.addAll(contentItemIds, splittedString);
                    }

                    if (contentItemIds != null) {
                        Set<String> contentItemIdSet = new HashSet<>(contentItemIds);

                        List<ContentItem> contentItems = contentService.createContentItemQuery()
                                .ids(contentItemIdSet)
                                .list();

                        formField.setValue(contentItems);
                    }
                }
            }
        }
    }

}
