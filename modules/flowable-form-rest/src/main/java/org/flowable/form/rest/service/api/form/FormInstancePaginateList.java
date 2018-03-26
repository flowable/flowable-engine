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
package org.flowable.form.rest.service.api.form;

import java.util.List;

import org.flowable.common.rest.api.AbstractPaginateList;
import org.flowable.form.api.FormInstance;
import org.flowable.form.rest.FormRestResponseFactory;

/**
 * @author Yvo Swillens
 */
public class FormInstancePaginateList extends AbstractPaginateList<FormInstanceResponse, FormInstance> {

    protected FormRestResponseFactory restResponseFactory;

    public FormInstancePaginateList(FormRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @Override
    protected List<FormInstanceResponse> processList(List<FormInstance> list) {
        return restResponseFactory.createFormInstanceResponse(list);
    }
}
