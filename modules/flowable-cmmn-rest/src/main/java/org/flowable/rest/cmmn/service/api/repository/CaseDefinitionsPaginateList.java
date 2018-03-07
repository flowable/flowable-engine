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

package org.flowable.rest.cmmn.service.api.repository;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.rest.api.AbstractPaginateList;
import org.flowable.rest.cmmn.service.api.CmmnRestResponseFactory;

/**
 * @author Tijs Rademakers
 */
public class CaseDefinitionsPaginateList extends AbstractPaginateList<CaseDefinitionResponse, CaseDefinition> {

    protected CmmnRestResponseFactory restResponseFactory;

    public CaseDefinitionsPaginateList(CmmnRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @Override
    protected List<CaseDefinitionResponse> processList(List<CaseDefinition> list) {
        return restResponseFactory.createCaseDefinitionResponseList(list);
    }
}
