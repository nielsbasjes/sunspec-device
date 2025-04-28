package nl.basjes.sunspec.schema.generate;

import nl.basjes.modbus.device.api.ModbusDevice;
import nl.basjes.modbus.device.memory.MockedModbusDevice;

import static nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER;

/**
 * Extracted on 2023-08-10 from my own SMA SunnyBoy 3.6
 */
public final class DeviceSMASunnyBoy36 {

    private DeviceSMASunnyBoy36() {
    }

    public static ModbusDevice getDevice() {
        // Extracted on 2023-08-10 from my own SMA SunnyBoy 3.6
        return MockedModbusDevice.builder()
//            .withLogging()
            .withRegisters(
                HOLDING_REGISTER,
                40000,
                """
                  // Extracted on 2025-03-31 from my own SMA SunnyBoy 3.6

                  # SunS header
                  5375 6E53
                  # --------------------------------------
                  # Model 1: Common
                  0001 0042
                  # Model 1 data: 66 registers.
                  534D 4100 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 5342 332E 362D 3141
                  562D 3431 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  342E 3031 2E31 352E 5200 0000 0000 0000 3330 3035
                  3036 3734 3135 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 FFFF 8000
                  # --------------------------------------
                  # Model 11: Ethernet Link Layer
                  000B
                  # Model 11 has 13 registers.
                  000D
                  # The binary data
                  0000 0000 0002 0000 0040 ADA9 9576 0000 0000 0000
                  0000 FFFF FFFF
                  # --------------------------------------
                  # Model 12: IPv4
                  000C
                  # Model 12 has 98 registers.
                  0062
                  # The binary data
                  0000 0000 0000 0000 0001 0000 0005 0001 0000 3139
                  322E 3136 382E 302E 3137 3000 0000 3235 352E 3235
                  352E 3235 352E 3000 0000 3139 322E 3136 382E 302E
                  3100 0000 0000 3139 322E 3136 382E 302E 3100 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 8000
                  # --------------------------------------
                  # Model 101: Inverter (Single Phase)
                  0065
                  # Model 101 has 50 registers.
                  0032
                  # The binary data
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF FFFF 8000 0001 FFFF FFFE 8000 0001 8000 0001
                  8000 FFFD 002C 815A 0001 FFFF 8000 FFFF 8000 8000
                  0001 8000 8000 8000 8000 0000 FFFF FFFF 0000 0000
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF

                  # --------------------------------------
                  # Model 120: Nameplate
                  0078
                  # Model 120 has 26 registers.
                  001A
                  # The binary data
                  0004 0170 0001 0170 0001 00B8 8000 8000 00B8 0001
                  00A0 FFFF 0320 8000 8000 0320 FFFD FFFF 0002 FFFF
                  0000 FFFF 0001 FFFF 0001 8000
                  # --------------------------------------
                  # Model 121: Basic Settings
                  0079
                  # Model 121 has 30 registers.
                  001E
                  # The binary data
                  0170 00E6 0000 FFFF FFFF 0170 8000 8000 8000 8000
                  0014 8000 8000 8000 8000 FFFF FFFF 0341 0032 0001
                  0001 0000 0000 8000 0001 8000 0000 8000 0000 0000

                  # --------------------------------------
                  # Model 122: Measurements_Status
                  007A
                  # Model 122 has 44 registers.
                  002C
                  # The binary data
                  0000 0000 0000 0000 0000 01BD 0D85 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
                  0000 0000 0000 0000 0000 0000 0000 8000 8000 FFFF
                  8000 FFFF FFFF FFFF FFFF 0000 0000 0000 0000 FFFF
                  FFFF FFFF FFFF 0004
                  # --------------------------------------
                  # Model 123: Immediate Controls
                  007B
                  # Model 123 has 24 registers.
                  0018
                  # The binary data
                  FFFF FFFF 0000 0000 FFFF FFFF FFFF 0001 0000 FFFF
                  FFFF FFFF 0000 0000 8000 8000 FFFF FFFF FFFF 0001
                  0000 FFFE FFFC FFFE
                  # --------------------------------------
                  # Model 124: Storage
                  007C
                  # Model 124 has 24 registers.
                  0018
                  # The binary data
                  FFFF FFFF FFFF 0000 FFFF FFFF FFFF FFFF FFFF FFFF
                  8000 8000 FFFF FFFF FFFF FFFF 0000 8000 8000 8000
                  0000 8000 FFFE 8000
                  # --------------------------------------
                  # Model 126: Static Volt-VAR
                  007E
                  # Model 126 has 64 registers.
                  0040
                  # The binary data
                  0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000
                  0004 0002 2710 0000 2710 0000 2710 0000 2710 0000
                  2710 0000 2710 0000 2710 0000 2710 0000 FFFF 8000
                  FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                  FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                  FFFF 8000 0000 0000 0000 0000 0000 0000 0000 0000
                  000A 04B0 04B0 0000
                  # --------------------------------------
                  # Model 127: Freq-Watt Param
                  007F
                  # Model 127 has 10 registers.
                  000A
                  # The binary data
                  0028 0014 0014 0000 0001 000A 0000 FFFE 0000 8000

                  # --------------------------------------
                  # Model 128: Dynamic Reactive Current
                  0080
                  # Model 128 has 14 registers.
                  000E
                  # The binary data
                  FFFF FFFF FFFF 0000 003C 8000 FFFF 0046 0005 0000
                  FFFF FFFE 0000 8000
                  # --------------------------------------
                  # Model 131: Watt-PF
                  0083
                  # Model 131 has 64 registers.
                  0040
                  # The binary data
                  0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000
                  0004 2710 0000 2710 0000 2710 0000 2710 0000 2710
                  0000 2710 0000 2710 0000 2710 0000 8000 8000 8000
                  8000 8000 8000 8000 8000 8000 8000 8000 8000 8000
                  8000 8000 8000 8000 8000 8000 8000 8000 8000 8000
                  8000 0000 0000 0000 0000 0000 0000 0000 0000 000A
                  04B0 04B0 0000 8000
                  # --------------------------------------
                  # Model 132: Volt-Watt
                  0084
                  # Model 132 has 64 registers.
                  0040
                  # The binary data
                  0001 0000 FFFF FFFF FFFF 0001 0008 FFFE FFFE 0000
                  0002 0001 2710 2710 2710 2710 2710 0000 2710 0000
                  2710 0000 2710 0000 2710 0000 2710 0000 FFFF 8000
                  FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                  FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000
                  FFFF 8000 0000 0000 0000 0000 0000 0000 0000 0000
                  000A 04B0 04B0 0000
                  # --------------------------------------
                  # Model 160: Multiple MPPT Inverter Extension Model
                  00A0
                  # Model 160 has 128 registers.
                  0080
                  # The binary data
                  FFFF 0000 0001 8000 0000 0000 0006 FFFF 0001 0000
                  0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                  0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0002 0000
                  0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                  0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0003 0000
                  0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                  0000 0000 FFFF FFFF 8000 FFFF 0000 0000 0004 0000
                  0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                  0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF 0005 0000
                  0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                  0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF 0006 0000
                  0000 0000 0000 0000 0000 0000 0000 FFFF FFFF FFFF
                  0000 0000 FFFF FFFF 8000 FFFF FFFF FFFF
                  # --------------------------------------
                  # Model 129: LVRTD
                  0081
                  # Model 129 has 60 registers.
                  003C
                  # The binary data
                  0001 0001 FFFF FFFF FFFF 0001 0003 FFFD 0000 8000
                  0003 07D0 0050 2710 0014 2710 0014 FFFF FFFF FFFF
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF 0000 0000 0000 0000 0000 0000 0000 0000 0000

                  # --------------------------------------
                  # Model 130: HVRTD
                  0082
                  # Model 130 has 60 registers.
                  003C
                  # The binary data
                  0001 0001 FFFF FFFF FFFF 0001 0003 FFFD 0000 8000
                  0003 07D0 006E 2710 007A 2710 007A FFFF FFFF FFFF
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF
                  FFFF 0000 0000 0000 0000 0000 0000 0000 0000 0000

                  # NO MORE MODELS
                  FFFF 0000
                """
            )
            .build();
    }
}
