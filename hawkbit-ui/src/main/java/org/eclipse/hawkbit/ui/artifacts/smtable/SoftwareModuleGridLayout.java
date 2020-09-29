/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.artifacts.upload.FileUploadProgress;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsHeader;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.FilterChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.GenericEventListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectGridEntityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedSelectionAwareSupport;
import org.eclipse.hawkbit.ui.common.softwaremodule.AbstractSoftwareModuleGridLayout;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;

/**
 * Software module table layout. (Upload Management)
 */
public class SoftwareModuleGridLayout extends AbstractSoftwareModuleGridLayout {
    private static final long serialVersionUID = 1L;

    private final SoftwareModuleGridHeader softwareModuleGridHeader;
    private final SoftwareModuleGrid softwareModuleGrid;
    private final SoftwareModuleDetailsHeader softwareModuleDetailsHeader;
    private final SoftwareModuleDetails softwareModuleDetails;

    /**
     * Constructor for SoftwareModuleGridLayout
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param softwareModuleManagement
     *            SoftwareModuleManagement
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param smTypeFilterLayoutUiState
     *            TypeFilterLayoutUiState
     * @param smGridLayoutUiState
     *            GridLayoutUiState
     */
    public SoftwareModuleGridLayout(final UIConfiguration uiConfig,
            final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final TypeFilterLayoutUiState smTypeFilterLayoutUiState, final GridLayoutUiState smGridLayoutUiState) {
        super(uiConfig, softwareModuleManagement, softwareModuleTypeManagement, EventView.UPLOAD);

        this.softwareModuleGridHeader = new SoftwareModuleGridHeader(uiConfig, smTypeFilterLayoutUiState,
                smGridLayoutUiState, getSmWindowBuilder(), getEventView());
        this.softwareModuleGridHeader.buildHeader();
        this.softwareModuleGrid = new SoftwareModuleGrid(uiConfig, smTypeFilterLayoutUiState, smGridLayoutUiState,
                softwareModuleManagement, getEventView());
        this.softwareModuleGrid.init();

        this.softwareModuleDetailsHeader = new SoftwareModuleDetailsHeader(uiConfig, getSmWindowBuilder(),
                getSmMetaDataWindowBuilder());
        this.softwareModuleDetailsHeader.buildHeader();
        this.softwareModuleDetails = new SoftwareModuleDetails(uiConfig, softwareModuleManagement,
                softwareModuleTypeManagement, getSmMetaDataWindowBuilder());
        this.softwareModuleDetails.buildDetails();

        addEventListener(new FilterChangedListener<>(uiConfig.getEventBus(), ProxySoftwareModule.class,
                new EventViewAware(getEventView()), softwareModuleGrid.getFilterSupport()));
        addEventListener(new SelectionChangedListener<>(uiConfig.getEventBus(),
                new EventLayoutViewAware(EventLayout.SM_LIST, getEventView()), getMasterSmAwareComponents()));
        addEventListener(new SelectGridEntityListener<>(uiConfig.getEventBus(),
                new EventLayoutViewAware(EventLayout.SM_LIST, getEventView()),
                softwareModuleGrid.getSelectionSupport()));
        addEventListener(new EntityModifiedListener.Builder<>(uiConfig.getEventBus(), ProxySoftwareModule.class)
                .entityModifiedAwareSupports(getSmModifiedAwareSupports()).build());
        addEventListener(new GenericEventListener<>(uiConfig.getEventBus(), EventTopics.FILE_UPLOAD_CHANGED,
                this::onUploadChanged));

        buildLayout(softwareModuleGridHeader, softwareModuleGrid, softwareModuleDetailsHeader, softwareModuleDetails);
    }

    private List<MasterEntityAwareComponent<ProxySoftwareModule>> getMasterSmAwareComponents() {
        return Arrays.asList(softwareModuleDetailsHeader, softwareModuleDetails);
    }

    private List<EntityModifiedAwareSupport> getSmModifiedAwareSupports() {
        return Arrays.asList(EntityModifiedGridRefreshAwareSupport.of(softwareModuleGrid::refreshAll),
                EntityModifiedSelectionAwareSupport.of(softwareModuleGrid.getSelectionSupport(),
                        softwareModuleGrid::mapIdToProxyEntity));
    }

    /**
     * Verifies when file upload is in progress
     *
     * @param fileUploadProgress
     *            FileUploadProgress
     */
    public void onUploadChanged(final FileUploadProgress fileUploadProgress) {
        softwareModuleGrid.onUploadChanged(fileUploadProgress);
    }

    @Override
    protected SoftwareModuleGridHeader getSoftwareModuleGridHeader() {
        return softwareModuleGridHeader;
    }

    @Override
    protected SoftwareModuleGrid getSoftwareModuleGrid() {
        return softwareModuleGrid;
    }

}
