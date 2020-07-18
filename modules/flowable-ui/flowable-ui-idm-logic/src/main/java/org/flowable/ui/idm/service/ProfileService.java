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
package org.flowable.ui.idm.service;

import java.io.InputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.idm.api.User;

/**
 * @author Joram Barrez
 */
public interface ProfileService {

    User updateProfile(String firstName, String lastName, String email);

    void changePassword(String originalPassword, String newPassword);

    Pair<String, InputStream> getProfilePicture();

    void uploadProfilePicture(String contentType, byte[] bytes);

}
