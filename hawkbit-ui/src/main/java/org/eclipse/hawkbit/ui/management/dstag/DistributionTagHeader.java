/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterHeader;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

/**
 *
 *
 */
public class DistributionTagHeader extends AbstractFilterHeader {

    private static final long serialVersionUID = 1L;

    private final ManagementUIState managementUIState;

    private final transient EntityFactory entityFactory;

    private final UINotification uiNotification;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

    DistributionTagHeader(final VaadinMessageSource i18n, final ManagementUIState managementUIState,
            final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final DistributionSetTagManagement distributionSetTagManagement, final EntityFactory entityFactory,
            final UINotification uiNotification) {
        super(permChecker, eventBus, i18n);
        this.entityFactory = entityFactory;
        this.managementUIState = managementUIState;
        this.uiNotification = uiNotification;
        this.distributionSetTagManagement = distributionSetTagManagement;
    }

    @Override
    protected String getHideButtonId() {
        return "hide.distribution.tags";
    }

    @Override
    protected String getTitle() {
        return i18n.getMessage("header.filter.tag", new Object[] {});
    }

    @Override
    protected boolean dropHitsRequired() {
        return true;
    }

    @Override
    protected void hideFilterButtonLayout() {
        managementUIState.setDistTagFilterClosed(true);
        eventBus.publish(this, ManagementUIEvent.HIDE_DISTRIBUTION_TAG_LAYOUT);
    }

    @Override
    protected String getConfigureFilterButtonId() {
        return UIComponentIdProvider.ADD_DISTRIBUTION_TAG;
    }

    @Override
    protected boolean isAddTagRequired() {
        return true;
    }

    @Override
    protected Command getAddButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new CreateDistributionSetTagLayout(i18n, distributionSetTagManagement, entityFactory, eventBus,
                        permChecker, uiNotification);
            }
        };
    }

    @Override
    protected Command getDeleteButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new DeleteDistributionSetTagLayout(i18n, distributionSetTagManagement, entityFactory, eventBus,
                        permChecker, uiNotification, managementUIState.getDistributionTableFilters().getDistSetTags());
            }
        };
    }

    @Override
    protected Command getUpdateButtonCommand() {
        return new MenuBar.Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(final MenuItem selectedItem) {
                new UpdateDistributionSetTagLayout(i18n, distributionSetTagManagement, entityFactory, eventBus,
                        permChecker, uiNotification);
            }
        };
    }

}
