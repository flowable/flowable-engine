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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class FileItemInstanceEntityImpl extends AbstractCmmnEngineEntity implements FileItemInstanceEntity {

    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected String fileItemKey;
    protected String definitionType;
    protected String contentId;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("fileItemKey", fileItemKey);
        persistentState.put("definitionType", definitionType);
        persistentState.put("contentId", contentId);
        return persistentState;
    }

    @Override
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    @Override
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    @Override
    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }
    @Override
    public String getFileItemKey() {
        return fileItemKey;
    }
    @Override
    public void setFileItemKey(String fileItemKey) {
        this.fileItemKey = fileItemKey;
    }
    @Override
    public String getDefinitionType() {
        return definitionType;
    }
    @Override
    public void setDefinitionType(String definitionType) {
        this.definitionType = definitionType;
    }
    @Override
    public String getContentId() {
        return contentId;
    }
    @Override
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

}
