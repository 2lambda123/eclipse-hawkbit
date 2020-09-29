/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for abstract entity window
 *
 * @param <T>
 *            Generic type entity
 * @param <E>
 *            Generic type entity
 */
public abstract class AbstractEntityWindowController<T, E> {

    protected final UIConfiguration uiConfig;
    protected final VaadinMessageSource i18n;
    protected final EntityFactory entityFactory;
    protected final UIEventBus eventBus;
    protected final UINotification uiNotification;

    /**
     * Constructor.
     *
     * @param uiConfig
     *            the {@link UIConfiguration}
     */
    protected AbstractEntityWindowController(final UIConfiguration uiConfig) {
        this.uiConfig = uiConfig;
        this.entityFactory = uiConfig.getEntityFactory();
        this.eventBus = uiConfig.getEventBus();
        this.i18n = uiConfig.getI18n();
        this.uiNotification = uiConfig.getUiNotification();
    }

    /**
     * Populate entity with data
     *
     * @param proxyEntity
     *            Generic type entity
     */
    public void populateWithData(final T proxyEntity) {
        getLayout().setEntity(buildEntityFromProxy(proxyEntity));

        adaptLayout(proxyEntity);
    }

    /**
     * @return layout
     */
    public abstract EntityWindowLayout<E> getLayout();

    protected abstract E buildEntityFromProxy(final T proxyEntity);

    protected void adaptLayout(final T proxyEntity) {
        // can be overriden to adapt layout components (e.g. disable/enable
        // fields, adapt bindings, etc.)
    }

    /**
     * @return Save dialog close listener
     */
    public SaveDialogCloseListener getSaveDialogCloseListener() {
        return new SaveDialogCloseListener() {
            @Override
            public void saveOrUpdate() {
                persistEntity(getLayout().getEntity());
            }

            @Override
            public boolean canWindowSaveOrUpdate() {
                return isEntityValid(getLayout().getEntity());
            }

            @Override
            public boolean canWindowClose() {
                return closeWindowAfterSave();
            }
        };
    }

    protected abstract void persistEntity(final E entity);

    protected abstract boolean isEntityValid(final E entity);

    protected boolean closeWindowAfterSave() {
        return true;
    }
}
