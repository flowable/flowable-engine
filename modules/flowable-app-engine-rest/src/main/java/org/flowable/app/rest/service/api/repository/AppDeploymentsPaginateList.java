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
package org.flowable.app.rest.service.api.repository;

import java.util.List;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestResponseFactory;
import org.flowable.common.rest.api.AbstractPaginateList;

/**
 * @author Tijs Rademakers
 */
public class AppDeploymentsPaginateList extends AbstractPaginateList<AppDeploymentResponse, AppDeployment> {

    protected AppRestResponseFactory appRestResponseFactory;

    public AppDeploymentsPaginateList(AppRestResponseFactory appRestResponseFactory) {
        this.appRestResponseFactory = appRestResponseFactory;
    }

    @Override
    protected List<AppDeploymentResponse> processList(List<AppDeployment> list) {
        return appRestResponseFactory.createAppDeploymentResponseList(list);
    }
}
