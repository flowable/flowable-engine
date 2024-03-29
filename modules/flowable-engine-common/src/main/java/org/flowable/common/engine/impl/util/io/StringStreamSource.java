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
package org.flowable.common.engine.impl.util.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Tom Baeyens
 */
public class StringStreamSource implements StreamSource {

    protected final String string;
    protected final Charset byteArrayEncoding;

    public StringStreamSource(String string) {
        this(string, StandardCharsets.UTF_8);
    }

    public StringStreamSource(String string, String byteArrayEncoding) {
        this.string = string;
        this.byteArrayEncoding = Charset.forName(byteArrayEncoding);
    }

    public StringStreamSource(String string, Charset byteArrayEncoding) {
        this.string = string;
        this.byteArrayEncoding = byteArrayEncoding;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(byteArrayEncoding == null ? string.getBytes() : string.getBytes(byteArrayEncoding));
    }

    @Override
    public String toString() {
        return "String";
    }
}
