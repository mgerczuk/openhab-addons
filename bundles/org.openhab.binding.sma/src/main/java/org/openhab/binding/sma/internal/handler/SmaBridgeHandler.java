/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sma.internal.SmaBridgeConfiguration;
import org.openhab.binding.sma.internal.discovery.SmaDiscoveryService;
import org.openhab.binding.sma.internal.hardware.devices.BluetoothSolarInverterPlant;
import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.InverterQuery;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.SmaUserGroup;
import org.openhab.binding.sma.internal.layers.Bluetooth;
import org.openhab.binding.sma.internal.util.SunriseSunset;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
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

    private final BundleContext bundleContext;

    @Nullable
    private SunriseSunset srs;

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

        SmaBridgeConfiguration config = getConfigAs(SmaBridgeConfiguration.class);

        srs = new SunriseSunset(config.latitude, config.longitude);

        int delay = config.cycle - (new Date().getSeconds() % config.cycle);
        schedule = scheduler.scheduleAtFixedRate(this, delay, config.cycle, TimeUnit.SECONDS);
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

        Calendar now = Calendar.getInstance();
        Calendar sunrise = srs.getSunrise(now);
        Calendar sunset = srs.getSunset(now);
        sunset.add(Calendar.MINUTE, 5);

        if (now.before(sunrise) || now.after(sunset)) {
            logger.debug("Nothing to do... it's dark.");
            return;
        }

        BluetoothSolarInverterPlant plant = new BluetoothSolarInverterPlant();

        logger.trace("config.btAddress = {}, config.userPassword = {}", config.btAddress, config.userPassword);

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
                logger.trace("Software Version: {}", inv.getSwVersion());
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

            if (srs.logErrors()) {
                logger.error("run() failed: {}", e.getMessage());
            } else {
                logger.debug("run() failed: {}", e.getMessage());
            }
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
                eTotal += ((QuantityType<?>) inv.getState(SmaDevice.LRIDefinition.MeteringTotWhOut)).doubleValue();
            } else {
                eTotalValid = false;
            }

            if (eTodayValid && inv.isValid(SmaDevice.LRIDefinition.MeteringDyWhOut)) {
                eToday += ((QuantityType<?>) inv.getState(SmaDevice.LRIDefinition.MeteringDyWhOut)).doubleValue();
            } else {
                eTodayValid = false;
            }

            for (SmaDevice.LRIDefinition lriDef : new SmaDevice.LRIDefinition[] { SmaDevice.LRIDefinition.GridMsPhVphsA,
                    SmaDevice.LRIDefinition.GridMsPhVphsB, SmaDevice.LRIDefinition.GridMsPhVphsC }) {
                if (inv.isValid(lriDef)) {
                    double uac = ((QuantityType<?>) inv.getState(lriDef)).doubleValue();
                    if (uac > uacMax) {
                        uacMax = uac;
                    }
                    uacMaxValid = true;
                }
            }

            if (eTodayValid && inv.isValid(SmaDevice.LRIDefinition.GridMsTotW)) {
                totalPac += ((QuantityType<?>) inv.getState(SmaDevice.LRIDefinition.GridMsTotW)).doubleValue();
            } else {
                totalPacValid = false;
            }
        }

        if (eTotalValid) {
            updateState(new ChannelUID(getThing().getUID(), "etotal"), new QuantityType<>(eTotal, Units.WATT_HOUR));
        }

        if (eTodayValid) {
            updateState(new ChannelUID(getThing().getUID(), "etoday"), new QuantityType<>(eToday, Units.WATT_HOUR));
        }

        if (uacMaxValid) {
            updateState(new ChannelUID(getThing().getUID(), "uacmax"), new QuantityType<>(uacMax, Units.VOLT));
        }

        if (totalPacValid) {
            updateState(new ChannelUID(getThing().getUID(), "totalpac"), new QuantityType<>(totalPac, Units.WATT));
        }
    }

    private void getData(SmaBridgeConfiguration config, BluetoothSolarInverterPlant inverter) throws IOException {

        inverter.init(new Bluetooth(new SmaBluetoothAddress(config.btAddress, 1)));
        inverter.logon(SmaUserGroup.User, config.userPassword);
        inverter.setInverterTime();

        InverterQuery[] required = new SmaDevice.InverterQuery[] { SmaDevice.InverterQuery.SoftwareVersion,
                SmaDevice.InverterQuery.TypeLabel, SmaDevice.InverterQuery.DeviceStatus,
                SmaDevice.InverterQuery.MaxACPower, SmaDevice.InverterQuery.EnergyProduction,
                SmaDevice.InverterQuery.SpotACVoltage, SmaDevice.InverterQuery.SpotACTotalPower };

        ArrayList<InverterQuery> remaining = new ArrayList<InverterQuery>(Arrays.asList(required));

        for (int retry = 0; retry < 3 && !remaining.isEmpty(); retry++) {

            for (int j = remaining.size() - 1; j >= 0; j--) {
                if (inverter.getInverterData(remaining.get(j))) {
                    remaining.remove(j);
                }
            }
        }

        for (int i = 0; i < remaining.size(); i++) {
            if (srs.logErrors()) {
                logger.warn("getInverterData({}) failed", remaining.get(i).toString());
            } else {
                logger.debug("getInverterData({}) failed", remaining.get(i).toString());
            }
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
