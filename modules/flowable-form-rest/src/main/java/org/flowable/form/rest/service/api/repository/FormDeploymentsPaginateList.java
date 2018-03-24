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
package org.flowable.form.rest.service.api.repository;

import java.util.List;

import org.flowable.common.rest.api.AbstractPaginateList;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.rest.FormRestResponseFactory;

/**
 * @author Yvo Swillens
 */
public class FormDeploymentsPaginateList extends AbstractPaginateList<FormDeploymentResponse, FormDeployment> {

    protected FormRestResponseFactory formRestResponseFactory;

    public FormDeploymentsPaginateList(FormRestResponseFactory formRestResponseFactory) {
        this.formRestResponseFactory = formRestResponseFactory;
    }

    @Override
    protected List<FormDeploymentResponse> processList(List<FormDeployment> list) {
        return formRestResponseFactory.createFormDeploymentResponseList(list);
    }
}
