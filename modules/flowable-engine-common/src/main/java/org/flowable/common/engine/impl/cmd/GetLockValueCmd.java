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

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;

/**
 * @author Filip Hrisafov
 */
public class GetLockValueCmd implements Command<String> {

    protected String lockName;
    protected String engineType;

    public GetLockValueCmd(String lockName, String engineType) {
        this.lockName = lockName;
        this.engineType = engineType;
    }

    @Override
    public String execute(CommandContext commandContext) {
        PropertyEntity lockProperty = commandContext.getEngineConfigurations().get(engineType).getPropertyEntityManager().findById(lockName);

        if (lockProperty != null) {
            return lockProperty.getValue();
        } else {
            return "UNKNOWN";
        }
    }
}
