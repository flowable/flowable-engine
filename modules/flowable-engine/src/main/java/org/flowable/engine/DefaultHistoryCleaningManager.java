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
package org.flowable.engine;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

public class DefaultHistoryCleaningManager implements HistoryCleaningManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    public DefaultHistoryCleaningManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public HistoricProcessInstanceQueryImpl createHistoricProcessInstanceCleaningQuery() {
        int days = processEngineConfiguration.getCleanInstancesEndedAfterNumberOfDays();
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        HistoricProcessInstanceQueryImpl historicProcessInstanceQuery = new HistoricProcessInstanceQueryImpl(
                processEngineConfiguration.getCommandExecutor(), processEngineConfiguration);
        historicProcessInstanceQuery.finishedBefore(cal.getTime());
        return historicProcessInstanceQuery;
    }
}
