/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.event.remote.DeploymentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

/**
 * AbstractDsAssignmentStrategy for online assignments, i.e. managed by hawkBit.
 *
 */
public class OnlineDsAssignmentStrategy extends AbstractDsAssignmentStrategy {

    OnlineDsAssignmentStrategy(final TargetRepository targetRepository,
            final AfterTransactionCommitExecutor afterCommit, final ApplicationEventPublisher eventPublisher,
            final BusProperties bus, final ActionRepository actionRepository,
            final ActionStatusRepository actionStatusRepository, final QuotaManagement quotaManagement,
            final Supplier<Boolean> multiAssignmentsConfig) {
        super(targetRepository, afterCommit, eventPublisher, bus, actionRepository, actionStatusRepository,
                quotaManagement, multiAssignmentsConfig);
    }

    @Override
    void sendTargetUpdatedEvents(final DistributionSet set, final List<JpaTarget> targets) {
        targets.stream().forEach(target -> {
            target.setUpdateStatus(TargetUpdateStatus.PENDING);
            sendTargetUpdatedEvent(target);
        });
    }

    void sendDeploymentEvent(final Target target) {
        sendDeploymentEvent(target.getTenant(), Collections.singletonList(target.getControllerId()));
    }

    @Override
    void sendDeploymentEvents(final DistributionSetAssignmentResult assignmentResult) {
        if (isMultiAssignmentsEnabled()) {
            sendDeploymentEvents(Collections.singletonList(assignmentResult));
        } else {
            sendDistributionSetAssignedEvent(assignmentResult);
        }
    }

    @Override
    void sendDeploymentEvents(final List<DistributionSetAssignmentResult> assignmentResults) {
        if (isMultiAssignmentsEnabled()) {
            sendDeploymentEvent(assignmentResults.stream().flatMap(result -> result.getActions().stream())
                    .collect(Collectors.toList()));
        } else {
            assignmentResults.forEach(this::sendDistributionSetAssignedEvent);
        }
    }

    void sendDeploymentEvents(final long distributionSetId, final List<Action> actions,
            final boolean deviceCanProcessMultipleActions) {
        if (deviceCanProcessMultipleActions) {
            sendDeploymentEvent(actions);
        } else {
            final List<Action> filteredActions = actions.stream().filter(action -> {
                final Status actionStatus = action.getStatus();
                return Status.CANCELING != actionStatus && Status.CANCELED != actionStatus;
            }).collect(Collectors.toList());
            if (filteredActions.isEmpty()) {
                return;
            }
            sendTargetAssignDistributionSetEvent(filteredActions.get(0).getTenant(), distributionSetId,
                    filteredActions);
        }
    }

    private void sendDeploymentEvent(final List<Action> actions) {
        final List<Action> filteredActions = actions.stream().filter(action -> {
            final Status actionStatus = action.getStatus();
            return Status.CANCELING != actionStatus && Status.CANCELED != actionStatus;
        }).collect(Collectors.toList());
        if (filteredActions.isEmpty()) {
            return;
        }
        final String tenant = filteredActions.get(0).getTenant();
        sendDeploymentEvent(tenant, filteredActions.stream().map(action -> action.getTarget().getControllerId())
                .collect(Collectors.toList()));
    }

    @Override
    List<JpaTarget> findTargetsForAssignment(final List<String> controllerIDs, final long setId) {
        final Function<List<String>, List<JpaTarget>> mapper;
        if (isMultiAssignmentsEnabled()) {
            mapper = targetRepository::findAllByControllerId;
        } else {
            mapper = ids -> targetRepository
                    .findAll(TargetSpecifications.hasControllerIdAndAssignedDistributionSetIdNot(ids, setId));
        }
        return Lists.partition(controllerIDs, Constants.MAX_ENTRIES_IN_STATEMENT).stream().map(mapper)
                .flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    Set<Long> cancelActiveActions(final List<List<Long>> targetIds) {
        return targetIds.stream().map(this::overrideObsoleteUpdateActions).flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    void closeActiveActions(final List<List<Long>> targetIds) {
        targetIds.forEach(this::closeObsoleteUpdateActions);
    }

    @Override
    void setAssignedDistributionSetAndTargetStatus(final JpaDistributionSet set, final List<List<Long>> targetIds,
            final String currentUser) {
        targetIds.forEach(tIds -> targetRepository.setAssignedDistributionSetAndUpdateStatus(TargetUpdateStatus.PENDING,
                set, System.currentTimeMillis(), currentUser, tIds));

    }

    @Override
    JpaAction createTargetAction(final Map<String, TargetWithActionType> targetsWithActionMap, final JpaTarget target,
            final JpaDistributionSet set) {
        final JpaAction result = super.createTargetAction(targetsWithActionMap, target, set);
        if (result != null) {
            result.setStatus(Status.RUNNING);
        }
        return result;
    }

    @Override
    JpaActionStatus createActionStatus(final JpaAction action, final String actionMessage) {
        final JpaActionStatus result = super.createActionStatus(action, actionMessage);
        result.setStatus(Status.RUNNING);
        return result;
    }

    private DistributionSetAssignmentResult sendDistributionSetAssignedEvent(
            final DistributionSetAssignmentResult assignmentResult) {
        final List<Action> filteredActions = assignmentResult.getActions().stream().filter(action -> {
            final Status actionStatus = action.getStatus();
            return !hasPendingCancellations(action.getTarget()) && Status.CANCELING != actionStatus
                    && Status.CANCELED != actionStatus;
        }).collect(Collectors.toList());
        final DistributionSet set = assignmentResult.getDistributionSet();
        sendTargetAssignDistributionSetEvent(set.getTenant(), set.getId(), filteredActions);
        return assignmentResult;
    }

    private void sendTargetAssignDistributionSetEvent(final String tenant, final long distributionSetId,
            final List<Action> actions) {
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }

        afterCommit.afterCommit(() -> eventPublisher.publishEvent(new TargetAssignDistributionSetEvent(tenant,
                distributionSetId, actions, bus.getId(), actions.get(0).isMaintenanceWindowAvailable())));
    }

    private void sendDeploymentEvent(final String tenant, final List<String> controllerIds) {
        afterCommit.afterCommit(
                () -> eventPublisher.publishEvent(new DeploymentEvent(tenant, bus.getId(), controllerIds)));
    }

}
