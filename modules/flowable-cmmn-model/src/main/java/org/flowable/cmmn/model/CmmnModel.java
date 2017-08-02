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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Joram Barrez
 */
public class CmmnModel {
    
    protected String id;
    protected String name;
    protected String targetNamespace;
    protected String expressionLanguage;
    protected String exporter;
    protected String exporterVersion;
    protected String author;
    protected Date creationDate;
    
    protected List<Case> cases = new ArrayList<>();
    
    public void addCase(Case caze) {
        cases.add(caze);
    }
    
    public Case getPrimaryCase() {
        return cases.get(0);
    }
    
    public Case getCaseById(String id) {
        for (Case caze : cases) {
            if (id.equals(caze.getId())) {
                return caze;
            }
        }
        return null;
    }
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getTargetNamespace() {
        return targetNamespace;
    }
    public void setTargetNamespace(String targetNamespace) {
        this.targetNamespace = targetNamespace;
    }
    public String getExpressionLanguage() {
        return expressionLanguage;
    }
    public void setExpressionLanguage(String expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }
    public String getExporter() {
        return exporter;
    }
    public void setExporter(String exporter) {
        this.exporter = exporter;
    }
    public String getExporterVersion() {
        return exporterVersion;
    }
    public void setExporterVersion(String exporterVersion) {
        this.exporterVersion = exporterVersion;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public Date getCreationDate() {
        return creationDate;
    }
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    public List<Case> getCases() {
        return cases;
    }
    public void setCases(List<Case> cases) {
        this.cases = cases;
    }
    
}
