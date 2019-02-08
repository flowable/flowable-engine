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
package org.flowable.content.engine.impl.storage.db;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.flowable.content.api.ContentObject;

/**
 * {@link ContentObject}, backed by a byte array.
 * 
 * @author Jorge Moraleda
 */
public class DatabaseContentObject implements ContentObject {

    protected byte[] data;
    protected InputStream inputStream;
    protected String id;

    public DatabaseContentObject(byte[] data, String id) {
        this.data = data;
        this.id = id;
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getContentLength() {
        return data.length;
    }

    @Override
    public InputStream getContent() {
        if (inputStream == null)
            inputStream = new ByteArrayInputStream(data);
           
        return inputStream;
    }

}
