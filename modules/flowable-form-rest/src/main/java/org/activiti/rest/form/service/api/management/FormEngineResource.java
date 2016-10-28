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
package org.activiti.rest.form.service.api.management;

import org.activiti.form.engine.ActivitiFormException;
import org.activiti.form.engine.FormEngine;
import org.activiti.form.engine.FormEngineInfo;
import org.activiti.form.engine.FormEngines;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yvo Swillens
 */
@RestController
public class FormEngineResource {

    @Autowired
    protected FormEngine formEngine;

    @RequestMapping(value = "/form-management/engine", method = RequestMethod.GET, produces = "application/json")
    public FormEngineInfoResponse getEngineInfo() {
        FormEngineInfoResponse response = new FormEngineInfoResponse();

        try {
            FormEngineInfo formEngineInfo = FormEngines.getFormEngineInfo(formEngine.getName());
            if (formEngineInfo != null) {
                response.setName(formEngineInfo.getName());
                response.setResourceUrl(formEngineInfo.getResourceUrl());
                response.setException(formEngineInfo.getException());
            } else {
                response.setName(formEngine.getName());
            }
        } catch (Exception e) {
            throw new ActivitiFormException("Error retrieving form engine info", e);
        }

        response.setVersion(FormEngine.VERSION);

        return response;
    }
}
