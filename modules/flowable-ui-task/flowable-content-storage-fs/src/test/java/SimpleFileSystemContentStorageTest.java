import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.activiti.content.storage.api.ContentMetaDataKeys;
import org.activiti.content.storage.api.ContentObject;
import org.activiti.content.storage.api.ContentStorage;
import org.activiti.content.storage.fs.SimpleFileSystemContentStorage;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

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

/**
 * @author Joram Barrez
 */
public class SimpleFileSystemContentStorageTest {
  
  protected ContentStorage contentStorage;
  protected File rootFolder;
  
  @Before
  public void setup() throws Exception {
    rootFolder = Files.createTempDirectory("flowable-test").toFile();
    
    // Clean all content of the root folder
    File[] subFolders = rootFolder.listFiles();
    for (File subFolder : subFolders) {
      subFolder.delete();
    }
    
    this.contentStorage = new SimpleFileSystemContentStorage(rootFolder);
  }
  
  @Test
  public void testDefaultSubFoldersCreatedObBoot() {
    File[] subFolder = rootFolder.listFiles();
    assertEquals(3, subFolder.length);
  }
  
  @Test
  public void testCreateContent() {
    
    int fileCountAtStart = 0;
    
    // No metadata -> in uncategorized folder
    Map<String, Object> metaData = new HashMap<String, Object>();
    ContentObject contentObject = contentStorage.createContentObject(
        new ByteArrayInputStream("Hello".getBytes()), null, metaData);
    assertNotNull(contentObject);
    assertTrue(contentObject.getContentLength() > 0);
    assertNotNull(contentObject.getId());
    assertEquals(fileCountAtStart + 1, countFilesInFolder(rootFolder));
    assertEquals(1, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_UNCATEGORIZED)));
    
    // task id -> in task folder
    metaData.put(ContentMetaDataKeys.TASK_ID, "abc");
    contentObject = contentStorage.createContentObject(
        new ByteArrayInputStream("Hello2".getBytes()), null, metaData);
    assertEquals(fileCountAtStart + 2, countFilesInFolder(rootFolder));
    assertEquals(1, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_TASK)));
    assertTrue(new File(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_TASK), "abc").exists());
    
    metaData.put(ContentMetaDataKeys.TASK_ID, "abc2");
    contentObject = contentStorage.createContentObject(
        new ByteArrayInputStream("Hello3".getBytes()), null, metaData);
    assertEquals(2, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_TASK)));
    
    // process instance id -> in proc inst folder
    metaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, "def");
    contentObject = contentStorage.createContentObject(
        new ByteArrayInputStream("Hello4".getBytes()), null, metaData);
    assertEquals(2, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_TASK)));
    assertEquals(1, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_PROCESS_INSTANCE)));
  }
  
  @Test
  public void getContent() throws Exception {
    Map<String, Object> metaData = new HashMap<String, Object>();
    metaData.put(ContentMetaDataKeys.TASK_ID, "abc");
    ContentObject contentObject = contentStorage.createContentObject(
        new ByteArrayInputStream("Hello World".getBytes()), null, metaData);
    
    ContentObject contentObjectFromStorage = contentStorage.getContentObject(contentObject.getId());
    assertEquals(contentObject.getId(), contentObjectFromStorage.getId());
    assertEquals(contentObject.getContentLength(), contentObjectFromStorage.getContentLength());
    
    String content = IOUtils.toString(contentObjectFromStorage.getContent());
    assertEquals("Hello World", content);
  }
  
  @Test
  public void testDeleteContent() {
    Map<String, Object> metaData = new HashMap<>();
    metaData.put(ContentMetaDataKeys.PROCESS_INSTANCE_ID, "def");
    ContentObject contentObject = null;
    for (int i=0; i<5; i++ ) {
      contentObject = contentStorage.createContentObject(
        new ByteArrayInputStream("Hello4".getBytes()), null, metaData);
    }
    assertEquals(5, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_PROCESS_INSTANCE)));
    
    contentStorage.deleteContentObject(contentObject.getId());
    assertEquals(4, countFilesInFolder(new File(rootFolder, SimpleFileSystemContentStorage.TYPE_PROCESS_INSTANCE)));
  }
  
  
  // HELPERS
  
  protected int countFilesInFolder(File folder) {
    int count = 0;
    LinkedList<File> foldersToCheck = new LinkedList<File>();
    foldersToCheck.add(folder);
    
    while (!foldersToCheck.isEmpty()) {
      File currentFile = foldersToCheck.pop();
      if (currentFile.isDirectory()) {
        for (File file : currentFile.listFiles()) {
          if (file.isDirectory()) {
            foldersToCheck.add(file);
          } else {
            count++;
          }
        }
      }
    }
    return count;
  }
  
}
