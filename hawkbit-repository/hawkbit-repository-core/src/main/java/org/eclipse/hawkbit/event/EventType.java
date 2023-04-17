/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.event;

/**
 * The {@link EventType} class declares the event-type and it's corresponding
 * encoding value in the payload of an remote header. The event-type is encoded
 * into the payload of the message which is distributed.
 *
 * To encode and decode the event class type we need some conversation mapping
 * between the actual class and the corresponding integer value which is the
 * encoded value in the byte-payload.
 */
public class EventType {

    private int value;

    /**
     * Constructor.
     */
    public EventType() {
        // for marshalling and unmarshalling.
    }

    /**
     * Constructor.
     *
     * @param value
     *            the value to initialize
     */
    public EventType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
