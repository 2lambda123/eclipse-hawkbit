/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEventTest;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.messaging.Message;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test the remote entity events.
 */
@Features("Component Tests - Repository")
@Stories("Entity Events")
public abstract class AbstractRemoteEntityEventTest<E extends BaseEntity> extends AbstractRemoteEventTest {

    protected RemoteEntityEvent<?> assertAndCreateRemoteEvent(final Class<? extends RemoteEntityEvent<?>> eventType) {

        final E baseEntity = createEntity();

        Constructor<?> constructor = null;
        for (final Constructor<?> constructors : eventType.getDeclaredConstructors()) {
            if (constructors.getParameterCount() == 2) {
                constructor = constructors;
            }
        }

        if (constructor == null) {
            throw new IllegalArgumentException("No suitable constructor foundes");
        }

        try {
            final RemoteEntityEvent<?> event = (RemoteEntityEvent<?>) constructor.newInstance(baseEntity, "Node");
            assertEntity(baseEntity, event);
            return event;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException e) {
            fail("Exception should not happen " + e.getMessage());
        }
        return null;
    }

    protected RemoteEntityEvent<?> assertEntity(final E baseEntity, final RemoteEntityEvent<?> event) {
        assertThat(event.getEntity()).isSameAs(baseEntity);

        final Message<?> message = createMessage(event);
        final RemoteEntityEvent<?> underTestCreatedEvent = (RemoteEntityEvent<?>) getAbstractMessageConverter()
                .fromMessage(message, event.getClass());
        assertThat(underTestCreatedEvent.getEntity()).isEqualTo(baseEntity);
        return underTestCreatedEvent;
    }

    protected abstract E createEntity();
}
