/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Rollout window dependencies holder.
 */
public final class RolloutWindowDependencies {

    private final RolloutManagement rolloutManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;
    private final TargetManagement targetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final UiProperties uiProperties;
    private final UIConfiguration uiConfig;

    /**
     * Constructor for RolloutWindowDependencies
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param rolloutManagement
     *            RolloutManagement
     * @param targetManagement
     *            TargetManagement
     * @param uiProperties
     *            UiProperties
     * @param targetFilterQueryManagement
     *            TargetFilterQueryManagement
     * @param rolloutGroupManagement
     *            RolloutGroupManagement
     * @param quotaManagement
     *            QuotaManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     */
    public RolloutWindowDependencies(final UIConfiguration uiConfig, final RolloutManagement rolloutManagement,
            final TargetManagement targetManagement, final UiProperties uiProperties,
            final TargetFilterQueryManagement targetFilterQueryManagement,
            final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement,
            final DistributionSetManagement distributionSetManagement) {
        this.uiConfig = uiConfig;
        this.rolloutManagement = rolloutManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.quotaManagement = quotaManagement;
        this.targetManagement = targetManagement;
        this.uiProperties = uiProperties;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    /**
     * @return Rollout management
     */
    public RolloutManagement getRolloutManagement() {
        return rolloutManagement;
    }

    /**
     * @return Rollout group management
     */
    public RolloutGroupManagement getRolloutGroupManagement() {
        return rolloutGroupManagement;
    }

    /**
     * @return Quota management
     */
    public QuotaManagement getQuotaManagement() {
        return quotaManagement;
    }

    /**
     * @return Target management
     */
    public TargetManagement getTargetManagement() {
        return targetManagement;
    }

    /**
     * @return Target filter query management
     */
    public TargetFilterQueryManagement getTargetFilterQueryManagement() {
        return targetFilterQueryManagement;
    }

    /**
     * @return UI notification
     */
    public UINotification getUiNotification() {
        return uiConfig.getUiNotification();
    }

    /**
     * @return Entity factory
     */
    public EntityFactory getEntityFactory() {
        return uiConfig.getEntityFactory();
    }

    /**
     * @return Vaadin message source
     */
    public VaadinMessageSource getI18n() {
        return uiConfig.getI18n();
    }

    /**
     * @return UI properties
     */
    public UiProperties getUiProperties() {
        return uiProperties;
    }

    /**
     * @return UI event bus
     */
    public UIEventBus getEventBus() {
        return uiConfig.getEventBus();
    }

    /**
     * @return Distribution set management
     */
    public DistributionSetManagement getDistributionSetManagement() {
        return distributionSetManagement;
    }

    /**
     * @return the uiConfig
     */
    public UIConfiguration getUiConfig() {
        return uiConfig;
    }
}
