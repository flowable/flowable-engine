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

/**
 * Element for defining an event listener to hook in to the global event-mechanism.
 *
 * @author Frederik Heremans
 */
public class TransactionEventListener extends EventListener {

    protected String transaction;

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public TransactionEventListener clone() {
        TransactionEventListener clone = new TransactionEventListener();
        clone.setValues(this);
        return clone;
    }

    public void setValues(TransactionEventListener otherListener) {
        setTransaction(otherListener.getTransaction());
    }
}
