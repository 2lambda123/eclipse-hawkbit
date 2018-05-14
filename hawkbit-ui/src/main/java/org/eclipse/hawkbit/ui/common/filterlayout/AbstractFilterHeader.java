/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.components.ConfigMenuBar;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.VerticalLayout;

/**
 * Parent class for filter button header layout.
 */
public abstract class AbstractFilterHeader extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    private final SpPermissionChecker permChecker;

    private transient EventBus.UIEventBus eventBus;

    private Label title;

    private Button hideIcon;

    private ConfigMenuBar menu;

    private final VaadinMessageSource i18n;

    private HorizontalLayout typeHeaderLayout;

    private Button cancelTagButton;

    protected AbstractFilterHeader(final SpPermissionChecker permChecker, final UIEventBus eventBus,
            final VaadinMessageSource i18n) {
        this.permChecker = permChecker;
        this.eventBus = eventBus;
        this.i18n = i18n;
        createComponents();
        buildLayout();
    }

    public void removeAbortButtonAndAddMenuBar() {
        final HorizontalLayout layout = getTypeHeaderLayout();
        layout.iterator().forEachRemaining(component -> {
            if ("cancelUpdateTag".equals(component.getId())) {
                getTypeHeaderLayout().removeComponent(cancelTagButton);
                getTypeHeaderLayout().addComponent(getMenu(), 1);
            }
        });
    }

    protected void removeMenuBarAndAddAbortButton() {
        getTypeHeaderLayout().removeComponent(getMenu());
        getTypeHeaderLayout().addComponent(createAbortButtonForUpdateOrDeleteTag(), 1);
    }

    protected Button createAbortButtonForUpdateOrDeleteTag() {
        cancelTagButton = SPUIComponentProvider.getButton("cancelUpdateTag", "", "", null, false,
                FontAwesome.TIMES_CIRCLE, SPUIButtonStyleNoBorder.class);
        cancelTagButton.addClickListener(this::abortUpdateOrDeleteTag);
        return cancelTagButton;
    }

    protected void abortUpdateOrDeleteTag(final ClickEvent event) {
        getTypeHeaderLayout().removeComponent(cancelTagButton);
        getTypeHeaderLayout().addComponent(getMenu(), 1);
    }

    /**
     * Create required components.
     */
    private void createComponents() {
        title = createHeaderCaption();

        if (isAddTagRequired()) {
            menu = new ConfigMenuBar(permChecker.hasCreateRepositoryPermission(),
                    permChecker.hasUpdateRepositoryPermission(), permChecker.hasDeleteRepositoryPermission(),
                    getAddButtonCommand(), getUpdateButtonCommand(), getDeleteButtonCommand());
        }
        hideIcon = SPUIComponentProvider.getButton(getHideButtonId(), "", "", "", true, FontAwesome.TIMES,
                SPUIButtonStyleNoBorder.class);
        hideIcon.addClickListener(

                event -> hideFilterButtonLayout());

    }

    protected abstract Command getDeleteButtonCommand();

    protected abstract Command getUpdateButtonCommand();

    protected abstract Command getAddButtonCommand();

    /**
     * Build layout.
     */
    private void buildLayout() {
        setStyleName("filter-btns-header-layout");
        typeHeaderLayout = new HorizontalLayout();
        typeHeaderLayout.setWidth(100.0F, Unit.PERCENTAGE);
        typeHeaderLayout.addComponentAsFirst(title);
        typeHeaderLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        typeHeaderLayout.setComponentAlignment(title, Alignment.TOP_LEFT);
        if (menu != null) {
            typeHeaderLayout.addComponent(menu);
            typeHeaderLayout.setComponentAlignment(menu, Alignment.TOP_LEFT);
        }
        typeHeaderLayout.addComponent(hideIcon);
        typeHeaderLayout.setComponentAlignment(hideIcon, Alignment.TOP_RIGHT);
        typeHeaderLayout.setExpandRatio(title, 1.0F);
        addComponent(typeHeaderLayout);
    }

    private Label createHeaderCaption() {
        return new LabelBuilder().name(getTitle()).buildCaptionLabel();
    }

    /**
     * 
     * @return
     */
    protected abstract String getHideButtonId();

    /**
     * Get the title to be displayed on the header of filter button layout.
     * 
     * @return title to be displayed.
     */
    protected abstract String getTitle();

    /**
     * Space required to show drop hits in the filter layout header.
     * 
     * @return true to display drop hit.
     */
    protected abstract boolean dropHitsRequired();

    /**
     * This method will be called when hide button (X) is clicked.
     */
    protected abstract void hideFilterButtonLayout();

    /**
     * Add/update type button id.
     */
    protected abstract String getConfigureFilterButtonId();

    /**
     * @return
     */
    protected abstract boolean isAddTagRequired();

    public SpPermissionChecker getPermChecker() {
        return permChecker;
    }

    public EventBus.UIEventBus getEventBus() {
        return eventBus;
    }

    public Button getHideIcon() {
        return hideIcon;
    }

    public ConfigMenuBar getMenu() {
        return menu;
    }

    public VaadinMessageSource getI18n() {
        return i18n;
    }

    public HorizontalLayout getTypeHeaderLayout() {
        return typeHeaderLayout;
    }

}
