/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sma.internal.hardware.devices;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public abstract class AbstractSmaDevice implements SmaDevice {

    /**
     * defines valid LRIs for that kind of device
     */
    private static final List<LRIDefinition> validLRIDefinition = new ArrayList<LRIDefinition>();

    // EventPublisher eventPublisher = null;

    // @Override
    // public void setEventPublisher(EventPublisher eventPublisher) {
    // this.eventPublisher = eventPublisher;;
    // }
    //
    // @Override
    // public void unsetEventPublisher(EventPublisher eventPublisher) {
    // // TODO Auto-generated method stub
    //
    // }

    @Override
    public List<LRIDefinition> getValidLRIDefinitions() {
        return AbstractSmaDevice.validLRIDefinition;
    }
}
