/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.activiti.rest.form.service.api.form;

import org.activiti.form.model.FormDefinition;

/**
 * @author Yvo Swillens
 */
public class RuntimeFormDefinitionResponse extends FormDefinition {

  private String url;

  public RuntimeFormDefinitionResponse(FormDefinition formDefinition) {
    setId(formDefinition.getId());
    setName(formDefinition.getName());
    setDescription(formDefinition.getDescription());
    setKey(formDefinition.getKey());
    setVersion(formDefinition.getVersion());
    setFields(formDefinition.getFields());
    setOutcomes(formDefinition.getOutcomes());
    setOutcomeVariableName(formDefinition.getOutcomeVariableName());
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
