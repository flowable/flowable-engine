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
package org.flowable.ui.idm.model;

import java.util.Date;

import org.flowable.idm.api.Token;
import org.flowable.ui.common.model.AbstractRepresentation;

public class TokenRepresentation extends AbstractRepresentation {

    protected String id;
    protected String value;
    protected Date date;
    protected String userId;
    protected String data;

    public TokenRepresentation() {

    }

    public TokenRepresentation(Token token) {
        setId(token.getId());
        setValue(token.getTokenValue());
        setDate(token.getTokenDate());
        setUserId(token.getUserId());
        setData(token.getTokenData());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
