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

package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.api.runtime.FileItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.FileItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.FileItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CaseFileItem;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class AssociateFileItemCmd implements Command<FileItemInstance> {

    protected String caseInstanceId;
    protected String fileItemKey;
    protected String contentId;

    public AssociateFileItemCmd(String caseInstanceId, String fileItemKey, String contentId) {
        this.caseInstanceId = caseInstanceId;
        this.fileItemKey = fileItemKey;
        this.contentId = contentId;
    }

    @Override
    public FileItemInstance execute(CommandContext commandContext) {

        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found with id " + caseInstanceId);
        }

        CmmnModel cmmnModel = CaseDefinitionUtil.getCmmnModel(caseInstanceEntity.getCaseDefinitionId());
        if (cmmnModel == null) {
            throw new FlowableObjectNotFoundException("No CmmnModel found for case definition id " + caseInstanceEntity.getCaseDefinitionId());
        }

        CaseFileItem fileItem = cmmnModel.getPrimaryCase().getFileModel().findFileItem(fileItemKey);
        if (fileItem == null) {
            throw new FlowableObjectNotFoundException("No case file item found for key " + fileItemKey + " in case definition " + caseInstanceEntity.getCaseDefinitionId());
        }

        FileItemInstanceEntityManager fileItemInstanceEntityManager = CommandContextUtil.getFileItemInstanceEntityManager(commandContext);
        FileItemInstanceEntity fileItemInstanceEntity = fileItemInstanceEntityManager.create();
        fileItemInstanceEntity.setCaseInstanceId(caseInstanceId);
        fileItemInstanceEntity.setCaseDefinitionId(caseInstanceEntity.getCaseDefinitionId());
        fileItemInstanceEntity.setFileItemKey(fileItemKey);
        fileItemInstanceEntity.setDefinitionType(fileItem.getCaseFileItemDefinition().getDefinitionType());
        fileItemInstanceEntity.setContentId(contentId);
        return fileItemInstanceEntity;
    }

}
