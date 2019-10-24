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

public class CasePageTask extends Task {

    public static final String TYPE = "casePage";
    
    protected String type;
    protected String label;
    protected String icon;

    public CasePageTask() {
        type = TYPE;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public CasePageTask clone() {
        CasePageTask clone = new CasePageTask();
        clone.setValues(this);
        return clone;
    }

    public void setValues(CasePageTask otherElement) {
        super.setValues(otherElement);
        
        setType(otherElement.getType());
        setLabel(otherElement.getLabel());
        setIcon(otherElement.getIcon());
    }

}
