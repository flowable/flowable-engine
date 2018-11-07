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
 * @author Joram Barrez
 */
public class Process extends CmmnElement {
    
    protected String name;
    protected String externalRef;
    protected String implementationType = "http://www.omg.org/spec/CMMN/ProcessType/Unspecified";
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getExternalRef() {
        return externalRef;
    }
    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }
    public String getImplementationType() {
        return implementationType;
    }
    public void setImplementationType(String implementationType) {
        this.implementationType = implementationType;
    }

}
