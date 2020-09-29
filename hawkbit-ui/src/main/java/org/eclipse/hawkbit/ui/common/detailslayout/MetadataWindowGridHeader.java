/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.grid.header.AbstractGridHeader;
import org.eclipse.hawkbit.ui.common.grid.header.support.AddHeaderSupport;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.ui.Component;

/**
 * Target table header layout.
 */
public class MetadataWindowGridHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 1L;

    private final transient AddHeaderSupport addHeaderSupport;

    /**
     * Constructor for MetadataWindowGridHeader
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param addNewItemCallback
     *            Runnable
     */
    public MetadataWindowGridHeader(final UIConfiguration uiConfig, final Runnable addNewItemCallback) {
        super(uiConfig.getI18n(), uiConfig.getPermChecker(), uiConfig.getEventBus());

        if (permChecker.hasCreateRepositoryPermission()) {
            this.addHeaderSupport = new AddHeaderSupport(i18n, UIComponentIdProvider.METADTA_ADD_ICON_ID,
                    addNewItemCallback, () -> false);
        } else {
            this.addHeaderSupport = null;
        }

        addHeaderSupport(addHeaderSupport);

        buildHeader();
    }

    @Override
    protected void init() {
        super.init();

        setWidth("100%");
        setHeight("30px");
        addStyleName("metadata-table-margin");
    }

    @Override
    protected Component getHeaderCaption() {
        return SPUIComponentProvider.generateCaptionLabel(i18n, "caption.metadata");
    }
}
