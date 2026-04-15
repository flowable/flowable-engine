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
package org.flowable.cmmn.engine.impl.runtime;

import java.util.Date;

import org.flowable.cmmn.api.runtime.CaseInstanceUpdateBuilder;

/**
 * @author Tijs Rademakers
 */
public class CaseInstanceUpdateBuilderImpl implements CaseInstanceUpdateBuilder {

    protected CmmnRuntimeServiceImpl cmmnRuntimeService;
    protected String caseInstanceId;
    protected String businessKey;
    protected boolean businessKeySet;
    protected String businessStatus;
    protected boolean businessStatusSet;
    protected String name;
    protected boolean nameSet;
    protected Date dueDate;
    protected boolean dueDateSet;

    public CaseInstanceUpdateBuilderImpl(CmmnRuntimeServiceImpl cmmnRuntimeService, String caseInstanceId) {
        this.cmmnRuntimeService = cmmnRuntimeService;
        this.caseInstanceId = caseInstanceId;
    }

    @Override
    public CaseInstanceUpdateBuilder businessKey(String businessKey) {
        this.businessKey = businessKey;
        this.businessKeySet = true;
        return this;
    }

    @Override
    public CaseInstanceUpdateBuilder businessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
        this.businessStatusSet = true;
        return this;
    }

    @Override
    public CaseInstanceUpdateBuilder name(String name) {
        this.name = name;
        this.nameSet = true;
        return this;
    }

    @Override
    public CaseInstanceUpdateBuilder dueDate(Date dueDate) {
        this.dueDate = dueDate;
        this.dueDateSet = true;
        return this;
    }

    @Override
    public void update() {
        cmmnRuntimeService.updateCaseInstance(this);
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public boolean isBusinessKeySet() {
        return businessKeySet;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public boolean isBusinessStatusSet() {
        return businessStatusSet;
    }

    public String getName() {
        return name;
    }

    public boolean isNameSet() {
        return nameSet;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public boolean isDueDateSet() {
        return dueDateSet;
    }
}
