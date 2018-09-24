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
package org.flowable.common.engine.impl.db;

/**
 * Contains a predefined set of states for process definitions and process instances
 * 
 * @author Daniel Meyer
 */
public interface SuspensionState {

    SuspensionState ACTIVE = new SuspensionStateImpl(1, "active");
    SuspensionState SUSPENDED = new SuspensionStateImpl(2, "suspended");

    int getStateCode();

    // default implementation ///////////////////////////////////////////////////

    static class SuspensionStateImpl implements SuspensionState {

        public final int stateCode;
        protected final String name;

        public SuspensionStateImpl(int suspensionCode, String string) {
            this.stateCode = suspensionCode;
            this.name = string;
        }

        @Override
        public int getStateCode() {
            return stateCode;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + stateCode;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SuspensionStateImpl other = (SuspensionStateImpl) obj;
            return stateCode == other.stateCode;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
