/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.sma.internal.handler;

import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.sma.internal.SmaInverterConfiguration;
import org.openhab.binding.sma.internal.hardware.devices.BluetoothSolarInverterPlant.Data;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.LRIDefinition;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SmaHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Martin Gerczuk - Initial contribution
 */
// @NonNullByDefault
public class SmaHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SmaHandler.class);
    private int susyId;
    private SmaBridgeHandler bridgeHandler;

    public SmaHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (channelUID.getId().equals(CHANNEL_ETODAY)) {
        // TODO: handle command

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
        // }
    }

    @Override
    public void initialize() {

        Bridge bridge = getBridge();
        bridgeHandler = bridge == null ? null : (SmaBridgeHandler) bridge.getHandler();

        SmaInverterConfiguration config = getConfigAs(SmaInverterConfiguration.class);
        susyId = config.susyid;

        bridgeHandler.registerInverter(susyId, this);

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        // updateStatus(ThingStatus.INITIALIZING);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    @Override
    public void dispose() {
        bridgeHandler.unregisterInverter(susyId, this);
        super.dispose();
    }

    public void dataReceived(Data inv) {
        logger.trace("dataReceived");

        for (Entry<LRIDefinition, State> entry : inv.getEntries()) {
            updateState(new ChannelUID(getThing().getUID(), entry.getKey().getChannelId()), entry.getValue());
        }

        updateStatus(ThingStatus.ONLINE);
    }

    public void setOffline() {
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public @NonNull String toString() {
        return String.format("SmaHandler {suSyID = %d}", susyId);
    }
}
