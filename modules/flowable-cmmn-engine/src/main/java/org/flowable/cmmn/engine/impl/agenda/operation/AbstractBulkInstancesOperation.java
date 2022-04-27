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
package org.flowable.cmmn.engine.impl.agenda.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Christopher Welsch
 */
public abstract class AbstractBulkInstancesOperation<T extends CmmnOperation> extends CmmnOperation {

    protected boolean manualTermination;
    protected String exitCriterionId;
    protected String exitType;
    protected String exitEventType;
    protected List<T> caseOperations;

    public AbstractBulkInstancesOperation(CommandContext commandContext, Set<String> caseInstanceIds, boolean manualTermination, String exitCriterionId,
            String exitType, String exitEventType) {

        this.manualTermination = manualTermination;
        this.exitCriterionId = exitCriterionId;
        this.exitType = exitType;
        this.exitEventType = exitEventType;
        this.caseOperations = new ArrayList<>(caseInstanceIds.size());
        this.initCaseInstanceOperations(commandContext, caseInstanceIds);
    }

    protected abstract void initCaseInstanceOperations(CommandContext commandContext, Set<String> caseInstanceIds);

    @Override
    public void run() {
        for (T caseInstanceOperation : caseOperations) {
            caseInstanceOperation.run();
        }
    }

    @Override
    public String getCaseInstanceId() {
        return null;
    }

    public boolean isManualTermination() {
        return manualTermination;
    }

    public void setManualTermination(boolean manualTermination) {
        this.manualTermination = manualTermination;
    }

    public String getExitCriterionId() {
        return exitCriterionId;
    }

    public void setExitCriterionId(String exitCriterionId) {
        this.exitCriterionId = exitCriterionId;
    }

    public String getExitType() {
        return exitType;
    }

    public void setExitType(String exitType) {
        this.exitType = exitType;
    }

    public String getExitEventType() {
        return exitEventType;
    }

    public void setExitEventType(String exitEventType) {
        this.exitEventType = exitEventType;
    }
}
