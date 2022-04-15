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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public interface SmaDevice {

    public static final short NaN_S16 = (short) 0x8000; // "Not a Number" representation for SHORT (converted to 0)
    public static final short NaN_U16 = (short) 0xFFFF; // "Not a Number" representation for USHORT (converted to 0)
    public static final int NaN_S32 = 0x80000000; // "Not a Number" representation for LONG (converted to 0)
    public static final int NaN_U32 = 0xFFFFFFFF; // "Not a Number" representation for ULONG (converted to 0)
    public static final long NaN_S64 = 0x8000000000000000L; // "Not a Number" representation for LONGLONG (converted to
                                                            // 0)
    public static final long NaN_U64 = 0xFFFFFFFFFFFFFFFFL; // "Not a Number" representation for ULONGLONG (converted to
                                                            // 0)

    public static final String strWatt = "{}: {} (W)   {}";
    public static final String strkW = "{}: {} (kW)   {}";
    public static final String strVolt = "{}: {} (V)   {}";
    public static final String strAmp = "{}: {} (A)   {}";
    public static final String strkWh = "{}: {} (kWh) {}";
    public static final String strHour = "{}: {} (h)   {}";

    //@formatter:off
    public enum InverterDataType {
        None                (0     , 0         , 0         , 0), // undefined
        EnergyProduction    (1 << 0, 0x54000200, 0x00260100, 0x002622FF), // SPOT_ETODAY, SPOT_ETOTAL
        SpotDCPower         (1 << 1, 0x53800200, 0x00251E00, 0x00251EFF), // SPOT_PDC1, SPOT_PDC2
        SpotDCVoltage       (1 << 2, 0x53800200, 0x00451F00, 0x004521FF), // SPOT_UDC1, SPOT_UDC2, SPOT_IDC1, SPOT_IDC2
        SpotACPower         (1 << 3, 0x51000200, 0x00464000, 0x004642FF), // SPOT_PAC1, SPOT_PAC2, SPOT_PAC3
        SpotACVoltage       (1 << 4, 0x51000200, 0x00464800, 0x004655FF), // SPOT_UAC1, SPOT_UAC2, SPOT_UAC3, SPOT_IAC1, SPOT_IAC2, SPOT_IAC3
        SpotGridFrequency   (1 << 5, 0x51000200, 0x00465700, 0x004657FF), // SPOT_FREQ
        MaxACPower          (1 << 6, 0x51000200, 0x00411E00, 0x004120FF), // INV_PACMAX1, INV_PACMAX2, INV_PACMAX3
        MaxACPower2         (1 << 7, 0x51000200, 0x00832A00, 0x00832AFF), // INV_PACMAX1_2
        SpotACTotalPower    (1 << 8, 0x51000200, 0x00263F00, 0x00263FFF), // SPOT_PACTOT
        TypeLabel           (1 << 9, 0x58000200, 0x00821E00, 0x008220FF), // INV_NAME, INV_TYPE, INV_CLASS
        OperationTime       (1 << 10, 0x54000200, 0x00462E00, 0x00462FFF), // SPOT_OPERTM, SPOT_FEEDTM
        SoftwareVersion     (1 << 11, 0x58000200, 0x00823400, 0x008234FF), // INV_SWVERSION
        DeviceStatus        (1 << 12, 0x51800200, 0x00214800, 0x002148FF), // INV_STATUS
        GridRelayStatus     (1 << 13, 0x51800200, 0x00416400, 0x004164FF), // INV_GRIDRELAY
        BatteryChargeStatus (1 << 14, 0x51000200, 0x00295A00, 0x00295AFF), //
        BatteryInfo         (1 << 15, 0x51000200, 0x00491E00, 0x00495DFF), //
        InverterTemperature (1 << 16, 0x52000200, 0x00237700, 0x002377FF); //
        //@formatter:on

        private final int value;
        private final int command;
        private final int first;
        private final int last;

        private InverterDataType(int value) {
            this(value, 0, 0, 0);
        }

        private InverterDataType(int value, int command, int first, int last) {
            this.value = value;
            this.command = command;
            this.first = first;
            this.last = last;
        }

        private static HashMap<Integer, SmaDevice.InverterDataType> map;

        public static InverterDataType fromOrdinal(int i) {
            if (map == null) {
                map = new HashMap<Integer, SmaDevice.InverterDataType>(SmaDevice.InverterDataType.values().length);
                for (SmaDevice.InverterDataType e : SmaDevice.InverterDataType.values()) {
                    map.put(e.getValue(), e);
                }
            }
            return map.get(i);
        }

        public int getValue() {
            return value;
        }

        public int getCommand() {
            return command;
        }

        public int getFirst() {
            return first;
        }

        public int getLast() {
            return last;
        }
    }

    public enum DeviceClass {
        AllDevices(8000), // DevClss0
        SolarInverter(8001), // DevClss1
        WindTurbineInverter(8002), // DevClss2
        BatteryInverter(8007), // DevClss7
        Consumer(8033), // DevClss33
        SensorSystem(8064), // DevClss64
        ElectricityMeter(8065), // DevClss65
        CommunicationProduct(8128); // DevClss128

        private final int value;

        private DeviceClass(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum SmaUserGroup {
        // User Group
        User(0x07),
        Installer(0x0A);

        private final int value;

        private SmaUserGroup(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    //@formatter:off
    public enum LRIDefinition {
        OperationHealth         (0x00214800, "INV_STATUS", InverterDataType.DeviceStatus), // *08* Condition (aka INV_STATUS)
        CoolsysTmpNom           (0x00237700), // *40* Operating condition temperatures
        DcMsWatt1               (0x00251E00, "SPOT_PDC1", InverterDataType.SpotDCPower, 1), // *40* DC power input (aka SPOT_PDC1 / SPOT_PDC2)
        DcMsWatt2               (0x00251E00, "SPOT_PDC2", InverterDataType.SpotDCPower, 2), // *40* DC power input (aka SPOT_PDC1 / SPOT_PDC2)
        MeteringTotWhOut        (0x00260100, "SPOT_ETOTAL", InverterDataType.EnergyProduction, "etotal"), // *00* Total yield (aka SPOT_ETOTAL)
        MeteringDyWhOut         (0x00262200, "SPOT_ETODAY", InverterDataType.EnergyProduction, "etoday"),  // *00* Day yield (aka SPOT_ETODAY)
        GridMsTotW              (0x00263F00, "SPOT_PACTOTAL", InverterDataType.SpotACTotalPower, "totalpac"), // *40* Power (aka SPOT_PACTOT)
        BatChaStt               (0x00295A00, "BAT_STATUS", InverterDataType.BatteryChargeStatus), // *00* Current battery charge status
        OperationHealthSttOk    (0x00411E00, "INV_PACMAX1", InverterDataType.MaxACPower), // *00* Nominal power in Ok Mode (aka INV_PACMAX1)
        OperationHealthSttWrn   (0x00411F00, "INV_PACMAX2", InverterDataType.MaxACPower), // *00* Nominal power in Warning Mode (aka INV_PACMAX2)
        OperationHealthSttAlm   (0x00412000, "INV_PACMAX3", InverterDataType.MaxACPower), // *00* Nominal power in Fault Mode (aka INV_PACMAX3)
        OperationGriSwStt       (0x00416400, "INV_GRIDRELAY", InverterDataType.GridRelayStatus), // *08* Grid relay/contactor (aka INV_GRIDRELAY)
        OperationRmgTms         (0x00416600), // *00* Waiting time until feed-in
        DcMsVol1                (0x00451F00, "SPOT_UDC1", InverterDataType.SpotDCVoltage, 1), // *40* DC voltage input (aka SPOT_UDC1  SPOT_UDC2)
        DcMsVol2                (0x00451F00, "SPOT_UDC1", InverterDataType.SpotDCVoltage, 2), // *40* DC voltage input (aka SPOT_UDC1  SPOT_UDC2)
        DcMsAmp1                (0x00452100, "SPOT_IDC1", InverterDataType.SpotDCVoltage, 1), // *40* DC current input (aka SPOT_IDC1 /SPOT_IDC2)
        DcMsAmp2                (0x00452100, "SPOT_IDC2", InverterDataType.SpotDCVoltage, 2), // *40* DC current input (aka SPOT_IDC1 /SPOT_IDC2)
        MeteringPvMsTotWhOut    (0x00462300), // *00* PV generation counter reading
        MeteringGridMsTotWhOut  (0x00462400), // *00* Grid feed-in counter reading
        MeteringGridMsTotWhIn   (0x00462500), // *00* Grid reference counter reading
        MeteringCsmpTotWhIn     (0x00462600), // *00* Meter reading consumption meter
        MeteringGridMsDyWhOut   (0x00462700), // *00* ?
        MeteringGridMsDyWhIn    (0x00462800), // *00* ?
        MeteringTotOpTms        (0x00462E00, "SPOT_OPERTM", InverterDataType.OperationTime), // *00* Operating time (aka SPOT_OPERTM)
        MeteringTotFeedTms      (0x00462F00, "SPOT_FEEDTM", InverterDataType.OperationTime), // *00* Feed-in time (aka SPOT_FEEDTM)
        MeteringGriFailTms      (0x00463100), // *00* Power outage
        MeteringWhIn            (0x00463A00), // *00* Absorbed energy
        MeteringWhOut           (0x00463B00), // *00* Released energy
        MeteringPvMsTotWOut     (0x00463500), // *40* PV power generated
        MeteringGridMsTotWOut   (0x00463600), // *40* Power grid feed-in
        MeteringGridMsTotWIn    (0x00463700), // *40* Power grid reference
        MeteringCsmpTotWIn      (0x00463900), // *40* Consumer power
        GridMsWphsA             (0x00464000, "SPOT_PAC1", InverterDataType.SpotACPower), // *40* Power L1 (aka SPOT_PAC1)
        GridMsWphsB             (0x00464100, "SPOT_PAC2", InverterDataType.SpotACPower), // *40* Power L2 (aka SPOT_PAC2)
        GridMsWphsC             (0x00464200, "SPOT_PAC3", InverterDataType.SpotACPower), // *40* Power L3 (aka SPOT_PAC3)
        GridMsPhVphsA           (0x00464800, "SPOT_UAC1", InverterDataType.SpotACVoltage, "uac1"), // *00* Grid voltage phase L1 (aka SPOT_UAC1)
        GridMsPhVphsB           (0x00464900, "SPOT_UAC2", InverterDataType.SpotACVoltage, "uac2"), // *00* Grid voltage phase L2 (aka SPOT_UAC2)
        GridMsPhVphsC           (0x00464A00, "SPOT_UAC3", InverterDataType.SpotACVoltage, "uac3"), // *00* Grid voltage phase L3 (aka SPOT_UAC3)
        GridMsAphsA_1           (0x00465000, "SPOT_IAC1", InverterDataType.SpotACVoltage), // *00* Grid current phase L1 (aka SPOT_IAC1)
        GridMsAphsB_1           (0x00465100, "SPOT_IAC2", InverterDataType.SpotACVoltage), // *00* Grid current phase L2 (aka SPOT_IAC2)
        GridMsAphsC_1           (0x00465200, "SPOT_IAC3", InverterDataType.SpotACVoltage), // *00* Grid current phase L3 (aka SPOT_IAC3)
        GridMsAphsA             (0x00465300), // *00* Grid current phase L1 (aka SPOT_IAC1_2)
        GridMsAphsB             (0x00465400), // *00* Grid current phase L2 (aka SPOT_IAC2_2)
        GridMsAphsC             (0x00465500), // *00* Grid current phase L3 (aka SPOT_IAC3_2)
        GridMsHz                (0x00465700, "FREQ", InverterDataType.SpotGridFrequency), // *00* Grid frequency (aka SPOT_FREQ)
        MeteringSelfCsmpSelfCsmpWh      (0x0046AA00), // *00* Energy consumed internally
        MeteringSelfCsmpActlSelfCsmp    (0x0046AB00), // *00* Current self-consumption
        MeteringSelfCsmpSelfCsmpInc     (0x0046AC00), // *00* Current rise in self-consumption
        MeteringSelfCsmpAbsSelfCsmpInc  (0x0046AD00), // *00* Rise in self-consumption
        MeteringSelfCsmpDySelfCsmpInc   (0x0046AE00), // *00* Rise in self-consumption today
        BatDiagCapacThrpCnt     (0x00491E00), // *40* Number of battery charge throughputs
        //TODO Check battery data assoc
        BatDiagTotAhIn          (0x00492600, "BAT_CHARGE", InverterDataType.BatteryChargeStatus), // *00* Amp hours counter for battery charge
        BatDiagTotAhOut         (0x00492700, "BAT_DISCHARGE", InverterDataType.BatteryChargeStatus), // *00* Amp hours counter for battery discharge
        BatTmpVal               (0x00495B00, "BAT_TEMP", InverterDataType.BatteryInfo), // *40* Battery temperature
        BatVol                  (0x00495C00, "BAT_VOL", InverterDataType.BatteryInfo), // *40* Battery voltage
        BatAmp                  (0x00495D00, "BAT_CUURENT", InverterDataType.BatteryInfo), // *40* Battery current
        NameplateLocation       (0x00821E00, "INV_NAME", InverterDataType.TypeLabel), // *10* Device name (aka INV_NAME)
        NameplateMainModel      (0x00821F00, "INV_CLASS", InverterDataType.TypeLabel), // *08* Device class (aka INV_CLASS)
        NameplateModel          (0x00822000, "INV_TYPE", InverterDataType.TypeLabel, "invtype"), // *08* Device type (aka INV_TYPE)
        NameplateAvalGrpUsr     (0x00822100), // * * Unknown
        NameplatePkgRev         (0x00823400, "INV_SWVERSION", InverterDataType.SoftwareVersion), // *08* Software package (aka INV_SWVER)
        InverterWLim            (0x00832A00); // *00* Maximum active power device (aka INV_PACMAX1_2) (Some inverters like SB3300/SB1200)
        //@formatter:on

        private final int value;
        private final int cls;
        private final String code;
        private final InverterDataType data;
        private final String channelId;

        private LRIDefinition(int value) {
            this(value, Integer.toString(value), InverterDataType.None, 0, null);
        }

        private LRIDefinition(int value, String code, InverterDataType data) {
            this(value, code, data, 0, null);
        }

        private LRIDefinition(int value, String code, InverterDataType data, String channelId) {
            this(value, code, data, 0, channelId);
        }

        private LRIDefinition(int value, String code, InverterDataType data, int cls) {
            this(value, code, data, cls, null);
        }

        private LRIDefinition(int value, String code, InverterDataType data, int cls, String channelId) {
            this.value = value;
            this.code = code.toUpperCase();
            this.data = data;
            this.cls = cls;
            this.channelId = channelId;
        }

        public int getValue() {
            return value + cls;
        }

        public String getCode() {
            return code;
        }

        public InverterDataType getData() {
            return data;
        }

        public String getChannelId() {
            return channelId;
        }

        private static HashMap<Integer, SmaDevice.LRIDefinition> valueMap;
        private static HashMap<String, SmaDevice.LRIDefinition> codeMap;

        public static LRIDefinition fromOrdinal(int i) {
            if (valueMap == null) {
                prepareMap();
            }
            return valueMap.get(i);
        }

        public static LRIDefinition fromOrdinal(String code) {
            if (codeMap == null) {
                prepareMap();
            }
            return codeMap.get(code.toUpperCase());
        }

        public static boolean containsCode(String code) {
            if (codeMap == null) {
                prepareMap();
            }
            return codeMap.containsKey(code);
        }

        private static synchronized void prepareMap() {
            valueMap = new HashMap<Integer, SmaDevice.LRIDefinition>(SmaDevice.LRIDefinition.values().length);
            codeMap = new HashMap<String, SmaDevice.LRIDefinition>(SmaDevice.LRIDefinition.values().length);

            for (SmaDevice.LRIDefinition e : SmaDevice.LRIDefinition.values()) {
                valueMap.put(e.getValue(), e);
                codeMap.put(e.getCode(), e);
            }
        }
    }

    public static String getModel(int code) {
        switch (code) {
            //@formatter:off
            case 0x2337: /*9015*/ return "SB 700";
            case 0x2338: /*9016*/ return "SB 700U";
            case 0x2339: /*9017*/ return "SB 1100";
            case 0x233A: /*9018*/ return "SB 1100U";
            case 0x233B: /*9019*/ return "SB 1100LV";
            case 0x233C: /*9020*/ return "SB 1700";
            case 0x233D: /*9021*/ return "SB 1900TLJ";
            case 0x233E: /*9022*/ return "SB 2100TL";
            case 0x233F: /*9023*/ return "SB 2500";
            case 0x2340: /*9024*/ return "SB 2800";
            case 0x2341: /*9025*/ return "SB 2800i";
            case 0x2342: /*9026*/ return "SB 3000";
            case 0x2343: /*9027*/ return "SB 3000US";
            case 0x2344: /*9028*/ return "SB 3300";
            case 0x2345: /*9029*/ return "SB 3300U";
            case 0x2346: /*9030*/ return "SB 3300TL";
            case 0x2347: /*9031*/ return "SB 3300TL HC";
            case 0x2348: /*9032*/ return "SB 3800";
            case 0x2349: /*9033*/ return "SB 3800U";
            case 0x234A: /*9034*/ return "SB 4000US";
            case 0x234B: /*9035*/ return "SB 4200TL";
            case 0x234C: /*9036*/ return "SB 4200TL HC";
            case 0x234D: /*9037*/ return "SB 5000TL";
            case 0x234E: /*9038*/ return "SB 5000TLW";
            case 0x234F: /*9039*/ return "SB 5000TL HC";
            case 0x2354: /*9044*/ return "SB 5000US";
            case 0x2357: /*9047*/ return "SB 6000US";
            case 0x235D: /*9053*/ return "SB 7000US";
            case 0x2363: /*9059*/ return "SB 3000 K";
            case 0x236A: /*9066*/ return "SB 1200";
            case 0x236F: /*9071*/ return "SB 2000HF-30";
            case 0x2370: /*9072*/ return "SB 2500HF-30";
            case 0x2371: /*9073*/ return "SB 3000HF-30";
            case 0x022E: /* 558*/ return "SB 3000TL-20";
            case 0x0166: /* 358*/ return "SB 4000TL-20";
            case 0x0167: /* 359*/ return "SB 5000TL-20";
            case 0x2372: /*9074*/ return "SB 3000TL-21";
            case 0x2373: /*9075*/ return "SB 4000TL-21";
            case 0x2374: /*9076*/ return "SB 5000TL-21";
            case 0x2375: /*9077*/ return "SB 2000HFUS-30";
            case 0x2376: /*9078*/ return "SB 2500HFUS-30";
            case 0x2377: /*9079*/ return "SB 3000HFUS-30";
            case 0x2378: /*9080*/ return "SB 8000TLUS";
            case 0x2379: /*9081*/ return "SB 9000TLUS";
            case 0x237A: /*9082*/ return "SB 10000TLUS";
            case 0x237B: /*9083*/ return "SB 8000US";
            case 0x237E: /*9086*/ return "SB 3800US-10";
            case 0x2390: /*9104*/ return "SB 3000TL-JP-21";
            case 0x2391: /*9105*/ return "SB 3500TL-JP-21";
            case 0x2392: /*9106*/ return "SB 4000TL-JP-21";
            case 0x2393: /*9107*/ return "SB 4500TL-JP-21";
            case 0x2395: /*9109*/ return "SB 1600TL-10";
            case 0x23A9: /*9129*/ return "SB 3800-11";
            case 0x23AA: /*9130*/ return "SB 3300-11";
            case 0x23AD: /*9133*/ return "SB 2000HFUS-32";
            case 0x23AE: /*9134*/ return "SB 2500HFUS-32";
            case 0x23AF: /*9135*/ return "SB 3000HFUS-32";
            case 0x23B5: /*9141*/ return "SB 3000US-12";
            case 0x23B6: /*9142*/ return "SB 3800-US-12";
            case 0x23B7: /*9143*/ return "SB 4000US-12";
            case 0x23B8: /*9144*/ return "SB 5000US-12";
            case 0x23B9: /*9145*/ return "SB 6000US-12";
            case 0x23BA: /*9146*/ return "SB 7000US-12";
            case 0x23BB: /*9147*/ return "SB 8000US-12";
            case 0x23BC: /*9148*/ return "SB 8000TLUS-12";
            case 0x23BD: /*9149*/ return "SB 9000TLUS-12";
            case 0x23BE: /*9150*/ return "SB 10000TLUS-12";
            case 0x23BF: /*9151*/ return "SB 11000TLUS-12";
            case 0x23C0: /*9152*/ return "SB 7000TLUS-12";
            case 0x23C1: /*9153*/ return "SB 6000TLUS-12";
            case 0x23C2: /*9154*/ return "SB 1300TL-10";
            case 0x23C8: /*9160*/ return "SB 3600TL-20";
            case 0x23C9: /*9161*/ return "SB 3000TL-JP-22";
            case 0x23CA: /*9162*/ return "SB 3500TL-JP-22";
            case 0x23CB: /*9163*/ return "SB 4000TL-JP-22";
            case 0x23CC: /*9164*/ return "SB 4500TL-JP-22";
            case 0x23CD: /*9165*/ return "SB 3600TL-21";
            case 0x23D9: /*9177*/ return "SB 240-10";
            case 0x23DA: /*9178*/ return "SB 240-US-10";
            case 0x23DF: /*9183*/ return "SB 2000TLST-21";
            case 0x23E0: /*9184*/ return "SB 2500TLST-21";
            case 0x23E1: /*9185*/ return "SB 3000TLST-21";
            case 0x23EE: /*9198*/ return "SB 3000TL-US-22";
            case 0x23EF: /*9199*/ return "SB 3800TL-US-22";
            case 0x23F0: /*9200*/ return "SB 4000TL-US-22";
            case 0x23F1: /*9201*/ return "SB 5000TL-US-22";
            case 0x2351: /*9041*/ return "SMC 4600A";
            case 0x2352: /*9042*/ return "SMC 5000";
            case 0x2353: /*9043*/ return "SMC 5000A";
            case 0x2355: /*9045*/ return "SMC 6000";
            case 0x2356: /*9046*/ return "SMC 6000A";
            case 0x2358: /*9048*/ return "SMC 6000UL";
            case 0x2359: /*9049*/ return "SMC 6000TL";
            case 0x235A: /*9050*/ return "SMC 6500A";
            case 0x235B: /*9051*/ return "SMC 7000A";
            case 0x235C: /*9052*/ return "SMC 7000HV";
            case 0x235E: /*9054*/ return "SMC 7000TL";
            case 0x235F: /*9055*/ return "SMC 8000TL";
            case 0x2360: /*9056*/ return "SMC 9000TL";
            case 0x2361: /*9057*/ return "SMC 10000TL";
            case 0x2362: /*9058*/ return "SMC 11000TL";
            case 0x2366: /*9062*/ return "SMC 11000TLRP";
            case 0x2367: /*9063*/ return "SMC 10000TLRP";
            case 0x2368: /*9064*/ return "SMC 9000TLRP";
            case 0x2369: /*9065*/ return "SMC 7000HVRP";
            case 0x23A6: /*9126*/ return "SMC 6000A-11";
            case 0x23A7: /*9127*/ return "SMC 5000A-11";
            case 0x23A8: /*9128*/ return "SMC 4600A-11";
            case 0x236B: /*9067*/ return "STP 10000TL-10";
            case 0x236C: /*9068*/ return "STP 12000TL-10";
            case 0x236D: /*9069*/ return "STP 15000TL-10";
            case 0x236E: /*9070*/ return "STP 17000TL-10";
            case 0x238A: /*9098*/ return "STP 5000TL-20";
            case 0x238B: /*9099*/ return "STP 6000TL-20";
            case 0x238C: /*9100*/ return "STP 7000TL-20";
            case 0x238D: /*9101*/ return "STP 8000TL-10";
            case 0x238E: /*9102*/ return "STP 9000TL-20";
            case 0x238F: /*9103*/ return "STP 8000TL-20";
            case 0x23AB: /*9131*/ return "STP 20000TL-10";
            case 0x23B3: /*9139*/ return "STP 20000TLHE-10";
            case 0x23B4: /*9140*/ return "STP 15000TLHE-10";
            case 0x23DD: /*9181*/ return "STP 20000TLEE-10";
            case 0x23DE: /*9182*/ return "STP 15000TLEE-10";
            case 0x23EA: /*9194*/ return "STP 12kTL-US-10";
            case 0x23EB: /*9195*/ return "STP 15kTL-US-10";
            case 0x23EC: /*9196*/ return "STP 20kTL-US-10";
            case 0x23ED: /*9197*/ return "STP 24kTL-US-10";
            case 0x237C: /*9084*/ return "WB 3600TL-20";
            case 0x237D: /*9085*/ return "WB 5000TL-20";
            case 0x2398: /*9112*/ return "WB 2000HF-30";
            case 0x2399: /*9113*/ return "WB 2500HF-30";
            case 0x239A: /*9114*/ return "WB 3000HF-30";
            case 0x239B: /*9115*/ return "WB 2000HFUS-30";
            case 0x239C: /*9116*/ return "WB 2500HFUS-30";
            case 0x239D: /*9117*/ return "WB 3000HFUS-30";
            case 0x23B0: /*9136*/ return "WB 2000HFUS-32";
            case 0x23B1: /*9137*/ return "WB 2500HFUS-32";
            case 0x23B2: /*9138*/ return "WB 3000HFUS-32";
            case 0x23D3: /*9171*/ return "WB 3000TL-21";
            case 0x23D4: /*9172*/ return "WB 3600TL-21";
            case 0x23D5: /*9173*/ return "WB 4000TL-21";
            case 0x23D6: /*9174*/ return "WB 5000TL-21";
            case 0x23E2: /*9186*/ return "WB 2000TLST-21";
            case 0x23E3: /*9187*/ return "WB 2500TLST-21";
            case 0x23E4: /*9188*/ return "WB 3000TLST-21";
            case 0x23F2: /*9202*/ return "WB 3000TL-US-22";
            case 0x23F3: /*9203*/ return "WB 3800TL-US-22";
            case 0x23F4: /*9204*/ return "WB 4000TL-US-22";
            case 0x23F5: /*9205*/ return "WB 5000TL-US-22";
            case 0x23E5: /*9189*/ return "WTP 5000TL-20";
            case 0x23E6: /*9190*/ return "WTP 6000TL-20";
            case 0x23E7: /*9191*/ return "WTP 7000TL-20";
            case 0x23E8: /*9192*/ return "WTP 8000TL-20";
            case 0x23E9: /*9193*/ return "WTP 9000TL-20";
            case 0x2407: /*9223*/ return "Sunny Island 6.0H";
            case 0x2408: /*9224*/ return "Sunny Island 8.0H";
            default:
                return "UNKNOWN TYPE";
                //@formatter:on
        }
    }

    void init() throws IOException;

    void exit();

    public void logon(SmaUserGroup userGroup, String password) throws IOException;

    public void logoff() throws IOException;

    // void setEventPublisher(EventPublisher eventPublisher);
    //
    // void unsetEventPublisher(EventPublisher eventPublisher);

    List<LRIDefinition> getValidLRIDefinitions();

    String getValueAsString(LRIDefinition lriDefinition);
}
