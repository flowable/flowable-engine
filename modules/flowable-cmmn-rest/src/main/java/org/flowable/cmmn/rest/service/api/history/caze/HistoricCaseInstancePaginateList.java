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

package org.flowable.cmmn.rest.service.api.history.caze;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.rest.api.AbstractPaginateList;

/**
 * @author Tijs Rademakers
 */
public class HistoricCaseInstancePaginateList extends AbstractPaginateList<HistoricCaseInstanceResponse, HistoricCaseInstance> {

    protected CmmnRestResponseFactory restResponseFactory;

    public HistoricCaseInstancePaginateList(CmmnRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @Override
    protected List<HistoricCaseInstanceResponse> processList(List<HistoricCaseInstance> list) {
        return restResponseFactory.createHistoricCaseInstanceResponseList(list);
    }
}
