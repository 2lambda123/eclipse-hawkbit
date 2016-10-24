/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote.entity;

import org.eclipse.hawkbit.repository.model.Target;

/**
 * Defines the remote event for updating a {@link Target}.
 *
 */
public class TargetUpdatedEvent extends RemoteEntityEvent<Target> {

    private static final long serialVersionUID = 5665118668865832477L;

    /**
     * Constructor for json serialization.
     * 
     * @param tenant
     *            the tenant
     * @param entityId
     *            the entity id
     * @param entityClass
     *            the entity entityClassName
     * @param applicationId
     *            the origin application id
     */
    public TargetUpdatedEvent(final String tenant, final Long entityId, final String entityClass,
            final String applicationId) {
        super(tenant, entityId, entityClass, applicationId);
    }

    /**
     * Constructor.
     * 
     * @param baseEntity
     *            Target entity
     * @param applicationId
     *            the origin application id
     */
    public TargetUpdatedEvent(final Target baseEntity, final String applicationId) {
        super(baseEntity, applicationId);
    }

}
