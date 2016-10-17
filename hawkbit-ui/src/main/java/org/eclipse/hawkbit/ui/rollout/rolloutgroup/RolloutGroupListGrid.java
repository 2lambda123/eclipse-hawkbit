/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.RolloutRenderer;
<<<<<<< HEAD
import org.eclipse.hawkbit.ui.push.events.RolloutGroupChangeEventContainer;
=======
import org.eclipse.hawkbit.ui.push.RolloutGroupChangeEventContainer;
>>>>>>> refs/heads/master
import org.eclipse.hawkbit.ui.rollout.DistributionBarHelper;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickListener;
import com.vaadin.ui.renderers.HtmlRenderer;

/**
 *
 * Rollout group list grid component.
 *
 */
@SpringComponent
@ViewScope
public class RolloutGroupListGrid extends AbstractGrid {
    private static final long serialVersionUID = 4060904914954370524L;

    private static final String ROLLOUT_RENDERER_DATA = "rolloutRendererData";

    @Autowired
    private transient RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    private transient RolloutManagement rolloutManagement;

    @Autowired
    private transient RolloutUIState rolloutUIState;

    @Autowired
    private transient SpPermissionChecker permissionChecker;

    private transient Map<RolloutGroupStatus, StatusFontIcon> statusIconMap = new EnumMap<>(RolloutGroupStatus.class);

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final RolloutEvent event) {
        if (RolloutEvent.SHOW_ROLLOUT_GROUPS != event) {
            return;
        }
        ((LazyQueryContainer) getContainerDataSource()).refresh();
    }

    /**
     *
     * Handles the RolloutGroupChangeEvent to refresh the item in the grid.
     *
     *
     * @param rolloutGroupChangeEvent
     *            the event which contains the rollout group which has been
     *            change
     */
    @SuppressWarnings("unchecked")
    @EventBusListenerMethod(scope = EventScope.SESSION)
    public void onRolloutGroupChangeEvent(final RolloutGroupChangeEventContainer eventContainer) {
        if (!rolloutUIState.isShowRolloutGroups()) {
            return;
        }

        ((LazyQueryContainer) getContainerDataSource()).refresh();
    }

    private String calculateFinishedPercentage(final RolloutGroup rolloutGroup) {
        return HawkbitCommonUtil.formattingFinishedPercentage(rolloutGroup,
                rolloutManagement.getFinishedPercentForRunningGroup(rolloutGroup.getRollout().getId(), rolloutGroup));
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutGroupBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutGroupBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGroupGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);

        rolloutGroupGridContainer.addContainerProperty(ROLLOUT_RENDERER_DATA, RolloutRendererData.class, null, false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutGroupStatus.class, null,
                false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE,
                String.class, null, false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD, String.class,
                null, false, false);

        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD, String.class, null,
                false, false);

        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
                false);

        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null,
                false, false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
                false);
        rolloutGroupGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS,
                TotalTargetCountStatus.class, null, false, false);

    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(ROLLOUT_RENDERER_DATA).setMinimumWidth(40);
        getColumn(ROLLOUT_RENDERER_DATA).setMaximumWidth(200);

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMaximumWidth(100);

        getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(75);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setMaximumWidth(75);

        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE).setMaximumWidth(100);

        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD).setMaximumWidth(100);

        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD).setMinimumWidth(40);
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD).setMaximumWidth(100);

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(280);

        setFrozenColumnCount(7);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(ROLLOUT_RENDERER_DATA).setHeaderCaption(i18n.get("header.name"));
        getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.get("header.status"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS)
                .setHeaderCaption(i18n.get("header.detail.status"));
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE)
                .setHeaderCaption(i18n.get("header.rolloutgroup.installed.percentage"));
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD)
                .setHeaderCaption(i18n.get("header.rolloutgroup.threshold.error"));
        getColumn(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD)
                .setHeaderCaption(i18n.get("header.rolloutgroup.threshold"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setHeaderCaption(i18n.get("header.createdBy"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
        getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
        getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.get("header.description"));
        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setHeaderCaption(i18n.get("header.total.targets"));
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ROLLOUT_GROUP_LIST_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        final List<Object> columnList = new ArrayList<>();
        columnList.add(ROLLOUT_RENDERER_DATA);
        columnList.add(SPUILabelDefinitions.VAR_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS);
        columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS);
        columnList.add(SPUILabelDefinitions.ROLLOUT_GROUP_INSTALLED_PERCENTAGE);
        columnList.add(SPUILabelDefinitions.ROLLOUT_GROUP_ERROR_THRESHOLD);
        columnList.add(SPUILabelDefinitions.ROLLOUT_GROUP_THRESHOLD);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnList.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnList.add(SPUILabelDefinitions.VAR_DESC);
        setColumnOrder(columnList.toArray());
        alignColumns();
    }

    @Override
    protected void addColumnRenderes() {
        createRolloutGroupStatusToFontMap();
        getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlLabelRenderer(),
                new RolloutGroupStatusConverter());

        getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(new HtmlRenderer(),
                new TotalTargetCountStatusConverter());
        if (permissionChecker.hasRolloutTargetsReadPermission()) {
            getColumn(ROLLOUT_RENDERER_DATA).setRenderer(new RolloutRenderer(new RolloutGroupClickListener()));
        }
    }

    @Override
    protected void setHiddenColumns() {
        final List<Object> columnsToBeHidden = new ArrayList<>();
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_NAME);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_DATE);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_USER);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
        columnsToBeHidden.add(SPUILabelDefinitions.VAR_DESC);
        for (final Object propertyId : columnsToBeHidden) {
            getColumn(propertyId).setHidden(true);
        }
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return this::getDescription;
    }

    private void createRolloutGroupStatusToFontMap() {
        statusIconMap.put(RolloutGroupStatus.FINISHED,
                new StatusFontIcon(FontAwesome.CHECK_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_GREEN));
        statusIconMap.put(RolloutGroupStatus.SCHEDULED,
                new StatusFontIcon(FontAwesome.HOURGLASS_1, SPUIStyleDefinitions.STATUS_ICON_PENDING));
        statusIconMap.put(RolloutGroupStatus.RUNNING,
                new StatusFontIcon(FontAwesome.ADJUST, SPUIStyleDefinitions.STATUS_ICON_YELLOW));
        statusIconMap.put(RolloutGroupStatus.READY,
                new StatusFontIcon(FontAwesome.DOT_CIRCLE_O, SPUIStyleDefinitions.STATUS_ICON_LIGHT_BLUE));
        statusIconMap.put(RolloutGroupStatus.ERROR,
                new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
    }

    private String getDescription(final CellReference cell) {
        if (SPUILabelDefinitions.VAR_STATUS.equals(cell.getPropertyId())) {
            return cell.getProperty().getValue().toString().toLowerCase();
        } else if (SPUILabelDefinitions.ACTION.equals(cell.getPropertyId())) {
            return SPUILabelDefinitions.ACTION.toLowerCase();
        } else if (ROLLOUT_RENDERER_DATA.equals(cell.getPropertyId())) {
            return ((RolloutRendererData) cell.getProperty().getValue()).getName();
        } else if (SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS.equals(cell.getPropertyId())) {
            return DistributionBarHelper
                    .getTooltip(((TotalTargetCountStatus) cell.getValue()).getStatusTotalCountMap());
        }
        return null;
    }

    private void alignColumns() {
        setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 5573570647129792429L;

            @Override
            public String getStyle(final CellReference cellReference) {
                final String[] coulmnNames = { SPUILabelDefinitions.VAR_STATUS,
                        SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS };
                if (Arrays.asList(coulmnNames).contains(cellReference.getPropertyId())) {
                    return "centeralign";
                }
                return null;
            }
        });
    }

    private class RolloutGroupClickListener implements RendererClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void click(final RendererClickEvent event) {
            rolloutUIState.setRolloutGroup(
                    rolloutGroupManagement.findRolloutGroupWithDetailedStatus((Long) event.getItemId()));
            eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS);
        }
    }

    /**
     *
     * Converts {@link TotalTargetCountStatus} into formatted string with status
     * and count details.
     */
    class TotalTargetCountStatusConverter implements Converter<String, TotalTargetCountStatus> {

        private static final long serialVersionUID = -9205943894818450807L;

        @Override
        public TotalTargetCountStatus convertToModel(final String value,
                final Class<? extends TotalTargetCountStatus> targetType, final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final TotalTargetCountStatus value,
                final Class<? extends String> targetType, final Locale locale) {
            return DistributionBarHelper.getDistributionBarAsHTMLString(value.getStatusTotalCountMap());
        }

        @Override
        public Class<TotalTargetCountStatus> getModelType() {
            return TotalTargetCountStatus.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

    /**
     * Converts {@link RolloutGroupStatus} to string.
     */
    class RolloutGroupStatusConverter implements Converter<String, RolloutGroupStatus> {

        private static final long serialVersionUID = 5448062736373292820L;

        @Override
        public RolloutGroupStatus convertToModel(final String value,
                final Class<? extends RolloutGroupStatus> targetType, final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final RolloutGroupStatus value, final Class<? extends String> targetType,
                final Locale locale) {
            return convertRolloutGroupStatusToString(value);
        }

        @Override
        public Class<RolloutGroupStatus> getModelType() {
            return RolloutGroupStatus.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }

        private String convertRolloutGroupStatusToString(final RolloutGroupStatus value) {
            final StatusFontIcon statusFontIcon = statusIconMap.get(value);
            final String codePoint = HawkbitCommonUtil.getCodePoint(statusFontIcon);
            return HawkbitCommonUtil.getStatusLabelDetailsInString(codePoint, statusFontIcon.getStyle(),
                    UIComponentIdProvider.ROLLOUT_GROUP_STATUS_LABEL_ID);

        }

    }
}
