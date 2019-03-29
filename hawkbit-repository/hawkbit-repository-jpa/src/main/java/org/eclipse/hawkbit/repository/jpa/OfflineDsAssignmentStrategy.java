/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResultMap;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;

import com.google.common.collect.Lists;

/**
 * AbstractDsAssignmentStrategy for offline assignments, i.e. not managed by
 * hawkBit.
 *
 */
public class OfflineDsAssignmentStrategy extends AbstractDsAssignmentStrategy {

    OfflineDsAssignmentStrategy(final TargetRepository targetRepository,
            final AfterTransactionCommitExecutor afterCommit, final ApplicationEventPublisher eventPublisher,
            final BusProperties bus, final ActionRepository actionRepository,
            final ActionStatusRepository actionStatusRepository, final QuotaManagement quotaManagement) {
        super(targetRepository, afterCommit, eventPublisher, bus, actionRepository, actionStatusRepository,
                quotaManagement);
    }

    @Override
    void sendTargetUpdatedEvents(final DistributionSet set, final List<JpaTarget> targets) {
        targets.forEach(target -> {
            target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
            sendTargetUpdatedEvent(target);
        });
    }

    @Override
    DistributionSetAssignmentResult sendDistributionSetAssignedEvent(
            final DistributionSetAssignmentResult assignmentResult) {
        // no need to send an event in the offline case
        return assignmentResult;
    }

    @Override
    DistributionSetAssignmentResultMap sendDistributionSetsAssignedEvent(
            final DistributionSetAssignmentResultMap assignmentResults) {
        // no need to send an event in the offline case
        return assignmentResults;
    }

    @Override
    public List<JpaTarget> findTargetsForAssignment(final List<String> controllerIDs, final long setId) {
        return Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream()
                .map(ids -> targetRepository.findAll(SpecificationsBuilder.combineWithAnd(
                        Arrays.asList(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, setId),
                                TargetSpecifications.notEqualToTargetUpdateStatus(TargetUpdateStatus.PENDING)))))
                .flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    public Set<Long> cancelActiveActions(final List<List<Long>> targetIds) {
        return Collections.emptySet();
    }

    @Override
    void closeActiveActions(final List<List<Long>> targetIds) {
        // Not supported by offline case
    }

    @Override
    void setAssignedDistributionSetAndTargetStatus(final JpaDistributionSet set, final List<List<Long>> targetIds,
            final String currentUser) {
        targetIds.forEach(tIds -> targetRepository.setAssignedAndInstalledDistributionSetAndUpdateStatus(
                TargetUpdateStatus.IN_SYNC, set, System.currentTimeMillis(), currentUser, tIds));
    }

    @Override
    protected JpaAction createTargetAction(final Map<String, TargetWithActionType> targetsWithActionMap,
            final JpaTarget target, final JpaDistributionSet set) {
        final JpaAction result = super.createTargetAction(targetsWithActionMap, target, set);
        if (result != null) {
            result.setStatus(Status.FINISHED);
            result.setActive(Boolean.FALSE);
        }
        return result;
    }

    @Override
    protected JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus result = super.createActionStatus(action, actionMessage);
        result.setStatus(Status.FINISHED);
        result.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Action reported as offline deployment");
        return result;
    }

}
