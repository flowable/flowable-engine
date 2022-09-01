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
package org.flowable.http.common.api;

/**
 * @author Filip Hrisafov
 */
public class MultiValuePart {

    protected final String name;
    protected final Object body;
    protected final String filename;

    protected MultiValuePart(String name, Object body, String filename) {
        this.name = name;
        this.body = body;
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public Object getBody() {
        return body;
    }

    public String getFilename() {
        return filename;
    }

    public static MultiValuePart fromText(String name, String value) {
        return new MultiValuePart(name, value, null);
    }

    public static MultiValuePart fromFile(String name, byte[] value, String filename) {
        return new MultiValuePart(name, value, filename);
    }
}
