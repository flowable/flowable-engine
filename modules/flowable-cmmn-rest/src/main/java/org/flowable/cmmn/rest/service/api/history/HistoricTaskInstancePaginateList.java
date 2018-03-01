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

package org.flowable.cmmn.rest.service.api.history;

import java.util.List;

import org.flowable.cmmn.rest.service.api.RestResponseFactory;
import org.flowable.rest.api.AbstractPaginateList;
import org.flowable.task.api.history.HistoricTaskInstance;

/**
 * @author Tijs Rademakers
 */
public class HistoricTaskInstancePaginateList extends AbstractPaginateList<HistoricTaskInstanceResponse, HistoricTaskInstance> {

    protected RestResponseFactory restResponseFactory;
    protected String serverRootUrl;

    public HistoricTaskInstancePaginateList(RestResponseFactory restResponseFactory, String serverRootUrl) {
        this.restResponseFactory = restResponseFactory;
        this.serverRootUrl = serverRootUrl;
    }

    @Override
    protected List<HistoricTaskInstanceResponse> processList(List<HistoricTaskInstance> list) {
        return restResponseFactory.createHistoricTaskInstanceResponseList(list);
    }
}
