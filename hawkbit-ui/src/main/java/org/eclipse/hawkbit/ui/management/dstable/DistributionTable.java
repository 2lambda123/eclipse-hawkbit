/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.confirmwindow.layout.ConfirmationTab;
import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.dd.criteria.ManagementViewClientCriterion;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.RefreshDistributionTableByFilterEvent;
import org.eclipse.hawkbit.ui.management.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupAbstractLayout.ActionTypeOption;
import org.eclipse.hawkbit.ui.management.miscs.ActionTypeOptionGroupAssignmentLayout;
import org.eclipse.hawkbit.ui.management.miscs.MaintenanceWindowLayout;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.push.DistributionSetUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.ui.view.filter.OnlyEventsFromDeploymentViewFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Distribution set table which is shown on the Deployment View.
 */
public class DistributionTable extends AbstractNamedVersionTable<DistributionSet> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DistributionTable.class);

    private final SpPermissionChecker permissionChecker;

    private final ManagementUIState managementUIState;

    private final ManagementViewClientCriterion managementViewClientCriterion;

    private final transient TargetManagement targetManagement;

    private final transient TargetTagManagement targetTagManagement;

    private final transient DistributionSetManagement distributionSetManagement;

    private final transient DeploymentManagement deploymentManagement;

    private final String notAllowedMsg;

    private boolean distPinned;

    private Button distributionPinnedBtn;

    private ConfirmationDialog confirmDialog;

    private final ActionTypeOptionGroupAssignmentLayout actionTypeOptionGroupLayout;

    private final MaintenanceWindowLayout maintenanceWindowLayout;

    private final UiProperties uiProperties;

    DistributionTable(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final SpPermissionChecker permissionChecker, final UINotification notification,
            final ManagementUIState managementUIState,
            final ManagementViewClientCriterion managementViewClientCriterion, final TargetManagement targetManagement,
            final DistributionSetManagement distributionSetManagement, final DeploymentManagement deploymentManagement,
            final TargetTagManagement targetTagManagement, final UiProperties uiProperties) {
        super(eventBus, i18n, notification, permissionChecker);
        this.permissionChecker = permissionChecker;
        this.managementUIState = managementUIState;
        this.managementViewClientCriterion = managementViewClientCriterion;
        this.targetManagement = targetManagement;
        this.targetTagManagement = targetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.deploymentManagement = deploymentManagement;
        this.actionTypeOptionGroupLayout = new ActionTypeOptionGroupAssignmentLayout(i18n);
        this.maintenanceWindowLayout = new MaintenanceWindowLayout(i18n);
        this.uiProperties = uiProperties;
        notAllowedMsg = i18n.getMessage(UIMessageIdProvider.MESSAGE_ACTION_NOT_ALLOWED);

        addNewContainerDS();
        setColumnProperties();
        setDataAvailable(getContainerDataSource().size() != 0);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onDistributionSetUpdateEvents(final DistributionSetUpdatedEventContainer eventContainer) {

        final List<Long> visibleItemIds = (List<Long>) getVisibleItemIds();

        if (allOfThemAffectCompletedSetsThatAreNotVisible(eventContainer.getEvents(), visibleItemIds)) {
            refreshContainer();
        } else if (!checkAndHandleIfVisibleDsSwitchesFromCompleteToIncomplete(eventContainer.getEvents(),
                visibleItemIds)) {
            updateVisableTableEntries(eventContainer.getEvents(), visibleItemIds);
        }
        final Long lastSelectedDsIdName = managementUIState.getLastSelectedDsIdName();
        eventContainer.getEvents().stream().filter(event -> event.getEntityId().equals(lastSelectedDsIdName))
                .filter(Objects::nonNull).findAny().ifPresent(event -> getEventBus().publish(this,
                        new DistributionTableEvent(BaseEntityEventType.SELECTED_ENTITY, event.getEntity())));
    }

    private static boolean allOfThemAffectCompletedSetsThatAreNotVisible(final List<DistributionSetUpdatedEvent> events,
            final List<Long> visibleItemIds) {
        return events.stream().allMatch(event -> !visibleItemIds.contains(event.getEntityId()) && event.isComplete());
    }

    private void updateVisableTableEntries(final List<DistributionSetUpdatedEvent> events,
            final List<Long> visibleItemIds) {
        events.stream().filter(event -> visibleItemIds.contains(event.getEntityId()))
                .filter(DistributionSetUpdatedEvent::isComplete).filter(Objects::nonNull)
                .forEach(event -> updateDistributionInTable(event.getEntity()));
    }

    private boolean checkAndHandleIfVisibleDsSwitchesFromCompleteToIncomplete(
            final List<DistributionSetUpdatedEvent> events, final List<Long> visibleItemIds) {
        final List<Long> setsThatAreVisibleButNotCompleteAnymore = events.stream()
                .filter(event -> visibleItemIds.contains(event.getEntityId())).filter(event -> !event.isComplete())
                .map(DistributionSetUpdatedEvent::getEntityId).collect(Collectors.toList());

        if (!setsThatAreVisibleButNotCompleteAnymore.isEmpty()) {
            refreshContainer();
            if (setsThatAreVisibleButNotCompleteAnymore.stream()
                    .anyMatch(id -> id.equals(managementUIState.getLastSelectedDsIdName()))) {
                managementUIState.setLastSelectedEntityId(null);
            }
            return true;
        }

        return false;
    }

    /**
     * DistributionTableFilterEvent.
     *
     * @param filterEvent
     *            as instance of {@link RefreshDistributionTableByFilterEvent}
     */
    @EventBusListenerMethod(scope = EventScope.UI, filter = OnlyEventsFromDeploymentViewFilter.class)
    void onEvent(final RefreshDistributionTableByFilterEvent filterEvent) {
        UI.getCurrent().access(this::refreshFilter);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionTableEvent event) {
        onBaseEntityEvent(event);
        if (BaseEntityEventType.UPDATED_ENTITY != event.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> updateDistributionInTable(event.getEntity()));
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final PinUnpinEvent pinUnpinEvent) {
        UI.getCurrent().access(() -> {
            if (pinUnpinEvent == PinUnpinEvent.PIN_TARGET) {
                refreshFilter();
                styleDistributionTableOnPinning();
                // unstyleDistPin
                if (distributionPinnedBtn != null) {
                    distributionPinnedBtn.setStyleName(getPinStyle());
                }
            } else if (pinUnpinEvent == PinUnpinEvent.UNPIN_TARGET) {
                refreshFilter();
                restoreDistributionTableStyle();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent managementUIEvent) {
        UI.getCurrent().access(() -> {
            if (managementUIEvent == ManagementUIEvent.UNASSIGN_DISTRIBUTION_TAG
                    || managementUIEvent == ManagementUIEvent.ASSIGN_DISTRIBUTION_TAG) {
                refreshFilter();
            }
        });
    }

    @Override
    protected String getTableId() {
        return UIComponentIdProvider.DIST_TABLE_ID;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<DistributionBeanQuery> distributionQF = new BeanQueryFactory<>(
                DistributionBeanQuery.class);
        distributionQF.setQueryConfiguration(queryConfiguration);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), distributionQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(4);
        managementUIState.getDistributionTableFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));
        managementUIState.getDistributionTableFilters().getPinnedTarget()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.ORDER_BY_PINNED_TARGET, value));

        final List<String> list = new ArrayList<>();
        queryConfig.put(SPUIDefinitions.FILTER_BY_NO_TAG,
                managementUIState.getDistributionTableFilters().isNoTagSelected());
        if (!managementUIState.getDistributionTableFilters().getDistSetTags().isEmpty()) {
            list.addAll(managementUIState.getDistributionTableFilters().getDistSetTags());
        }
        queryConfig.put(SPUIDefinitions.FILTER_BY_TAG, list);
        return queryConfig;
    }

    @Override
    protected void addContainerProperties(final Container container) {
        HawkbitCommonUtil.getDsTableColumnProperties(container);
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.PIN_COLUMN, (source, itemId, columnId) -> getPinButton(itemId));
    }

    @Override
    protected Object getItemIdToSelect() {
        return managementUIState.getSelectedDsIdName().isEmpty() ? null : managementUIState.getSelectedDsIdName();
    }

    @Override
    protected Optional<DistributionSet> findEntityByTableValue(final Long lastSelectedId) {
        return distributionSetManagement.getWithDetails(lastSelectedId);
    }

    @Override
    protected void publishSelectedEntityEvent(final DistributionSet selectedLastEntity) {
        getEventBus().publish(this,
                new DistributionTableEvent(BaseEntityEventType.SELECTED_ENTITY, selectedLastEntity));
    }

    @Override
    protected ManagementUIState getManagementEntityState() {
        return managementUIState;
    }

    @Override
    protected boolean isMaximized() {
        return managementUIState.isDsTableMaximized();
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        if (isMaximized()) {
            return columnList;
        }
        columnList.add(new TableColumn(SPUILabelDefinitions.PIN_COLUMN, "", 0.0F));
        return columnList;
    }

    @Override
    protected float getColumnNameMinimizedSize() {
        return 0.7F;
    }

    @Override
    public AcceptCriterion getDropAcceptCriterion() {
        return managementViewClientCriterion;
    }

    @Override
    protected void onDropEventFromTable(final DragAndDropEvent event) {
        assignTargetToDs(event);
    }

    @Override
    protected void onDropEventFromWrapper(final DragAndDropEvent event) {
        if (event.getTransferable().getSourceComponent().getId()
                .startsWith(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS)) {
            assignDsTag(event);
        } else {
            assignTargetTag(event);
        }
    }

    private void assignDsTag(final DragAndDropEvent event) {
        final com.vaadin.event.dd.TargetDetails taregtDet = event.getTargetDetails();
        final Table distTable = (Table) taregtDet.getTarget();
        final Set<Long> distsSelected = getTableValue(distTable);
        final Set<Long> distList = new HashSet<>();

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object distItemId = dropData.getItemIdOver();

        if (!distsSelected.contains(distItemId)) {
            distList.add((Long) distItemId);
        } else {
            distList.addAll(distsSelected);
        }

        final String distTagName = HawkbitCommonUtil.removePrefix(event.getTransferable().getSourceComponent().getId(),
                SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS);

        final DistributionSetTagAssignmentResult result = distributionSetManagement.toggleTagAssignment(distList,
                distTagName);

        getNotification().displaySuccess(HawkbitCommonUtil.createAssignmentMessage(distTagName, result, getI18n()));
        if (result.getAssigned() >= 1 && managementUIState.getDistributionTableFilters().isNoTagSelected()) {
            refreshFilter();
        }
    }

    private void assignTargetTag(final DragAndDropEvent event) {
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object distItemId = dropData.getItemIdOver();
        final String targetTagName = HawkbitCommonUtil.removePrefix(
                event.getTransferable().getSourceComponent().getId(), SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
        // get all the targets assigned to the tag
        // assign dist to those targets

        targetTagManagement.getByName(targetTagName).ifPresent(tag -> {
            Pageable query = new PageRequest(0, 500);
            Page<Target> assignedTargets;
            boolean assigned = false;
            do {
                assignedTargets = targetManagement.findByTag(query, tag.getId());
                if (assignedTargets.hasContent()) {
                    assignTargetToDs(getItem(distItemId), assignedTargets.getContent());
                    assigned = true;
                }
            } while (assignedTargets.hasNext() && (query = assignedTargets.nextPageable()) != null);
            if (assigned) {
                getNotification()
                        .displaySuccess(getI18n().getMessage("message.no.targets.assiged.fortag", targetTagName));
            }
        });
    }

    private void assignTargetToDs(final DragAndDropEvent event) {
        final TableTransferable transferable = (TableTransferable) event.getTransferable();
        final TargetTable targetTable = (TargetTable) transferable.getSourceComponent();
        final Set<Long> targetIdSet = targetTable.getSelectedEntitiesByTransferable(transferable);
        selectDraggedEntities(targetTable, targetIdSet);
        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Object distItemId = dropData.getItemIdOver();
        assignTargetToDs(getItem(distItemId), targetManagement.get(targetIdSet));
    }

    private void assignTargetToDs(final Item item, final List<Target> targetDetailsList) {
        if (item == null || item.getItemProperty("id") == null) {
            return;
        }

        if (targetDetailsList.isEmpty()) {
            getNotification().displayWarning(getI18n().getMessage(TARGETS_NOT_EXISTS));
            return;
        }

        final Long distId = (Long) item.getItemProperty("id").getValue();
        selectDroppedEntities(distId);
        final Optional<DistributionSet> findDistributionSetById = distributionSetManagement.get(distId);
        if (!findDistributionSetById.isPresent()) {
            getNotification().displayWarning(getI18n().getMessage(DISTRIBUTIONSET_NOT_EXISTS));
            return;
        }

        addNewDistributionToAssignmentList(targetDetailsList, findDistributionSetById.get());
        openConfirmationWindowForAssignment(findDistributionSetById.get().getName(), targetDetailsList);
    }

    private void addNewDistributionToAssignmentList(final List<Target> targetDetailsList,
            final DistributionSet distributionSet) {
        String pendingActionMessage = null;
        final DistributionSetIdName distributionSetIdName = new DistributionSetIdName(distributionSet);

        for (final Target target : targetDetailsList) {
            final TargetIdName key = new TargetIdName(target);
            if (managementUIState.getAssignedList().keySet().contains(key)
                    && managementUIState.getAssignedList().get(key).equals(distributionSetIdName)) {
                pendingActionMessage = getPendingActionMessage(pendingActionMessage, target.getControllerId(),
                        HawkbitCommonUtil.getDistributionNameAndVersion(distributionSetIdName.getName(),
                                distributionSetIdName.getVersion()));
                getNotification().displayValidationError(pendingActionMessage);
            } else {
                managementUIState.getAssignedList().put(key, distributionSetIdName);
            }
        }
    }

    private void openConfirmationWindowForAssignment(final String distributionNameToAssign,
            final List<Target> targetDetailsList) {
        final String confirmQuestion = createConfirmationQuestionForAssignment(distributionNameToAssign,
                targetDetailsList);
        createConfirmationWindowForAssignment(confirmQuestion);
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void createConfirmationWindowForAssignment(final String confirmQuestion) {
        confirmDialog = new ConfirmationDialog(getI18n().getMessage(CAPTION_ENTITY_ASSIGN_ACTION_CONFIRMBOX),
                confirmQuestion, getI18n().getMessage(UIMessageIdProvider.BUTTON_OK),
                getI18n().getMessage(UIMessageIdProvider.BUTTON_CANCEL), ok -> {
                    if (ok && isMaintenanceWindowValid()) {
                        saveAllAssignments();
                    } else {
                        managementUIState.getAssignedList().clear();
                    }
                }, createAssignmentTab(), UIComponentIdProvider.DIST_SET_TO_TARGET_ASSIGNMENT_CONFIRM_ID);
    }

    private String createConfirmationQuestionForAssignment(final String distributionNameToAssign,
            final List<Target> targetDetailsList) {
        if (targetDetailsList.size() == 1) {
            return getI18n().getMessage(MESSAGE_CONFIRM_ASSIGN_ENTITY, distributionNameToAssign, "target",
                    targetDetailsList.get(0).getName());
        } else {
            return getI18n().getMessage(MESSAGE_CONFIRM_ASSIGN_MULTIPLE_ENTITIES, targetDetailsList.size(), "targets",
                    distributionNameToAssign);
        }
    }

    private boolean isMaintenanceWindowValid() {
        if (maintenanceWindowLayout.isEnabled()) {
            try {
                MaintenanceScheduleHelper.validateMaintenanceSchedule(maintenanceWindowLayout.getMaintenanceSchedule(),
                        maintenanceWindowLayout.getMaintenanceDuration(),
                        maintenanceWindowLayout.getMaintenanceTimeZone());
            } catch (final InvalidMaintenanceScheduleException e) {
                LOG.error("Maintenance window is not valid", e);
                getNotification().displayValidationError(e.getMessage());
                return false;
            }
        }
        return true;
    }

    private void saveAllAssignments() {
        final Set<TargetIdName> itemIds = managementUIState.getAssignedList().keySet();
        Long distId;
        List<TargetIdName> targetIdSetList;
        List<TargetIdName> tempIdList;
        final ActionType actionType = ((ActionTypeOption) actionTypeOptionGroupLayout.getActionTypeOptionGroup()
                .getValue()).getActionType();
        final long forcedTimeStamp = (((ActionTypeOption) actionTypeOptionGroupLayout.getActionTypeOptionGroup()
                .getValue()) == ActionTypeOption.AUTO_FORCED)
                        ? actionTypeOptionGroupLayout.getForcedTimeDateField().getValue().getTime()
                        : RepositoryModelConstants.NO_FORCE_TIME;

        final Map<Long, List<TargetIdName>> saveAssignedList = Maps.newHashMapWithExpectedSize(itemIds.size());

        for (final TargetIdName itemId : itemIds) {
            final DistributionSetIdName distitem = managementUIState.getAssignedList().get(itemId);
            distId = distitem.getId();

            if (saveAssignedList.containsKey(distId)) {
                targetIdSetList = saveAssignedList.get(distId);
            } else {
                targetIdSetList = new ArrayList<>();
            }
            targetIdSetList.add(itemId);
            saveAssignedList.put(distId, targetIdSetList);
        }

        final String maintenanceSchedule = maintenanceWindowLayout.getMaintenanceSchedule();
        final String maintenanceDuration = maintenanceWindowLayout.getMaintenanceDuration();
        final String maintenanceTimeZone = maintenanceWindowLayout.getMaintenanceTimeZone();

        for (final Map.Entry<Long, List<TargetIdName>> mapEntry : saveAssignedList.entrySet()) {
            tempIdList = saveAssignedList.get(mapEntry.getKey());
            final DistributionSetAssignmentResult distributionSetAssignmentResult = deploymentManagement
                    .assignDistributionSet(mapEntry.getKey(),
                            tempIdList.stream().map(t -> maintenanceWindowLayout.isEnabled()
                                    ? new TargetWithActionType(t.getControllerId(), actionType, forcedTimeStamp,
                                            maintenanceSchedule, maintenanceDuration, maintenanceTimeZone)
                                    : new TargetWithActionType(t.getControllerId(), actionType, forcedTimeStamp))
                                    .collect(Collectors.toList()));

            if (distributionSetAssignmentResult.getAssigned() > 0) {
                getNotification().displaySuccess(getI18n().getMessage("message.target.assignment",
                        distributionSetAssignmentResult.getAssigned()));
            }
            if (distributionSetAssignmentResult.getAlreadyAssigned() > 0) {
                getNotification().displaySuccess(getI18n().getMessage("message.target.alreadyAssigned",
                        distributionSetAssignmentResult.getAlreadyAssigned()));
            }
        }
        resfreshPinnedDetails(saveAssignedList);

        managementUIState.getAssignedList().clear();
        getNotification().displaySuccess(getI18n().getMessage("message.target.ds.assign.success"));
        getEventBus().publish(this, SaveActionWindowEvent.SAVED_ASSIGNMENTS);
    }

    private void resfreshPinnedDetails(final Map<Long, List<TargetIdName>> saveAssignedList) {
        final Optional<Long> pinnedDist = managementUIState.getTargetTableFilters().getPinnedDistId();
        final Optional<TargetIdName> pinnedTarget = managementUIState.getDistributionTableFilters().getPinnedTarget();

        if (pinnedDist.isPresent()) {
            if (saveAssignedList.keySet().contains(pinnedDist.get())) {
                getEventBus().publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
            }
        } else if (pinnedTarget.isPresent()) {
            final Set<TargetIdName> assignedTargetIds = managementUIState.getAssignedList().keySet();
            if (assignedTargetIds.contains(pinnedTarget.get())) {
                getEventBus().publish(this, PinUnpinEvent.PIN_TARGET);
            }
        }
    }

    private ConfirmationTab createAssignmentTab() {
        final ConfirmationTab assignmentTab = new ConfirmationTab();
        actionTypeOptionGroupLayout.selectDefaultOption();
        assignmentTab.addComponent(actionTypeOptionGroupLayout);
        assignmentTab.addComponent(enableMaintenanceWindowLayout());
        initMaintenanceWindow();
        assignmentTab.addComponent(maintenanceWindowLayout);
        return assignmentTab;
    }

    private HorizontalLayout enableMaintenanceWindowLayout() {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.addComponent(enableMaintenanceWindowControl());
        layout.addComponent(maintenanceWindowHelpLinkControl());
        return layout;
    }

    private CheckBox enableMaintenanceWindowControl() {
        final CheckBox enableMaintenanceWindow = new CheckBox(
                getI18n().getMessage("caption.maintenancewindow.enabled"));
        enableMaintenanceWindow.setId(UIComponentIdProvider.MAINTENANCE_WINDOW_ENABLED_ID);
        enableMaintenanceWindow.addStyleName(ValoTheme.CHECKBOX_SMALL);
        enableMaintenanceWindow.addStyleName("dist-window-maintenance-window-enable");
        enableMaintenanceWindow.addValueChangeListener(event -> {
            final Boolean isMaintenanceWindowEnabled = enableMaintenanceWindow.getValue();
            maintenanceWindowLayout.setVisible(isMaintenanceWindowEnabled);
            maintenanceWindowLayout.setEnabled(isMaintenanceWindowEnabled);
            enableSaveButton(!isMaintenanceWindowEnabled);
            maintenanceWindowLayout.clearAllControls();
        });
        return enableMaintenanceWindow;
    }

    private Link maintenanceWindowHelpLinkControl() {
        final String maintenanceWindowHelpUrl = uiProperties.getLinks().getDocumentation().getMaintenanceWindowView();
        return SPUIComponentProvider.getHelpLink(getI18n(), maintenanceWindowHelpUrl);
    }

    private void initMaintenanceWindow() {
        maintenanceWindowLayout.setVisible(false);
        maintenanceWindowLayout.setEnabled(false);
        maintenanceWindowLayout.getScheduleControl()
                .addTextChangeListener(event -> enableSaveButton(maintenanceWindowLayout.onScheduleChange(event)));
        maintenanceWindowLayout.getDurationControl()
                .addTextChangeListener(event -> enableSaveButton(maintenanceWindowLayout.onDurationChange(event)));
    }

    private void enableSaveButton(final boolean enabled) {
        confirmDialog.getOkButton().setEnabled(enabled);
    }

    @Override
    protected List<String> hasMissingPermissionsForDrop() {
        return permissionChecker.hasUpdateTargetPermission() ? Collections.emptyList()
                : Arrays.asList(SpPermission.UPDATE_TARGET);
    }

    @Override
    protected String getDropTableId() {
        return UIComponentIdProvider.TARGET_TABLE_ID;
    }

    @Override
    protected boolean validateDragAndDropWrapper(final DragAndDropWrapper wrapperSource) {
        final String tagData = wrapperSource.getData().toString();
        if (wrapperSource.getId().startsWith(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS)) {
            return !isNoTagButton(tagData, getI18n().getMessage(UIMessageIdProvider.CAPTION_DISTRIBUTION_TAG));
        } else if (wrapperSource.getId().startsWith(SPUIDefinitions.TARGET_TAG_ID_PREFIXS)) {
            return !isNoTagButton(tagData, getI18n().getMessage(UIMessageIdProvider.CAPTION_TARGET_TAG));
        }
        getNotification().displayValidationError(notAllowedMsg);
        return false;
    }

    private boolean isNoTagButton(final String tagData, final String targetNoTagData) {
        if (tagData.equals(targetNoTagData)) {
            getNotification().displayValidationError(getI18n().getMessage("message.tag.cannot.be.assigned",
                    getI18n().getMessage("label.no.tag.assigned")));
            return true;
        }
        return false;
    }

    private String getPendingActionMessage(final String message, final String controllerId,
            final String distNameVersion) {
        String pendActionMsg = getI18n().getMessage("message.target.assigned.pending");
        if (null == message) {
            pendActionMsg = getI18n().getMessage("message.dist.pending.action", controllerId, distNameVersion);
        }
        return pendActionMsg;
    }

    private void updateDistributionInTable(final DistributionSet editedDs) {
        final Item item = getContainerDataSource().getItem(editedDs.getId());
        if (item == null) {
            return;
        }
        updateEntity(editedDs, item);
    }

    private void restoreDistributionTableStyle() {
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return null;
            }
        });
    }

    private void styleDistributionTableOnPinning() {
        managementUIState.getDistributionTableFilters().getPinnedTarget().map(TargetIdName::getControllerId)
                .ifPresent(controllerId -> {
                    final Long installedDistId = deploymentManagement.getInstalledDistributionSet(controllerId)
                            .map(DistributionSet::getId).orElse(null);
                    final Long assignedDistId = deploymentManagement.getAssignedDistributionSet(controllerId)
                            .map(DistributionSet::getId).orElse(null);
                    styleDistributionSetTable(installedDistId, assignedDistId);
                });
    }

    private static String getPinnedDistributionStyle(final Long installedDistItemIds,
            final Long assignedDistTableItemIds, final Object itemId) {
        final Long distId = (Long) itemId;

        if (distId == null) {
            return null;
        }

        if (distId.equals(installedDistItemIds)) {
            return SPUIDefinitions.HIGHLIGHT_GREEN;
        }
        if (distId.equals(assignedDistTableItemIds)) {
            return SPUIDefinitions.HIGHLIGHT_ORANGE;
        }

        return null;
    }

    private Button getPinButton(final Object itemId) {
        final Button pinBtn = createPinBtn(itemId);
        saveDistributionPinnedBtn(pinBtn);
        pinBtn.addClickListener(this::addPinClickListener);
        rePinDistribution(pinBtn, (Long) itemId);
        return pinBtn;
    }

    private void saveDistributionPinnedBtn(final Button pinBtn) {
        if (pinBtn.getData() == null) {
            return;
        }

        final Long pinnedId = ((DistributionSetIdName) pinBtn.getData()).getId();

        if (managementUIState.getTargetTableFilters().getPinnedDistId().map(pinnedId::equals).orElse(false)) {
            setDistributionPinnedBtn(pinBtn);
        }
    }

    private void addPinClickListener(final ClickEvent event) {
        checkifAlreadyPinned(event.getButton());
        if (distPinned) {
            pinDitribution(event.getButton());
        } else {
            unPinDistribution(event.getButton());
        }
    }

    private void checkifAlreadyPinned(final Button eventBtn) {
        final Long newPinnedDistItemId = ((DistributionSetIdName) eventBtn.getData()).getId();
        final Long pinnedDistId = managementUIState.getTargetTableFilters().getPinnedDistId().orElse(null);

        if (pinnedDistId == null) {
            distPinned = !distPinned;
            managementUIState.getTargetTableFilters().setPinnedDistId(newPinnedDistItemId);
        } else if (newPinnedDistItemId.equals(pinnedDistId)) {
            distPinned = false;
        } else {
            distPinned = true;
            managementUIState.getTargetTableFilters().setPinnedDistId(newPinnedDistItemId);
            distributionPinnedBtn.setStyleName(getPinStyle());
        }
        distributionPinnedBtn = eventBtn;
    }

    private void unPinDistribution(final Button eventBtn) {
        managementUIState.getTargetTableFilters().setPinnedDistId(null);
        getEventBus().publish(this, PinUnpinEvent.UNPIN_DISTRIBUTION);
        resetPinStyle(eventBtn);
    }

    private static void resetPinStyle(final Button pinBtn) {
        pinBtn.setStyleName(getPinStyle());
    }

    private void pinDitribution(final Button eventBtn) {
        managementUIState.getDistributionTableFilters().setPinnedTarget(null);
        getEventBus().publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
        applyPinStyle(eventBtn);
        styleDistributionSetTable();
        distPinned = false;
    }

    private void rePinDistribution(final Button pinBtn, final Long distID) {
        if (managementUIState.getTargetTableFilters().getPinnedDistId().map(distID::equals).orElse(false)) {
            applyPinStyle(pinBtn);
            distPinned = true;
            distributionPinnedBtn = pinBtn;
            getEventBus().publish(this, PinUnpinEvent.PIN_DISTRIBUTION);
        }
    }

    private void styleDistributionSetTable() {
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return null;
            }
        });
    }

    private static void applyPinStyle(final Button eventBtn) {
        final StringBuilder style = new StringBuilder(SPUIComponentProvider.getPinButtonStyle());
        style.append(' ').append(SPUIStyleDefinitions.DIST_PIN).append(' ').append("tablePin").append(' ')
                .append("pin-icon-red");
        eventBtn.setStyleName(style.toString());
    }

    private static String getPinButtonId(final String distName, final String distVersion) {
        final StringBuilder pinBtnId = new StringBuilder(UIComponentIdProvider.DIST_PIN_BUTTON);
        pinBtnId.append('.');
        pinBtnId.append(distName);
        pinBtnId.append('.');
        pinBtnId.append(distVersion);
        return pinBtnId.toString();
    }

    private Button createPinBtn(final Object itemId) {

        final Item item = getContainerDataSource().getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();

        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        final DistributionSetIdName distributionSetIdName = new DistributionSetIdName((Long) itemId, name, version);

        final Button pinBtn = new Button();
        pinBtn.setIcon(FontAwesome.THUMB_TACK);
        pinBtn.setHeightUndefined();
        pinBtn.addStyleName(getPinStyle());
        pinBtn.setData(distributionSetIdName);
        pinBtn.setId(getPinButtonId(name, version));
        pinBtn.setImmediate(true);
        pinBtn.setDescription(getI18n().getMessage(UIMessageIdProvider.TOOLTIP_DISTRIBUTION_SET_PIN));
        return pinBtn;
    }

    private static String getPinStyle() {
        final StringBuilder pinBtnStyle = new StringBuilder(SPUIComponentProvider.getPinButtonStyle());
        pinBtnStyle.append(' ');
        pinBtnStyle.append(SPUIStyleDefinitions.DIST_PIN);
        pinBtnStyle.append(' ');
        pinBtnStyle.append(SPUIStyleDefinitions.DIST_PIN_BLUE);
        return pinBtnStyle.toString();
    }

    /**
     * Added by Saumya Target pin listener.
     *
     * @param installedDistItemId
     *            Item ids of installed distribution set
     * @param assignedDistTableItemId
     *            Item ids of assigned distribution set
     */
    public void styleDistributionSetTable(final Long installedDistItemId, final Long assignedDistTableItemId) {
        setCellStyleGenerator((source, itemId, propertyId) -> getPinnedDistributionStyle(installedDistItemId,
                assignedDistTableItemId, itemId));
    }

    public void setDistributionPinnedBtn(final Button distributionPinnedBtn) {
        this.distributionPinnedBtn = distributionPinnedBtn;
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        managementUIState.setNoDataAvailableDistribution(!available);

    }

    @Override
    protected void handleOkDelete(final List<Long> entitiesToDelete) {
        distributionSetManagement.delete(entitiesToDelete);
        getEventBus().publish(this, new DistributionTableEvent(BaseEntityEventType.REMOVE_ENTITY, entitiesToDelete));
        getNotification().displaySuccess(getI18n().getMessage("message.delete.success",
                entitiesToDelete.size() + " " + getI18n().getMessage("distribution.details.header") + "(s)"));
        managementUIState.getTargetTableFilters().getPinnedDistId()
                .ifPresent(distId -> unPinDeletedDS(entitiesToDelete, distId));
        managementUIState.getSelectedDsIdName().clear();
    }

    private void unPinDeletedDS(final Collection<Long> deletedDsIds, final Long pinnedDsId) {
        if (deletedDsIds.contains(pinnedDsId)) {
            managementUIState.getTargetTableFilters().setPinnedDistId(null);
            getEventBus().publish(this, PinUnpinEvent.UNPIN_DISTRIBUTION);
        }
    }

    @Override
    protected String getEntityType() {
        return getI18n().getMessage("distribution.details.header");
    }

    @Override
    protected Set<Long> getSelectedEntities() {
        return managementUIState.getSelectedDsIdName();
    }

    @Override
    protected String getEntityId(final Object itemId) {
        final String entityId = String.valueOf(
                getContainerDataSource().getItem(itemId).getItemProperty(SPUILabelDefinitions.DIST_ID).getValue());
        return "distributionSet." + entityId;
    }

    @Override
    protected String getDeletedEntityName(final Long entityId) {
        final Optional<DistributionSet> distribution = distributionSetManagement.get(entityId);
        if (distribution.isPresent()) {
            return distribution.get().getName();
        }
        return "";
    }

}
