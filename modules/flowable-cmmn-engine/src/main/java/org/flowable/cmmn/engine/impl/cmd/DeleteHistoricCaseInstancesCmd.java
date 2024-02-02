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

import java.io.Serializable;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.HistoricCaseInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class DeleteHistoricCaseInstancesCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected HistoricCaseInstanceQueryImpl historicCaseInstanceQuery;

    public DeleteHistoricCaseInstancesCmd(HistoricCaseInstanceQueryImpl historicCaseInstanceQuery) {
        this.historicCaseInstanceQuery = historicCaseInstanceQuery;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        if (historicCaseInstanceQuery == null) {
            throw new FlowableIllegalArgumentException("query is null");
        }
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().deleteHistoricCaseInstances(historicCaseInstanceQuery);

        return null;
    }

}
