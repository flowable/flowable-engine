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
package org.flowable.common.engine.impl.identity;

import java.security.Principal;

/**
 * A simple user id principal that has the user id as its name.
 *
 * @author Filip Hrisafov
 */
public class UserIdPrincipal implements Principal {

    private final String userId;

    public UserIdPrincipal(String userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId;
    }
}
