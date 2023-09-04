/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetType;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetType;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.targettype.MgmtTargetTypeRequestBodyPut;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for TargetType CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Target Types", description = "REST API for Target Type CRUD operations.")
public interface MgmtTargetTypeRestApi {

    /**
     * Handles the GET request of retrieving all TargetTypes.
     *
     * @param pagingOffsetParam
     *            the offset of list of target types for pagination, might not
     *            be present in the rest request then default value will be
     *            applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     *
     * @return a list of all TargetTypes for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all target types", description = "Handles the GET request of retrieving all target types.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTargetType>> getTargetTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the GET request of retrieving a single TargetType.
     *
     * @param targetTypeId
     *            the ID of the target type to retrieve
     *
     * @return a single target type with status OK.
     */
    @Operation(summary = "Return target type by id", description = "Handles the GET request of retrieving a single target type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Target type not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}", produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetType> getTargetType(@PathVariable("targetTypeId") Long targetTypeId);

    /**
     * Handles the DELETE request for a single Target Type.
     *
     * @param targetTypeId
     *            the ID of the target type to retrieve
     * @return status OK if delete is successful.
     *
     */
    @Operation(summary = "Delete target type by id", description = "Handles the DELETE request for a single target type. Required Permission: DELETE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Target type not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @DeleteMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}")
    ResponseEntity<Void> deleteTargetType(@PathVariable("targetTypeId") Long targetTypeId);

    /**
     * Handles the PUT request of updating a Target Type.
     *
     * @param targetTypeId
     *            the ID of the target type in the URL
     * @param restTargetType
     *            the target type to be updated.
     * @return status OK if update is successful
     */
    @Operation(summary = "Update target type by id", description = "Handles the PUT request for a single target type. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Target type not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PutMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}", consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetType> updateTargetType(@PathVariable("targetTypeId") Long targetTypeId,
            MgmtTargetTypeRequestBodyPut restTargetType);

    /**
     * Handles the POST request of creating new Target Types. The request body
     * must always be a list of types.
     *
     * @param targetTypes
     *            the target types to be created.
     * @return In case all target types could be successfully created the
     *         ResponseEntity with status code 201 - Created but without
     *         ResponseBody. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @Operation(summary = "Create target types", description = "Handles the POST request for creating new target types. The request body must always be a list of types. Required Permission: CREATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Target type not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTargetType>> createTargetTypes(List<MgmtTargetTypeRequestBodyPost> targetTypes);

    /**
     * Handles the GET request of retrieving the list of compatible distribution
     * set types in that target type.
     *
     * @param targetTypeId
     *            of the TargetType.
     * @return Unpaged list of distribution set types and OK in case of success.
     */
    @Operation(summary = "Return list of compatible distribution set types", description = "Handles the GET request of retrieving the list of compatible distribution set types in that target type. Required Permission: READ_TARGET, READ_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Distribution set type was not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @GetMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}/"
            + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtDistributionSetType>> getCompatibleDistributionSets(
            @PathVariable("targetTypeId") Long targetTypeId);

    /**
     * Handles DELETE request for removing the compatibility of a distribution
     * set type from the target type.
     *
     * @param targetTypeId
     *            of the TargetType.
     * @param distributionSetTypeId
     *            of the DistributionSetType.
     *
     * @return OK if the request was successful
     */
    @Operation(summary = "Remove compatibility of distribution set type from the target type", description = "Handles the DELETE request for removing a distribution set type from a single target type. Required Permission: UPDATE_TARGET and READ_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Distribution set type was not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @DeleteMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}/"
            + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES + "/{distributionSetTypeId}")
    ResponseEntity<Void> removeCompatibleDistributionSet(@PathVariable("targetTypeId") Long targetTypeId,
            @PathVariable("distributionSetTypeId") Long distributionSetTypeId);

    /**
     * Handles the POST request for adding the compatibility of a distribution
     * set type to a target type.
     *
     * @param targetTypeId
     *            of the TargetType.
     * @param distributionSetTypeIds
     *            of the DistributionSetTypes as a List.
     *
     * @return OK if the request was successful
     */
    @Operation(summary = "Adding compatibility of a distribution set type to a target type", description = "Handles the POST request for adding compatible distribution set types to a target type. Required Permission: UPDATE_TARGET and READ_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters"),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Distribution set type was not found."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
    @PostMapping(value = MgmtRestConstants.TARGETTYPE_V1_REQUEST_MAPPING + "/{targetTypeId}/"
            + MgmtRestConstants.TARGETTYPE_V1_DS_TYPES, consumes = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> addCompatibleDistributionSets(@PathVariable("targetTypeId") final Long targetTypeId,
            final List<MgmtDistributionSetTypeAssignment> distributionSetTypeIds);
}
