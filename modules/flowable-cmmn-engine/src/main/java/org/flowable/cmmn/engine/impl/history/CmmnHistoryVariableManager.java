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
package org.flowable.cmmn.engine.impl.history;

import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryVariableManager implements InternalHistoryVariableManager {
    
    protected CmmnHistoryManager cmmnHistoryManager;
    
    public CmmnHistoryVariableManager(CmmnHistoryManager cmmnHistoryManager) {
        this.cmmnHistoryManager = cmmnHistoryManager;
    }

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        cmmnHistoryManager.recordVariableCreate(variable);
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variable) {
        cmmnHistoryManager.recordVariableUpdate(variable);
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variable) {
        cmmnHistoryManager.recordVariableRemoved(variable);
    }

}
