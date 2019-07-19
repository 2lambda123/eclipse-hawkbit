/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tagdetails;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.common.table.BaseUIEntityEvent;
import org.eclipse.hawkbit.ui.common.tagdetails.TagPanelLayout.TagAssignmentListener;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.UI;

/**
 * Abstract class for target/ds tag token layout.
 *
 * @param <T>
 *            the special entity
 */
public abstract class AbstractTagToken<T extends BaseEntity> implements Serializable, TagAssignmentListener {

    protected static final int MAX_TAG_QUERY = 500;

    private static final long serialVersionUID = 6599386705285184783L;

    protected TagPanelLayout tagPanelLayout;

    protected final transient Map<Long, TagData> tagDetailsById = new ConcurrentHashMap<>();
    protected final transient Map<String, TagData> tagDetailsByName = new ConcurrentHashMap<>();

    protected SpPermissionChecker checker;

    protected VaadinMessageSource i18n;

    protected UINotification uinotification;

    protected transient EventBus.UIEventBus eventBus;

    protected ManagementUIState managementUIState;

    // BaseEntity implements Serializable so this entity is serializable. Maybe
    // a sonar bug
    @SuppressWarnings("squid:S1948")
    protected T selectedEntity;

    protected AbstractTagToken(final SpPermissionChecker checker, final VaadinMessageSource i18n,
            final UINotification uinotification, final UIEventBus eventBus, final ManagementUIState managementUIState) {
        this.checker = checker;
        this.i18n = i18n;
        this.uinotification = uinotification;
        this.eventBus = eventBus;
        this.managementUIState = managementUIState;
        createTagPanel();
        if (doSubscribeToEventBus()) {
            eventBus.subscribe(this);
        }
    }

    /**
     * Subscribes the view to the eventBus. Method has to be overriden (return
     * false) if the view does not contain any listener to avoid Vaadin blowing
     * up our logs with warnings.
     */
    protected boolean doSubscribeToEventBus() {
        return true;
    }

    protected void onBaseEntityEvent(final BaseUIEntityEvent<T> baseEntityEvent) {
        if (BaseEntityEventType.SELECTED_ENTITY != baseEntityEvent.getEventType()) {
            return;
        }
        UI.getCurrent().access(() -> {
            final T entity = baseEntityEvent.getEntity();
            if (entity != null) {
                selectedEntity = entity;
                repopulateToken();
            }
        });
    }

    private void createTagPanel() {
        tagPanelLayout = new TagPanelLayout(i18n, !isToggleTagAssignmentAllowed());
        tagPanelLayout.addTagAssignmentListener(this);
        tagPanelLayout.setSizeFull();
    }

    protected void repopulateToken() {
        tagDetailsById.clear();
        tagDetailsByName.clear();

        final List<TagData> allAssignableTags = getAllAssignableTags();
        allAssignableTags.forEach(tagData -> {
            tagDetailsByName.put(tagData.getName(), tagData);
            tagDetailsById.put(tagData.getId(), tagData);
        });
        tagPanelLayout.initializeTags(allAssignableTags, getAssignedTags());
    }

    protected void tagCreated(final TagData tagData) {
        tagDetailsByName.put(tagData.getName(), tagData);
        tagDetailsById.put(tagData.getId(), tagData);

        tagPanelLayout.tagCreated(tagData);
    }

    protected void tagDeleted(final Long id) {
        final TagData tagData = tagDetailsById.get(id);
        if (tagData != null) {
            tagPanelLayout.tagDeleted(tagData);

            tagDetailsByName.remove(tagData.getName());
            tagDetailsById.remove(id);
        }
    }

    @Override
    public void assignTag(final String tagName) {
        final TagData tagData = tagDetailsByName.get(tagName);
        if (tagData != null) {
            assignTag(tagData);
        }
    }

    @Override
    public void unassignTag(final String tagName) {
        final TagData tagData = tagDetailsByName.get(tagName);
        if (tagData != null) {
            unassignTag(tagData);
        }
    }

    protected abstract void assignTag(TagData tagData);

    protected abstract void unassignTag(TagData tagData);

    protected Long getTagIdByTagName(final Long tagId) {
        return tagDetailsById.entrySet().stream().filter(entry -> entry.getValue().getId().equals(tagId)).findAny()
                .map(Entry::getKey).orElse(null);
    }

    protected boolean checkAssignmentResult(final List<? extends NamedEntity> assignedEntities,
            final Long expectedAssignedEntityId) {
        if (assignedEntities != null && !assignedEntities.isEmpty() && expectedAssignedEntityId != null) {
            final List<Long> assignedDsIds = assignedEntities.stream().map(NamedEntity::getId)
                    .collect(Collectors.toList());
            if (assignedDsIds.contains(expectedAssignedEntityId)) {
                return true;
            }
        }
        return false;
    }

    protected boolean checkUnassignmentResult(final NamedEntity unAssignedDistributionSet,
            final Long expectedUnAssignedEntityId) {
        return unAssignedDistributionSet != null && expectedUnAssignedEntityId != null
                && unAssignedDistributionSet.getId().equals(expectedUnAssignedEntityId);
    }

    protected abstract Boolean isToggleTagAssignmentAllowed();

    protected abstract List<TagData> getAllAssignableTags();

    protected abstract List<TagData> getAssignedTags();

    public TagPanelLayout getTagPanel() {
        return tagPanelLayout;
    }
}
