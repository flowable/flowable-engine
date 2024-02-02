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
package org.flowable.spring.security;

import java.util.List;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Provides core user information within a Flowable application.
 *
 * @author Filip Hrisafov
 * @see UserDetails
 */
public interface FlowableUserDetails extends UserDetails {

    /**
     * The user object containing the information for the Flowable IDM User.
     * If not using the default FlowableUserDetailsService make sure that you are not reusing the User returned by the IDM and that you
     * use a correct serializable User. For example use {@link UserDto}
     */
    User getUser();

    /**
     * The groups of the Flowable IDM User.
     * If not using the default FlowableUserDetailsService make sure that you are not reusing the Groups returned by the IDM and that you
     * use a correct serializable Group. For example use {@link GroupDetails}
     */
    List<Group> getGroups();
}
