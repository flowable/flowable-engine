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
package org.flowable.ui.idm.rest.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.ui.idm.model.SSOUserInfo;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Component;

@Component
public class EmptySSOHandler implements SSOHandler {

    @Override
    public boolean isActive() {
        return false;
    }
    @Override
    public SSOUserInfo handleSsoReturn(HttpServletRequest request, HttpServletResponse response, MultiValueMap<String, String> body) {
        return null;
    }
    @Override
    public String getExternalUrl(String idmUrl) {
        return null;
    }
}