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

import java.util.List;

/**
 * @author Tijs Rademakers
 */
public interface HasAssociations {
    
    public void addIncomingAssociation(Association association);
    
    public List<Association> getIncomingAssociations();
    
    public void setIncomingAssociations(List<Association> incomingAssociations);
    
    public void addOutgoingAssociation(Association association);
    
    public List<Association> getOutgoingAssociations();
    
    public void setOutgoingAssociations(List<Association> outgoingAssociations);

}
