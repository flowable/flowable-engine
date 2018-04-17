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
package org.flowable.ui.idm.rest.api;

import org.flowable.idm.api.Token;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.idm.model.TokenRepresentation;
import org.flowable.ui.idm.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiTokensResource {

    @Autowired
    protected TokenService tokenService;

    @RequestMapping(value = "/idm/tokens/{tokenId}", method = RequestMethod.GET, produces = { "application/json" })
    public TokenRepresentation getToken(@PathVariable String tokenId) {
        Token token = tokenService.findTokenById(tokenId);
        if (token == null) {
            throw new NotFoundException();
        } else {
            return new TokenRepresentation(token);
        }
    }

}
