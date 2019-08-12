/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test serializability of DDI api model 'DdiProgress'
 */
@Feature("Model Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class DdiProgressTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldSerializeAndDeserializeObject() throws IOException {
        // Setup
        DdiProgress ddiProgress = new DdiProgress(30, 100);

        // Test
        String serializedDdiProgress = mapper.writeValueAsString(ddiProgress);
        DdiProgress deserializedDdiProgress = mapper.readValue(serializedDdiProgress, DdiProgress.class);

        assertThat(serializedDdiProgress).contains(ddiProgress.getCnt().toString(), ddiProgress.getOf().toString());
        assertThat(deserializedDdiProgress.getCnt()).isEqualTo(ddiProgress.getCnt());
        assertThat(deserializedDdiProgress.getOf()).isEqualTo(ddiProgress.getOf());
    }

    @Test
    public void shouldDeserializeObjectWithUnknownProperty() throws IOException {
        // Setup
        String serializedDdiProgress = "{\"cnt\":30,\"of\":100,\"unknownProperty\":\"test\"}";

        // Test
        DdiProgress ddiProgress = mapper.readValue(serializedDdiProgress, DdiProgress.class);

        assertThat(ddiProgress.getCnt()).isEqualTo(30);
        assertThat(ddiProgress.getOf()).isEqualTo(100);
    }

    @Test(expected = com.fasterxml.jackson.databind.exc.MismatchedInputException.class)
    public void shouldFailForObjectWithWrongDataTypes() throws IOException {
        // Setup
        String serializedDdiProgress = "{\"cnt\":[30],\"of\":100}";

        // Test
        mapper.readValue(serializedDdiProgress, DdiProgress.class);
    }
}