/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.ui.components.ProxyTarget;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.QueryDefinition;

/**
 * @author gah6kor
 *
 */
public class RolloutGroupTargetsBeanQuery extends AbstractBeanQuery<ProxyTarget> {

    private static final long serialVersionUID = -8841076207255485907L;

    private final Sort sort = new Sort(Direction.ASC, "createdAt");

    private transient Page<TargetWithActionStatus> firstPageTargetSets = null;

    private transient RolloutManagement rolloutManagement;

    private transient RolloutGroupManagement rolloutGroupManagement;

    private transient RolloutUIState rolloutUIState;

    private final RolloutGroup rolloutGroup;

    private transient I18N i18N;

    /**
     * Parametric Constructor.
     * 
     * @param definition
     *            as QueryDefinition
     * @param queryConfig
     *            as Config
     * @param sortPropertyIds
     *            as sort
     * @param sortStates
     *            as Sort status
     */
    public RolloutGroupTargetsBeanQuery(final QueryDefinition definition, final Map<String, Object> queryConfig,
            final Object[] sortPropertyIds, final boolean[] sortStates) {

        super(definition, queryConfig, sortPropertyIds, sortStates);

        rolloutGroup = getRolloutUIState().getRolloutGroup().isPresent() ? getRolloutUIState().getRolloutGroup().get()
                : null;

    }

    @Override
    protected ProxyTarget constructBean() {
        return new ProxyTarget();
    }

    @Override
    protected List<ProxyTarget> loadBeans(final int startIndex, final int count) {
        List<TargetWithActionStatus> rolloutGroupTargetsList = new ArrayList<TargetWithActionStatus>();
        if (startIndex == 0 && firstPageTargetSets != null) {
            rolloutGroupTargetsList = firstPageTargetSets.getContent();
        } else if (null != rolloutGroup) {
            rolloutGroupTargetsList = getRolloutGroupManagement()
                    .findAllTargetsWithActionStatus(new PageRequest(startIndex / count, count), rolloutGroup)
                    .getContent();
        }
        return getProxyRolloutGroupTargetsList(rolloutGroupTargetsList);
    }

    private List<ProxyTarget> getProxyRolloutGroupTargetsList(final List<TargetWithActionStatus> rolloutGroupTargets) {
        final List<ProxyTarget> proxyTargetBeans = new ArrayList<ProxyTarget>();
        for (final TargetWithActionStatus targetWithActionStatus : rolloutGroupTargets) {
            final Target targ = targetWithActionStatus.getTarget();
            final ProxyTarget prxyTarget = new ProxyTarget();
            prxyTarget.setTargetIdName(targ.getTargetIdName());
            prxyTarget.setName(targ.getName());
            prxyTarget.setDescription(targ.getDescription());
            prxyTarget.setControllerId(targ.getControllerId());
            prxyTarget.setInstallationDate(targ.getTargetInfo().getInstallationDate());
            prxyTarget.setAddress(targ.getTargetInfo().getAddress());
            prxyTarget.setLastTargetQuery(targ.getTargetInfo().getLastTargetQuery());
            prxyTarget.setLastModifiedDate(SPDateTimeUtil.getFormattedDate(targ.getLastModifiedAt()));
            prxyTarget.setCreatedDate(SPDateTimeUtil.getFormattedDate(targ.getCreatedAt()));
            prxyTarget.setCreatedAt(targ.getCreatedAt());
            prxyTarget.setCreatedByUser(HawkbitCommonUtil.getIMUser(targ.getCreatedBy()));
            prxyTarget.setModifiedByUser(HawkbitCommonUtil.getIMUser(targ.getLastModifiedBy()));
            if (targetWithActionStatus.getStatus() != null) {
                prxyTarget.setStatus(targetWithActionStatus.getStatus());
            }
            prxyTarget.setLastTargetQuery(targ.getTargetInfo().getLastTargetQuery());
            prxyTarget.setTargetInfo(targ.getTargetInfo());
            prxyTarget.setPollStatusToolTip(
                    HawkbitCommonUtil.getPollStatusToolTip(prxyTarget.getTargetInfo().getPollStatus(), getI18N()));
            prxyTarget.setId(targ.getId());
            proxyTargetBeans.add(prxyTarget);

        }
        return proxyTargetBeans;
    }

    @Override
    protected void saveBeans(final List<ProxyTarget> arg0, final List<ProxyTarget> arg1, final List<ProxyTarget> arg2) {
        /**
         * No implementation required.
         */
    }

    @Override
    public int size() {
        long size = 0;
        if (null != rolloutGroup) {
            firstPageTargetSets = getRolloutGroupManagement()
                    .findAllTargetsWithActionStatus(new PageRequest(0, SPUIDefinitions.PAGE_SIZE, sort), rolloutGroup);
            size = firstPageTargetSets.getTotalElements();
        }
        getRolloutUIState().setRolloutGroupTargetsTotalCount(size);
        if (size > SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES) {
            getRolloutUIState().setRolloutGroupTargetsTruncated(size - SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES);
            return SPUIDefinitions.MAX_TARGET_TABLE_ENTRIES;
        }

        return (int) size;
    }

    /**
     * @return the rolloutManagement
     */
    public RolloutManagement getRolloutManagement() {
        if (null == rolloutManagement) {
            rolloutManagement = SpringContextHelper.getBean(RolloutManagement.class);
        }
        return rolloutManagement;
    }

    /**
     * @return the rolloutManagement
     */
    public RolloutGroupManagement getRolloutGroupManagement() {
        if (null == rolloutGroupManagement) {
            rolloutGroupManagement = SpringContextHelper.getBean(RolloutGroupManagement.class);
        }
        return rolloutGroupManagement;
    }

    /**
     * @return the rolloutUIState
     */
    public RolloutUIState getRolloutUIState() {
        if (null == rolloutUIState) {
            rolloutUIState = SpringContextHelper.getBean(RolloutUIState.class);
        }
        return rolloutUIState;
    }

    private I18N getI18N() {
        if (i18N == null) {
            i18N = SpringContextHelper.getBean(I18N.class);
        }
        return i18N;
    }

}
