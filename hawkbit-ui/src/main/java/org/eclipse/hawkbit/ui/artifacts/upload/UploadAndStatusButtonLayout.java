/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.upload;

import javax.servlet.MultipartConfigElement;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadStatusEvent.UploadStatusEventType;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

/**
 * Upload files layout.
 */
public class UploadAndStatusButtonLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UploadAndStatusButtonLayout.class);

    private final UploadStatusInfoWindow uploadInfoWindow;

    private final VaadinMessageSource i18n;

    private final transient MultipartConfigElement multipartConfigElement;

    private final UI ui;

    private HorizontalLayout fileUploadButtonLayout;

    private Button uploadStatusButton;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final UploadLogic uploadLogic;

    private final UploadMessageBuilder uploadMessageBuilder;

    private final ArtifactManagement artifactManagement;
    private Upload upload;

    public UploadAndStatusButtonLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final ArtifactUploadState artifactUploadState, final MultipartConfigElement multipartConfigElement,
            final ArtifactManagement artifactManagement, final SoftwareModuleManagement softwareManagement,
            final UploadLogic uploadLogic) {
        this.artifactManagement = artifactManagement;
        this.uploadLogic = uploadLogic;
        this.uploadInfoWindow = new UploadStatusInfoWindow(eventBus, artifactUploadState, i18n, artifactManagement,
                softwareManagement, uploadLogic);
        this.uploadInfoWindow.addCloseListener(event -> {
            if (uploadLogic.isUploadComplete()) {
                hideUploadStatusButton();
            }
        });
        this.i18n = i18n;
        this.multipartConfigElement = multipartConfigElement;
        this.softwareModuleManagement = softwareManagement;
        this.uploadMessageBuilder = new UploadMessageBuilder(uploadLogic, i18n);

        createComponents();
        buildLayout();
        restoreState();
        eventBus.subscribe(this);
        ui = UI.getCurrent();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadStatusEvent event) {
        final UploadStatusEventType uploadProgressEventType = event.getUploadStatusEventType();
        switch (uploadProgressEventType) {
        case UPLOAD_STARTED:
            ui.access(this::onStartOfUpload);
            break;

        case UPLOAD_FAILED:
        case UPLOAD_SUCCESSFUL:
        case UPLOAD_FINISHED:
        case UPLOAD_ABORTED_BY_USER:
            ui.access(this::onUploadFinished);
            break;
        case UPLOAD_IN_PROGRESS:
            break;
        default:
            throw new IllegalArgumentException("Enum " + UploadStatusEventType.class.getSimpleName()
                    + " doesn't contain value " + uploadProgressEventType);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        final BaseEntityEventType eventType = event.getEventType();
        if (eventType == BaseEntityEventType.SELECTED_ENTITY) {
            ui.access(() -> {
                if (uploadLogic.isNoSoftwareModuleSelected() || uploadLogic.isMoreThanOneSoftwareModulesSelected()) {
                    upload.setEnabled(false);
                } else {
                    upload.setEnabled(true);
                }
            });
        }
    }

    private void createComponents() {
        uploadStatusButton = SPUIComponentProvider.getButton(UIComponentIdProvider.UPLOAD_STATUS_BUTTON, "", "", "",
                false, null, SPUIButtonStyleSmall.class);
        uploadStatusButton.setStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        uploadStatusButton.addStyleName(SPUIStyleDefinitions.UPLOAD_PROGRESS_INDICATOR_STYLE);
        uploadStatusButton.setIcon(null);
        uploadStatusButton.setWidth("100px");
        uploadStatusButton.setHtmlContentAllowed(true);
        uploadStatusButton.addClickListener(event -> onClickOfUploadStatusButton());
        uploadStatusButton.setVisible(false);
    }

    private void buildLayout() {

        upload = new Upload();
        final FileTransferHandlerVaadinUpload uploadHandler = new FileTransferHandlerVaadinUpload(uploadLogic,
                multipartConfigElement.getMaxFileSize(), upload, softwareModuleManagement, artifactManagement,
                uploadMessageBuilder);
        upload.setButtonCaption(i18n.getMessage("upload.file"));
        upload.setImmediate(true);
        upload.setReceiver(uploadHandler);
        upload.addSucceededListener(uploadHandler);
        upload.addFailedListener(uploadHandler);
        upload.addFinishedListener(uploadHandler);
        upload.addProgressListener(uploadHandler);
        upload.addStartedListener(uploadHandler);
        upload.addStyleName(SPUIStyleDefinitions.ACTION_BUTTON);
        upload.addStyleName("no-border");

        fileUploadButtonLayout = new HorizontalLayout();
        fileUploadButtonLayout.setSpacing(true);
        fileUploadButtonLayout.addStyleName(SPUIStyleDefinitions.FOOTER_LAYOUT);
        fileUploadButtonLayout.addComponent(upload);
        fileUploadButtonLayout.setComponentAlignment(upload, Alignment.MIDDLE_LEFT);
        fileUploadButtonLayout.addComponent(uploadStatusButton);
        fileUploadButtonLayout.setComponentAlignment(uploadStatusButton, Alignment.MIDDLE_RIGHT);
        setMargin(false);

        setSizeFull();
        setSpacing(true);
    }

    private void restoreState() {
        if (uploadLogic.isUploadComplete()) {
            uploadLogic.clearUploadDetails();

            hideUploadStatusButton();
        } else if (uploadLogic.isUploadRunning()) {
            showUploadStatusButton();
        }
    }

    private void onStartOfUpload() {
        showUploadStatusButton();
    }

    /**
     * Called for every finished (succeeded or failed) upload.
     */
    private void onUploadFinished() {
        if (uploadLogic.isUploadComplete()) {
            hideUploadStatusButton();
        }
    }

    public HorizontalLayout getFileUploadButtonLayout() {
        return fileUploadButtonLayout;
    }

    private void onClickOfUploadStatusButton() {
        uploadInfoWindow.maximizeWindow();
    }

    private void showUploadStatusButton() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.setVisible(true);
    }

    private void hideUploadStatusButton() {
        if (uploadStatusButton == null) {
            return;
        }
        uploadStatusButton.setVisible(false);
    }
}
