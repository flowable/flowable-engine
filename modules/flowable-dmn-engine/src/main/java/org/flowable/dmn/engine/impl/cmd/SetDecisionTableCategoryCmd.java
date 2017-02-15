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
package org.flowable.dmn.engine.impl.cmd;

import org.flowable.dmn.engine.impl.interceptor.Command;
import org.flowable.dmn.engine.impl.persistence.deploy.DecisionTableCacheEntry;
import org.flowable.dmn.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntity;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;

/**
 * @author Joram Barrez
 */
public class SetDecisionTableCategoryCmd implements Command<Void> {

    protected String decisionTableId;
    protected String category;

    public SetDecisionTableCategoryCmd(String decisionTableId, String category) {
        this.decisionTableId = decisionTableId;
        this.category = category;
    }

    public Void execute(org.flowable.dmn.engine.impl.interceptor.CommandContext commandContext) {

        if (decisionTableId == null) {
            throw new FlowableIllegalArgumentException("Decision table id is null");
        }

        DecisionTableEntity decisionTable = commandContext.getDecisionTableEntityManager().findById(decisionTableId);

        if (decisionTable == null) {
            throw new FlowableObjectNotFoundException("No decision table found for id = '" + decisionTableId + "'");
        }

        // Update category
        decisionTable.setCategory(category);

        // Remove process definition from cache, it will be refetched later
        DeploymentCache<DecisionTableCacheEntry> decisionTableCache = commandContext.getDmnEngineConfiguration().getDecisionCache();
        if (decisionTableCache != null) {
            decisionTableCache.remove(decisionTableId);
        }

        commandContext.getDecisionTableEntityManager().update(decisionTable);

        return null;
    }

    public String getDecisionTableId() {
        return decisionTableId;
    }

    public void setDecisionTableId(String decisionTableId) {
        this.decisionTableId = decisionTableId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
