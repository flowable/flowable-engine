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
package org.flowable.cmmn.model;

/**
 * @author Tijs Rademakers
 */
public class Association extends BaseElement {

    protected String sourceRef;
    protected BaseElement sourceElement;
    protected String targetRef;
    protected BaseElement targetElement;
    protected String transitionEvent;

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public BaseElement getSourceElement() {
        return sourceElement;
    }

    public void setSourceElement(BaseElement sourceElement) {
        this.sourceElement = sourceElement;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    public BaseElement getTargetElement() {
        return targetElement;
    }

    public void setTargetElement(BaseElement targetElement) {
        this.targetElement = targetElement;
    }
    
    public String getTransitionEvent() {
        return transitionEvent;
    }

    public void setTransitionEvent(String transitionEvent) {
        this.transitionEvent = transitionEvent;
    }

    @Override
    public Association clone() {
        Association clone = new Association();
        clone.setValues(this);
        return clone;
    }

    public void setValues(Association otherElement) {
        super.setValues(otherElement);
        setSourceRef(otherElement.getSourceRef());
        setTargetRef(otherElement.getTargetRef());
    }
}
