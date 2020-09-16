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
package org.flowable.variable.service.history;

import java.util.Date;

import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public interface InternalHistoryVariableManager {

    /**
     * Record a variable has been created, if audit history is enabled.
     */
    void recordVariableCreate(VariableInstanceEntity variable, Date createTime);

    /**
     * Record a variable has been updated, if audit history is enabled.
     */
    void recordVariableUpdate(VariableInstanceEntity variable, Date updateTime);

    /**
     * Record a variable has been deleted, if audit history is enabled.
     */
    void recordVariableRemoved(VariableInstanceEntity variable, Date removeTime);
    
    void initAsyncHistoryCommandContextCloseListener();
}
