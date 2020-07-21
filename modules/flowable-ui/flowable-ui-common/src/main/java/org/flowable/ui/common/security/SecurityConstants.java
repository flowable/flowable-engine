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
package org.flowable.ui.common.security;

/**
 * @author Filip Hrisafov
 */
public interface SecurityConstants {

    int IDM_API_SECURITY_ORDER = 1;

    int TASK_API_SECURITY_ORDER = 2;

    int MODELER_API_SECURITY_ORDER = 3;

    int ACTUATOR_SECURITY_ORDER = 5; // Actuator configuration should kick in before the Form Login there should always be http basic for the endpoints

    int FORM_LOGIN_SECURITY_ORDER = 10; // API config first (has Order(1))

}
