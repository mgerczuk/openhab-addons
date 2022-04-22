# SMA Binding

This binding is reads values from SMA inverters over Bluetooth.

The code is mostly ported from the [SBFspot](https://github.com/SBFspot/SBFspot) project

## Supported Things

SMA inverters with Bluetooth interface.

## Discovery

After creating the SMA inverter bridge representing the inverter network, the single inverters are discovered automatically and added to the inbox.

## Binding Configuration

No binding configuration required.

## Thing Configuration

Bridge configuration:

The inverter things are created automatically and do not need further configuration.

## Channels

Bridge channels

| channel | type   | description                |
|---------|--------|----------------------------|
| etotal  | Number | Production total sum [kWh] |


Inverter channels

| channel  | type   | description                  |
|----------|--------|------------------------------|
| etoday   | Number | Production today [kWh]       |
| etotal   | Number | Production total [kWh]       |
| uac1     | Number | Voltage AC1                  |
| uac2     | Number | Voltage AC2                  |
| uac3     | Number | Voltage AC3                  |
| totalpac | Number | Total power AC               |
| invtype  | String | Inverter type                |
