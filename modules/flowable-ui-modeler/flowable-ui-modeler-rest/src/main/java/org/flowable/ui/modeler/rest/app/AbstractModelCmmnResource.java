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
package org.flowable.ui.modeler.rest.app;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.BaseModelerRestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.domain.ModelHistory;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Tijs Rademakers
 */
public class AbstractModelCmmnResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractModelCmmnResource.class);

    @Autowired
    protected ModelService modelService;

    public void getCaseModelCmmnXml(HttpServletResponse response, String caseModelId) throws IOException {

        if (caseModelId == null) {
            throw new BadRequestException("No case model id provided");
        }

        Model model = modelService.getModel(caseModelId);
        generateCmmnXml(response, model);
    }

    public void getHistoricCaseModelCmmnXml(HttpServletResponse response, String caseModelId, String caseModelHistoryId) throws IOException {

        if (caseModelId == null) {
            throw new BadRequestException("No case model id provided");
        }

        ModelHistory historicModel = modelService.getModelHistory(caseModelId, caseModelHistoryId);
        generateCmmnXml(response, historicModel);
    }

    protected void generateCmmnXml(HttpServletResponse response, AbstractModel model) {
        String name = model.getName().replaceAll(" ", "_");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".cmmn.xml");
        if (model.getModelEditorJson() != null) {
            try {
                ServletOutputStream servletOutputStream = response.getOutputStream();
                response.setContentType("application/xml");

                CmmnModel cmmnModel = modelService.getCmmnModel(model);
                byte[] xmlBytes = modelService.getCmmnXML(cmmnModel);
                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(xmlBytes));

                byte[] buffer = new byte[8096];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    servletOutputStream.write(buffer, 0, count);
                }

                // Flush and close stream
                servletOutputStream.flush();
                servletOutputStream.close();

            } catch (BaseModelerRestException e) {
                throw e;

            } catch (Exception e) {
                LOGGER.error("Could not generate CMMN XML", e);
                throw new InternalServerErrorException("Could not generate CMMN xml");
            }
        }
    }
}
