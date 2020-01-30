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
package org.flowable.eventregistry.impl.tenantdetector;

import org.flowable.eventregistry.api.InboundEventTenantDetector;

/**
 * @author Joram Barrez
 */
public class InboundEventStaticTenantDetector<T> implements InboundEventTenantDetector<T> {

    protected String staticTenantId;

    public InboundEventStaticTenantDetector(String staticTenantId) {
        this.staticTenantId = staticTenantId;
    }

    @Override
    public String detectTenantId(T event) {
        return staticTenantId;
    }

    public String getStaticTenantId() {
        return staticTenantId;
    }
}
