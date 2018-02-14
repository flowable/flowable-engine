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

package org.flowable.rest.idm.service.api.user;

import java.util.List;

import org.flowable.idm.api.User;
import org.flowable.rest.api.AbstractPaginateList;
import org.flowable.rest.idm.service.api.IdmRestResponseFactory;

public class UserPaginateList extends AbstractPaginateList<UserResponse, User> {

    protected IdmRestResponseFactory idmRestResponseFactory;

    public UserPaginateList(IdmRestResponseFactory idmRestResponseFactory) {
        this.idmRestResponseFactory = idmRestResponseFactory;
    }

    @Override
    protected List<UserResponse> processList(List<User> list) {
        return idmRestResponseFactory.createUserResponseList(list, false);
    }
}
