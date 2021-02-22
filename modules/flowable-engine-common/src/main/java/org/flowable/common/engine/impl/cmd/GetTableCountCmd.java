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

import java.io.Serializable;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tom Baeyens
 */
public class GetTableCountCmd implements Command<Map<String, Long>>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String engineType;
    
    public GetTableCountCmd(String engineType) {
        this.engineType = engineType;
    }

    @Override
    public Map<String, Long> execute(CommandContext commandContext) {
        return commandContext.getEngineConfigurations().get(engineType).getTableDataManager().getTableCount();
    }
}
