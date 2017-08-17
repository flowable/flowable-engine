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
package org.flowable.cmmn.engine.impl;

import org.flowable.cmmn.engine.CmmnHistoryService;
import org.flowable.cmmn.engine.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.engine.history.HistoricMilestoneInstanceQuery;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryServiceImpl extends ServiceImpl implements CmmnHistoryService {
    
    @Override
    public HistoricCaseInstanceQuery createHistoricCaseInstanceQuery() {
        return cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().createHistoricCaseInstanceQuery();
    }

    @Override
    public HistoricMilestoneInstanceQuery createHistoricMilestoneInstanceQuery() {
        return cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager().createHistoricMilestoneInstanceQuery();
    }
    
}
