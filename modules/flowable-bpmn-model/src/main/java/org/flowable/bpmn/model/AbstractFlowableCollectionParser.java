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
package org.flowable.bpmn.model;

import java.util.Collection;

/**
 * @author Lori Small
 */
public abstract class AbstractFlowableCollectionParser extends BaseElement {

    protected String implementationType;
    protected String implementation;

    public String getImplementationType() {
        return implementationType;
    }

    public void setImplementationType(String implementationType) {
        this.implementationType = implementationType;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    @SuppressWarnings("rawtypes")
	public abstract Collection parse(String collectionString);

    public abstract AbstractFlowableCollectionParser clone();

    public void setValues(AbstractFlowableCollectionParser otherParser) {
        super.setValues(otherParser);
        setImplementation(otherParser.getImplementation());
        setImplementationType(otherParser.getImplementationType());
    }
}
