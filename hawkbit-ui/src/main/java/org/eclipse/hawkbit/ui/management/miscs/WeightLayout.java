/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.miscs;

import java.util.function.Consumer;

import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Validator;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class WeightLayout extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private final MaintenanceWindowLayout maintenanceWindowLayout;
    private final VaadinMessageSource i18n;
    private final Consumer<Boolean> saveButtonToggle;
    private TextField weightField;
    private Label weightLabel;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param saveButtonToggle
     *            boolean indicating to enable/disable OK button on confirmation
     *            dialog of manual assignment
     * @param maintenanceWindowLayout
     *            the maintenance window layout
     */
    public WeightLayout(final VaadinMessageSource i18n, final Consumer<Boolean> saveButtonToggle,
            final MaintenanceWindowLayout maintenanceWindowLayout) {
        this.i18n = i18n;
        this.saveButtonToggle = saveButtonToggle;
        this.maintenanceWindowLayout = maintenanceWindowLayout;
        
        createWeightField();
        createWeightLabel(); 
        final HorizontalLayout weightContainer = new HorizontalLayout();
        weightContainer.addComponent(weightField);
        weightContainer.addComponent(weightLabel);
        addComponent(weightContainer);

        // should a seperate ID be defined like in the other layouts??
    }

    /**
     * Returns the weight field component.
     * 
     * @return weightField
     */
    public TextField getWeightField() {
        return weightField;
    }

    private TextField createWeightField() {
        weightField = new TextFieldBuilder(32).prompt(i18n.getMessage("textfield.weight"))
                .id(UIComponentIdProvider.ROLLOUT_WEIGHT_FIELD_ID).buildTextComponent();
        weightField.addStyleName(ValoTheme.TEXTFIELD_SMALL);
        weightField.setStyleName("dist-window-actiontype");
        weightField.setConverter(new StringToIntegerConverter());
        weightField.setConversionError(i18n.getMessage("message.enter.number"));
        weightField.setSizeUndefined();
        weightField.setMaxLength(4);
        weightField.addValidator(new NullValidator(null, false));
        weightField.addValidator(new WeightFieldValidator());
        weightField.addTextChangeListener(event -> saveButtonToggle.accept(onWeightChange(event)));
        return weightField;
    }

    private Label createWeightLabel() {
        weightLabel = new LabelBuilder().name(i18n.getMessage("textfield.weight")).buildLabel();
        weightLabel.setContentMode(ContentMode.HTML);
        weightLabel.setValue(weightLabel.getValue().concat(" <span style='color:#ed473b'>*</span>"));
        weightLabel.setStyleName("dist-window-actiontype");

        return weightLabel;
    }

    private boolean onWeightChange(final TextChangeEvent event) {
        if (event.getText().isEmpty() || event.getText().matches("^\\D*$")) {
            return false;
        } else if (checkWeightValue(event.getText())) {
            weightField.setValue(event.getText());
            return validateSaveButtonToggle();
        }
        return false;
    }

    private boolean validateSaveButtonToggle() {
        if (maintenanceWindowLayout.isEnabled()) {
            if (maintenanceWindowLayout.isScheduleAndDurationValid()) {
                return maintenanceWindowLayout.isEnabled();
            } else {
                return !maintenanceWindowLayout.isEnabled();
            }
        }
        return !maintenanceWindowLayout.isEnabled();
    }

    public boolean checkWeightValue(final String weightValue) {
        return (Integer.valueOf(weightValue.replace(",", "")) >= 0
                && Integer.valueOf(weightValue.replace(",", "")) <= 1000);
    }

    class WeightFieldValidator implements Validator {
        private static final long serialVersionUID = 1L;
        private static final String MESSAGE_ROLLOUT_FIELD_VALUE_RANGE = "message.rollout.field.value.range";

        @Override
        public void validate(final Object value) {
            if (value != null) {
                new IntegerRangeValidator(i18n.getMessage(MESSAGE_ROLLOUT_FIELD_VALUE_RANGE, 0, 1000), 0, 1000)
                        .validate(Integer.valueOf(value.toString()));
            }
        }
    }
}
