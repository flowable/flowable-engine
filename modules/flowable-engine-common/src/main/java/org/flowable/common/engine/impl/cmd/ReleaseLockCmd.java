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
package org.flowable.common.engine.impl.cmd;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;

/**
 * @author Filip Hrisafov
 */
public class ReleaseLockCmd implements Command<Void> {

    protected String lockName;
    protected String engineType;

    public ReleaseLockCmd(String lockName, String engineType) {
        this.lockName = lockName;
        this.engineType = engineType;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        PropertyEntityManager propertyEntityManager = commandContext.getEngineConfigurations().get(engineType).getPropertyEntityManager();
        PropertyEntity property = propertyEntityManager.findById(lockName);
        if (property != null) {
            property.setValue(null);
            return null;
        } else {
            throw new FlowableObjectNotFoundException("Lock with name " + lockName + " does not exist");
        }
    }
}
