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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sma.internal.SmaBinding;
import org.openhab.binding.sma.internal.SmaBridgeConfiguration;
import org.openhab.binding.sma.internal.discovery.SmaDiscoveryService;
import org.openhab.binding.sma.internal.hardware.devices.BluetoothSolarInverterPlant;
import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.InverterDataType;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.SmaUserGroup;
import org.openhab.binding.sma.internal.layers.Bluetooth;
import org.openhab.binding.sma.internal.util.SunriseSunset;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SmaHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class SmaBridgeHandler extends BaseBridgeHandler implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(SmaBridgeHandler.class);

    private static final float SunRSOffset = 450.0f;

    private final BundleContext bundleContext;

    @Nullable
    private ScheduledFuture<?> schedule;

    @Nullable
    private ServiceRegistration<DiscoveryService> discoveryServiceRegistration;

    @Nullable
    private SmaDiscoveryService discoveryService;

    private HashMap<Integer, SmaHandler> attachedThings = new HashMap<Integer, SmaHandler>(13);

    public SmaBridgeHandler(Bridge thing, BundleContext bundleContext) {
        super(thing);
        this.bundleContext = bundleContext;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        logger.info("SmaBridgeHandler.handleCommand({},{})", channelUID.toString(), command.toString());
    }

    @Override
    public void initialize() {

        logger.info("SmaBridgeHandler.initialize()");

        SmaDiscoveryService discovery = new SmaDiscoveryService(this);

        this.discoveryServiceRegistration = this.bundleContext.registerService(DiscoveryService.class, discovery, null);

        discoveryService = (SmaDiscoveryService) this.bundleContext
                .getService(this.discoveryServiceRegistration.getReference());

        if (discoveryService != null) {
            discoveryService.startScan(null);
        }

        // SmaConfig config = getConfigAs(SmaConfig.class);
        // schedule = scheduler.scheduleWithFixedDelay(this, 1, config.cycle, TimeUnit.SECONDS);

        // zum Testen starten wir die Abfrage alle 5 Minuten um 0,5,10,...
        int delay = 5 - (new Date().getMinutes() % 5);
        // delay = (delay + 7) % 10;
        schedule = scheduler.scheduleAtFixedRate(this, delay, 5, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (schedule != null) {
            schedule.cancel(true);
        }
        if (discoveryServiceRegistration != null) {
            discoveryServiceRegistration.unregister();
        }
        logger.info("SmaBridgeHandler.dispose()");
    }

    @Override
    public void run() {
        logger.debug("SmaBridgeHandler.initRunnable run()");

        SmaBridgeConfiguration config = getConfigAs(SmaBridgeConfiguration.class);
        if (!SunriseSunset.sunrise_sunset(config.latitude, config.longitude, SunRSOffset / 3600.0f)) {
            logger.debug("Nothing to do... it's dark.");
            return;
        }

        SmaBinding binding = new SmaBinding();
        BluetoothSolarInverterPlant plant = new BluetoothSolarInverterPlant(
                binding.createDevice(config.btAddress, config.userPassword));

        logger.trace("config.btAddress = {}, config.userPassword = {}", config.btAddress, config.userPassword);
        ThingStatus thingStatus = ThingStatus.UNKNOWN;

        HashMap<Integer, SmaHandler> attachedThingsCopy;
        synchronized (attachedThings) {
            attachedThingsCopy = new HashMap<Integer, SmaHandler>(attachedThings);
        }

        try {
            getData(config, plant);

            logger.trace("*******************");

            ArrayList<BluetoothSolarInverterPlant.Data> inverters = plant.getInverters();
            logger.trace("{} inverters found:", inverters.size());
            // for (int inv = 0; inverters.[inv] != null && inv < Inverters.length; inv++) {
            boolean complete = true;
            for (BluetoothSolarInverterPlant.Data inv : inverters) {

                discoveryService.notifyDiscovery(inv.getSerial().suSyID,
                        inv.getDeviceType() + " " + inv.getDeviceName());

                logger.trace("attachedThings = {}", attachedThingsCopy.toString());
                SmaHandler handler = attachedThingsCopy.get(new Integer(inv.getSerial().suSyID));
                if (handler != null) {
                    handler.dataReceived(inv);
                } else {
                    logger.info("No handler for suSyID = {} attachedThings.size() = {}", inv.getSerial().suSyID,
                            attachedThingsCopy.size());
                    complete = false;
                }

                logger.trace("SUSyID: {} - SN: {}", inv.getSerial().suSyID, inv.getSerial().serial);
                logger.trace("Device Name:      {}", inv.getDeviceName());
                // logger.info("Device Class: {}", inv.deviceClass);
                logger.trace("Device Type:      {}", inv.getDeviceType());
                logger.trace("Software Version: {}", inv.swVersion);
                logger.trace("Serial number:    {}", inv.getSerial().serial);
            }
            if (complete) {
                for (Entry<Integer, SmaHandler> handler : attachedThingsCopy.entrySet()) {
                    if (!inverters.stream().anyMatch(inv -> inv.getSerial().suSyID == handler.getKey())) {
                        logger.info("no inverter for handler with suSyID = {}", handler.getKey());
                        complete = false;
                        handler.getValue().setOffline();
                    }
                }
            }
            if (complete) {
                updateSums(inverters);
            }
            updateStatus(ThingStatus.ONLINE);

        } catch (IOException e) {

            logger.error("run() failed: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } catch (Exception e) {

            logger.error("run() unexpected exception", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        } finally {

            plant.exit();
            logger.debug("run() finished.");
        }
    }

    private void updateSums(ArrayList<BluetoothSolarInverterPlant.Data> inverters) {
        double eTotal = 0.0;
        boolean eTotalValid = true;
        double eToday = 0.0;
        boolean eTodayValid = true;
        double uacMax = 0.0;
        boolean uacMaxValid = false;
        double totalPac = 0.0;
        boolean totalPacValid = true;

        for (BluetoothSolarInverterPlant.Data inv : inverters) {

            if (eTotalValid && inv.isValid(SmaDevice.LRIDefinition.MeteringTotWhOut)) {
                eTotal += ((DecimalType) inv.getState(SmaDevice.LRIDefinition.MeteringTotWhOut)).doubleValue();
            } else {
                eTotalValid = false;
            }

            if (eTodayValid && inv.isValid(SmaDevice.LRIDefinition.MeteringDyWhOut)) {
                eToday += ((DecimalType) inv.getState(SmaDevice.LRIDefinition.MeteringDyWhOut)).doubleValue();
            } else {
                eTodayValid = false;
            }

            for (SmaDevice.LRIDefinition lriDef : new SmaDevice.LRIDefinition[] { SmaDevice.LRIDefinition.GridMsPhVphsA,
                    SmaDevice.LRIDefinition.GridMsPhVphsB, SmaDevice.LRIDefinition.GridMsPhVphsC }) {
                if (inv.isValid(lriDef)) {
                    double uac = ((DecimalType) inv.getState(lriDef)).doubleValue();
                    if (uac > uacMax) {
                        uacMax = uac;
                    }
                    uacMaxValid = true;
                }
            }

            if (eTodayValid && inv.isValid(SmaDevice.LRIDefinition.GridMsTotW)) {
                totalPac += ((DecimalType) inv.getState(SmaDevice.LRIDefinition.GridMsTotW)).doubleValue();
            } else {
                totalPacValid = false;
            }
        }

        if (eTotalValid) {
            updateState(new ChannelUID(getThing().getUID(), "etotal"), new DecimalType(eTotal));
        }

        if (eTodayValid) {
            updateState(new ChannelUID(getThing().getUID(), "etoday"), new DecimalType(eToday));
        }

        if (uacMaxValid) {
            updateState(new ChannelUID(getThing().getUID(), "uacmax"), new DecimalType(uacMax));
        }

        if (totalPacValid) {
            updateState(new ChannelUID(getThing().getUID(), "totalpac"), new DecimalType(totalPac));
        }
    }

    private void getData(SmaBridgeConfiguration config, BluetoothSolarInverterPlant inverter) throws IOException {

        inverter.init(new Bluetooth(new SmaBluetoothAddress(config.btAddress, 1)));
        inverter.logon(SmaUserGroup.User, config.userPassword);
        inverter.setInverterTime();

        InverterDataType[] required = new SmaDevice.InverterDataType[] { SmaDevice.InverterDataType.SoftwareVersion,
                SmaDevice.InverterDataType.TypeLabel, SmaDevice.InverterDataType.DeviceStatus,
                SmaDevice.InverterDataType.MaxACPower, SmaDevice.InverterDataType.EnergyProduction,
                SmaDevice.InverterDataType.SpotACVoltage, SmaDevice.InverterDataType.SpotACTotalPower };

        ArrayList<InverterDataType> remaining = new ArrayList<InverterDataType>(Arrays.asList(required));

        for (int retry = 0; retry < 3 && !remaining.isEmpty(); retry++) {

            for (int j = remaining.size() - 1; j >= 0; j--) {
                if (inverter.getInverterData(remaining.get(j))) {
                    remaining.remove(j);
                }
            }
        }

        for (int i = 0; i < remaining.size(); i++) {
            logger.error("getInverterData({}) failed", remaining.get(i).toString());
        }

        inverter.logoff();
    }

    public void registerInverter(int susyId, SmaHandler smaHandler) {
        synchronized (attachedThings) {
            attachedThings.put(susyId, smaHandler);
            logger.info("registerInverter({}) finished: attachedThings.size() = {}", susyId, attachedThings.size());
        }
    }

    public void unregisterInverter(int susyId, SmaHandler smaHandler) {
        synchronized (attachedThings) {
            attachedThings.remove(susyId);
            logger.info("unregisterInverter({}) finished: attachedThings.size() = {}", susyId, attachedThings.size());
        }
    }
}
