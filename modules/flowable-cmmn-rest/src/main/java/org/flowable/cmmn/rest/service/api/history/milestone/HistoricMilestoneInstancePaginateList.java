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

package org.flowable.cmmn.rest.service.api.history.milestone;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.rest.api.AbstractPaginateList;

import java.util.List;

/**
 * @author Tijs Rademakers
 * @author DennisFederico
 */
public class HistoricMilestoneInstancePaginateList extends AbstractPaginateList<HistoricMilestoneInstanceResponse, HistoricMilestoneInstance> {

    protected CmmnRestResponseFactory restResponseFactory;

    public HistoricMilestoneInstancePaginateList(CmmnRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @Override
    protected List<HistoricMilestoneInstanceResponse> processList(List<HistoricMilestoneInstance> list) {
        return restResponseFactory.createHistoricMilestoneInstanceResponseList(list);
    }
}
