/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

import com.google.common.base.MoreObjects;
import org.eclipse.hawkbit.repository.model.TargetType;

import java.util.Objects;

/**
 * Proxy for {@link TargetType}.
 */
public class ProxyTargetType extends ProxyType {

    private static final long serialVersionUID = 1L;

    private boolean isNoTargetType;

    /**
     * Constructor
     */
    public ProxyTargetType() {
    }

    /**
     * Constructor for ProxyTargetType
     *
     * @param id
     *          Type id
     * @param name
     *          Type name
     * @param colour
     *          Type colour
     */
    public ProxyTargetType(final Long id, final String name, final String colour) {
        setId(id);
        setName(name);
        setColour(colour);
    }

    public boolean isNoTargetType() {
        return isNoTargetType;
    }

    public void setNoTargetType(boolean noTargetType) {
        isNoTargetType = noTargetType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProxyTargetType other = (ProxyTargetType) obj;
        return Objects.equals(this.getId(), other.getId()) && Objects.equals(this.getName(), other.getName())
                && Objects.equals(this.getColour(), other.getColour());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getColour());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("id", getId()).add("name", getName()).add("color", getColour())
                .toString();
    }
}
