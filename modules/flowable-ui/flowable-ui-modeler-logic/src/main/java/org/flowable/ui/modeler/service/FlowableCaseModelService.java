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
package org.flowable.ui.modeler.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.model.casemodel.CaseRepresentation;
import org.flowable.ui.modeler.repository.ModelSort;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableCaseModelService extends BaseFlowableModelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableCaseModelService.class);

    protected static final int MIN_FILTER_LENGTH = 1;

    @Autowired
    protected ModelService modelService;

    public ResultListDataRepresentation getCases(String filter, String excludeId) {
        String validFilter = makeValidFilterText(filter);

        List<Model> models = null;

        if (validFilter != null) {
            models = modelRepository.findByModelTypeAndFilter(AbstractModel.MODEL_TYPE_CMMN, validFilter, ModelSort.NAME_ASC);

        } else {
            models = modelRepository.findByModelType(AbstractModel.MODEL_TYPE_CMMN, ModelSort.NAME_ASC);
        }

        List<CaseRepresentation> reps = new ArrayList<>();

        for (Model model : models) {
            if (excludeId == null || !model.getId().equals(excludeId)) {
                reps.add(new CaseRepresentation(model));
            }
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(reps);
        result.setTotal(Long.valueOf(models.size()));
        return result;
    }

    protected String makeValidFilterText(String filterText) {
        String validFilter = null;

        if (filterText != null) {
            String trimmed = StringUtils.trim(filterText);
            if (trimmed.length() >= MIN_FILTER_LENGTH) {
                validFilter = "%" + trimmed.toLowerCase() + "%";
            }
        }
        return validFilter;
    }
}
