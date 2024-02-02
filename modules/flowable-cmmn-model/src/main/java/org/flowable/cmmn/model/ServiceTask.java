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

import java.util.ArrayList;

/**
 * @author Tijs Rademakers
 */
public class ServiceTask extends TaskWithFieldExtensions {

    public static final String JAVA_TASK = "java";
    public static final String MAIL_TASK = "mail";

    protected String implementation;
    protected String implementationType;
    protected String resultVariableName;
    protected String type;
    protected boolean storeResultVariableAsTransient;

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getImplementationType() {
        return implementationType;
    }

    public void setImplementationType(String implementationType) {
        this.implementationType = implementationType;
    }

    public String getResultVariableName() {
        return resultVariableName;
    }

    public void setResultVariableName(String resultVariableName) {
        this.resultVariableName = resultVariableName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isStoreResultVariableAsTransient() {
        return storeResultVariableAsTransient;
    }

    public void setStoreResultVariableAsTransient(boolean storeResultVariableAsTransient) {
        this.storeResultVariableAsTransient = storeResultVariableAsTransient;
    }

    @Override
    public ServiceTask clone() {
        ServiceTask clone = new ServiceTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(ServiceTask otherElement) {
        super.setValues(otherElement);
        setImplementation(otherElement.getImplementation());
        setImplementationType(otherElement.getImplementationType());
        setResultVariableName(otherElement.getResultVariableName());
        setType(otherElement.getType());
        setStoreResultVariableAsTransient(otherElement.isStoreResultVariableAsTransient());

        fieldExtensions = new ArrayList<>();
        if (otherElement.getFieldExtensions() != null && !otherElement.getFieldExtensions().isEmpty()) {
            for (FieldExtension extension : otherElement.getFieldExtensions()) {
                fieldExtensions.add(extension.clone());
            }
        }
    }
}
