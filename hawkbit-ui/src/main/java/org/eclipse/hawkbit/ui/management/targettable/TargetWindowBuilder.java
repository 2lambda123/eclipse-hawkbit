/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Window;

/**
 * Builder for target window
 */
public class TargetWindowBuilder extends AbstractEntityWindowBuilder<ProxyTarget> {

    private final TargetManagement targetManagement;

    private final EventView view;

    /**
     * Constructor for TargetWindowBuilder
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param targetManagement
     *            TargetManagement
     * @param view
     *            EventView
     */
    public TargetWindowBuilder(final UIConfiguration uiConfig, final TargetManagement targetManagement,
            final EventView view) {
        super(uiConfig);

        this.targetManagement = targetManagement;

        this.view = view;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.CREATE_POPUP_ID;
    }

    @Override
    public Window getWindowForAdd() {
        return getWindowForNewEntity(
                new AddTargetWindowController(uiConfig, targetManagement, new TargetWindowLayout(i18n), view));

    }

    @Override
    public Window getWindowForUpdate(final ProxyTarget proxyTarget) {
        return getWindowForEntity(proxyTarget,
                new UpdateTargetWindowController(uiConfig, targetManagement, new TargetWindowLayout(i18n)));
    }
}
