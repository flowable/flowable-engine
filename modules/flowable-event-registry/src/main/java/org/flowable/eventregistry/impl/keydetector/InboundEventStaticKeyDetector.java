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
package org.flowable.eventregistry.impl.keydetector;

import org.flowable.eventregistry.api.InboundEventKeyDetector;

/**
 * @author Joram Barrez
 */
public class InboundEventStaticKeyDetector<T> implements InboundEventKeyDetector<T> {

    protected String staticKey;

    public InboundEventStaticKeyDetector(String staticKey) {
        this.staticKey = staticKey;
    }

    @Override
    public String detectEventDefinitionKey(T event) {
        return staticKey;
    }

    public String getStaticKey() {
        return staticKey;
    }
    
}
