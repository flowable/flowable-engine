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
import java.util.List;

/**
 * @author Valentin Zickner
 */
public class ChildTask extends Task {

    protected String businessKey;
    protected boolean inheritBusinessKey;
    protected List<IOParameter> inParameters = new ArrayList<>();
    protected List<IOParameter> outParameters = new ArrayList<>();

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public boolean isInheritBusinessKey() {
        return inheritBusinessKey;
    }

    public void setInheritBusinessKey(boolean inheritBusinessKey) {
        this.inheritBusinessKey = inheritBusinessKey;
    }

    public List<IOParameter> getInParameters() {
        return inParameters;
    }

    public void setInParameters(List<IOParameter> inParameters) {
        this.inParameters = inParameters;
    }

    public List<IOParameter> getOutParameters() {
        return outParameters;
    }

    public void setOutParameters(List<IOParameter> outParameters) {
        this.outParameters = outParameters;
    }

}
