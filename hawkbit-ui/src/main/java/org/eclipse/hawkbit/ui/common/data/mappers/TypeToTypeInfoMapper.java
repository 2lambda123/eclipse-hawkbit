/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.Type;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;

/**
 * 
 * Use to map {@link Type} to {@link ProxyTypeInfo}
 *
 * @param <T>
 *            type of input type
 */
public class TypeToTypeInfoMapper<T extends Type>
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyTypeInfo, T> {

    @Override
    public ProxyTypeInfo map(final T entity) {
        return new ProxyTypeInfo(entity.getId(), entity.getName(), entity.getKey());
    }

}
