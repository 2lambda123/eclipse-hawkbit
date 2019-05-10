/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.Test;
import org.springframework.http.MediaType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Spring MVC Tests against the MgmtTenantManagementResource.
 *
 */
@Feature("Component Tests - Management API")
@Story("Tenant Management Resource")
public class MgmtTenantManagementResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    @Description("Multiassignment can not be deactivated.")
    public void deactivateMultiassignment() throws Exception {
        final String multiAssignmentKey = "multi.assignments.enabled";
        final String bodyActivate = new JSONObject().put("value", true).toString();
        final String bodyDeactivate = new JSONObject().put("value", false).toString();

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", multiAssignmentKey)
                .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", multiAssignmentKey)
                .content(bodyDeactivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("The 'repository.actions.autoclose.enabled' property must not be modified if Multi-Assignments is enabled.")
    public void autoCloseCannotBeModifiedIfMultiAssignmentIsEnabled() throws Exception {
        final String multiAssignmentKey = "multi.assignments.enabled";
        final String autoCloseKey = "repository.actions.autoclose.enabled";

        final String bodyActivate = new JSONObject().put("value", true).toString();
        final String bodyDeactivate = new JSONObject().put("value", false).toString();

        // enable Multi-Assignments
        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", multiAssignmentKey)
                .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // try to enable Auto-Close
        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", autoCloseKey)
                .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        // try to disable Auto-Close
        mvc.perform(put(MgmtRestConstants.SYSTEM_V1_REQUEST_MAPPING + "/configs/{keyName}", autoCloseKey)
                .content(bodyDeactivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

}
