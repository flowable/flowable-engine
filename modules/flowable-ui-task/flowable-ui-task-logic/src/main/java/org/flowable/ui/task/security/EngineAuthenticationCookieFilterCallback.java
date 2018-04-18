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
package org.flowable.ui.task.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.ui.common.filter.FlowableCookieFilterCallback;
import org.flowable.ui.common.model.RemoteToken;

public class EngineAuthenticationCookieFilterCallback implements FlowableCookieFilterCallback {

    @Override
    public void onValidTokenFound(HttpServletRequest request, HttpServletResponse response, RemoteToken token) {
        if (token != null && token.getUserId() != null) {
            Authentication.setAuthenticatedUserId(token.getUserId());
        }
    }

    @Override
    public void onFilterCleanup(HttpServletRequest request, HttpServletResponse response) {
        Authentication.setAuthenticatedUserId(null);
    }

}
