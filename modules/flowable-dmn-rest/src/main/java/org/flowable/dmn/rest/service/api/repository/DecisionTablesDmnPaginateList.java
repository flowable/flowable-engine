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
package org.flowable.dmn.rest.service.api.repository;

import java.util.List;

import org.flowable.common.rest.api.AbstractPaginateList;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;

/**
 * @author Yvo Swillens
 */
public class DecisionTablesDmnPaginateList extends AbstractPaginateList<DecisionTableResponse, DmnDecisionTable> {

    protected DmnRestResponseFactory dmnRestResponseFactory;

    public DecisionTablesDmnPaginateList(DmnRestResponseFactory dmnRestResponseFactory) {
        this.dmnRestResponseFactory = dmnRestResponseFactory;
    }

    @Override
    protected List<DecisionTableResponse> processList(List<DmnDecisionTable> list) {
        return dmnRestResponseFactory.createDecisionTableResponseList(list);
    }
}
