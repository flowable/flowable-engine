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

package org.flowable.common.engine.api;

/**
 * An exception indicating that an API call could not be executed due to data being in the wrong state.
 *
 * @author Joram Barrez
 */
public class FlowableIllegalStateException extends FlowableException {

    private static final long serialVersionUID = 1L;

    public FlowableIllegalStateException(String message) {
        super(message);
    }

    public FlowableIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
