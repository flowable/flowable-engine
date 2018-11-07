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
package org.flowable.content.engine.impl.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.flowable.content.api.ContentObject;
import org.flowable.content.api.ContentStorageException;

/**
 * 
 * {@link ContentObject}, backed by a file.
 * 
 * @author Frederik Heremans
 */
public class FileSystemContentObject implements ContentObject {

    protected File file;
    protected InputStream inputStream;
    protected String id;
    protected Long length;

    public FileSystemContentObject(File file, String id) {
        this.file = file;
        this.id = id;
    }

    public FileSystemContentObject(File file, String id, Long length) {
        this(file, id);
        this.length = length;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getContentLength() {
        if (length == null) {
            length = file.length();
        }
        return length;
    }

    @Override
    public InputStream getContent() {
        if (inputStream == null) {
            try {
                inputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new ContentStorageException("Error while opening file stream", e);
            }
        }
        return inputStream;
    }

}
