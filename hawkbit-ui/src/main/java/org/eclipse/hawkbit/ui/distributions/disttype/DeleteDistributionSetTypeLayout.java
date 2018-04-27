/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Layout for the pop-up window which is created when deleting a Distribution
 * Set Type on the Distributions View.
 *
 */
public class DeleteDistributionSetTypeLayout extends UpdateDistributionSetTypeLayout {

    private static final long serialVersionUID = 1L;

    private final List<DistributionSetType> selectedTypes;

    public DeleteDistributionSetTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetManagement distributionSetManagement, final List<DistributionSetType> selectedTypes) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, softwareModuleTypeManagement,
                distributionSetTypeManagement, distributionSetManagement);
        this.selectedTypes = selectedTypes;
    }

    @Override
    public void init() {
        super.init();
        getUpdateCombobox().getComboLabel()
                .setValue(getI18n().getMessage("label.choose.type", getI18n().getMessage("label.choose.tag.delete")));
    }

    @Override
    protected void buildLayout() {
        super.buildLayout();
        getTagDesc().setEnabled(false);
        getTwinTables().setEnabled(false);
        getTagName().setEnabled(false);
        getContentLayout().removeComponent(getColorLabelLayout());
        getUpdateCombobox().getComboLabel()
                .setValue(getI18n().getMessage("label.choose.type", getI18n().getMessage("label.choose.tag.delete")));
    }

    @Override
    protected boolean isUpdateAction() {
        return false;
    }

    @Override
    protected boolean isDuplicate() {
        return false;
    }

    @Override
    protected String getWindowCaption() {
        return getI18n().getMessage("caption.configure", getI18n().getMessage("caption.delete"),
                getI18n().getMessage("caption.type"));
    }

    @Override
    protected void saveEntity() {
        if (canBeDeleted()) {
            deleteDistributionType();
        }
    }

    @Override
    protected boolean isDeleteAction() {
        return true;
    }

    private boolean canBeDeleted() {
        if (!getPermChecker().hasDeleteRepositoryPermission()) {
            getUiNotification().displayValidationError(
                    getI18n().getMessage("message.permission.insufficient", SpPermission.DELETE_REPOSITORY));
            return false;
        }
        return true;
    }

    private void deleteDistributionType() {
        final String tagNameToDelete = getTagName().getValue();
        final Optional<DistributionSetType> distTypeToDelete = getDistributionSetTypeManagement()
                .getByName(tagNameToDelete);
        distTypeToDelete.ifPresent(tag -> {
            if (selectedTypes.contains(distTypeToDelete.get())) {
                getUiNotification().displayValidationError(getI18n().getMessage("message.tag.delete", tagNameToDelete));
            } else {
                getDistributionSetTypeManagement().delete(distTypeToDelete.get().getId());
                getEventBus().publish(this, SaveActionWindowEvent.SAVED_DELETE_DIST_SET_TYPES);
                getUiNotification().displaySuccess(getI18n().getMessage("message.dist.set.type.deleted.success"));
                selectedTypes.remove(distTypeToDelete.get());
            }
        });
    }

}
