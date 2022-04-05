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
package org.flowable.eventregistry.impl;

import java.util.Map;

import org.flowable.eventregistry.api.FlowableEventInfo;

public class FlowableEventInfoImpl<T> implements FlowableEventInfo<T> {
    
    protected Map<String, Object> headers;
    protected T payload;
    
    public FlowableEventInfoImpl(Map<String, Object> headers, T payload) {
        this.headers = headers;
        this.payload = payload;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return headers;
    }
    
    @Override
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    @Override
    public T getPayload() {
        return payload;
    }

}
