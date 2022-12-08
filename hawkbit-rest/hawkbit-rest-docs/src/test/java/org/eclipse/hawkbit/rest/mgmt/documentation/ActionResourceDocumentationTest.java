/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.mgmt.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.rest.documentation.AbstractApiRestDocumentation;
import org.eclipse.hawkbit.rest.documentation.ApiModelPropertiesGeneric;
import org.eclipse.hawkbit.rest.documentation.MgmtApiModelProperties;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.restdocs.payload.JsonFieldType;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Documentation generation for Management API for {@link Action}.
 */
@Feature("Spring Rest Docs Tests - Action")
@Story("Action Resource")
public class ActionResourceDocumentationTest extends AbstractApiRestDocumentation {

    private final String targetId = "137";

    @Override
    public String getResourceName() {
        return "actions";
    }

    @Test
    @Description("Handles the GET request of retrieving all actions. Required Permission: READ_TARGET.")
    public void getActions() throws Exception {
        enableMultiAssignments();
        generateRolloutActionForTarget(targetId);

        mockMvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(responseFields(
                        fieldWithPath("size").type(JsonFieldType.NUMBER).description(ApiModelPropertiesGeneric.SIZE),
                        fieldWithPath("total").description(ApiModelPropertiesGeneric.TOTAL_ELEMENTS),
                        fieldWithPath("content[]").description(MgmtApiModelProperties.ACTION_LIST),
                        fieldWithPath("content[].createdBy").description(ApiModelPropertiesGeneric.CREATED_BY),
                        fieldWithPath("content[].createdAt").description(ApiModelPropertiesGeneric.CREATED_AT),
                        fieldWithPath("content[].lastModifiedBy")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_BY).type("String"),
                        fieldWithPath("content[].lastModifiedAt")
                                .description(ApiModelPropertiesGeneric.LAST_MODIFIED_AT).type("String"),
                        fieldWithPath("content[].type").description(MgmtApiModelProperties.ACTION_TYPE)
                                .attributes(key("value").value("['update', 'cancel']")),

                        fieldWithPath("content[].status").description(MgmtApiModelProperties.ACTION_EXECUTION_STATUS)
                                .attributes(key("value").value("['finished', 'pending']")),
                        fieldWithPath("content[]._links").description(MgmtApiModelProperties.LINK_TO_ACTION),
                        fieldWithPath("content[].id").description(MgmtApiModelProperties.ACTION_ID),
                        fieldWithPath("content[].weight").description(MgmtApiModelProperties.ACTION_WEIGHT),
                        fieldWithPath("content[].rollout").description(MgmtApiModelProperties.ACTION_ROLLOUT),
                        fieldWithPath("content[].rolloutName")
                                .description(MgmtApiModelProperties.ACTION_ROLLOUT_NAME))));
    }

    @Test
    @Description("Handles the GET request of retrieving all actions based on parameters. Required Permission: READ_TARGET.")
    public void getActionsFromTargetWithParameters() throws Exception {
        generateActionForTarget(targetId);

        mockMvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING
                + "?limit=10&sort=id:ASC&offset=0&q=target.name==" + targetId)).andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andDo(this.document.document(requestParameters(
                        parameterWithName("limit").attributes(key("type").value("query"))
                                .description(ApiModelPropertiesGeneric.LIMIT),
                        parameterWithName("sort").description(ApiModelPropertiesGeneric.SORT),
                        parameterWithName("offset").description(ApiModelPropertiesGeneric.OFFSET),
                        parameterWithName("q").description(ApiModelPropertiesGeneric.FIQL))));
    }

    private Action generateActionForTarget(final String knownControllerId) throws Exception {
        return generateActionForTarget(knownControllerId, true, false, null, null, null);
    }

    private Action generateRolloutActionForTarget(final String knownControllerId) throws Exception {
        return generateActionForTarget(knownControllerId, true, false, null, null, null, true);
    }

    private Action generateActionForTarget(final String knownControllerId, final boolean inSync,
            final boolean timeforced, final String maintenanceWindowSchedule, final String maintenanceWindowDuration,
            final String maintenanceWindowTimeZone) throws Exception {
        return generateActionForTarget(knownControllerId, inSync, timeforced, maintenanceWindowSchedule,
                maintenanceWindowDuration, maintenanceWindowTimeZone, false);
    }

    private Action generateActionForTarget(final String knownControllerId, final boolean inSync,
            final boolean timeforced, final String maintenanceWindowSchedule, final String maintenanceWindowDuration,
            final String maintenanceWindowTimeZone, final boolean createRollout) throws Exception {
        final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, ActionStatusFields.ID.getFieldName());

        createTargetByGivenNameWithAttributes(knownControllerId, inSync, timeforced, createDistributionSet(),
                maintenanceWindowSchedule, maintenanceWindowDuration, maintenanceWindowTimeZone, createRollout);

        final List<Action> actions = deploymentManagement.findActionsAll(pageRequest).getContent();

        assertThat(actions).hasSize(1);
        return actions.get(0);
    }

}
