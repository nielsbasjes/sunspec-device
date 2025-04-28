package nl.basjes.sunspec.devices

import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder

object DeviceOther {
    @JvmStatic
    val device: ModbusDevice
        get() =// Extracted on 2023-08-10 from my own SMA SunnyBoy 3.6
            builder() //            .withLogging()
                .withRegisters(
                    AddressClass.HOLDING_REGISTER,
                    40000,  // The SunS header
                    // The SunS header
                    "5375 6E53 " +

                    // Model Id 1 at 40004.
                    "0001 0042 " +
                    // Model Id 1 is 66 registers.
                    "4649 4D45 5200 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "2D33 5135 382D 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "3078 3035 3543 2F30 7830 4235 372F 0000 3233 3330 4600 0000 0000 0000 0000 0000 " +
                    "3131 3837 3437 2D33 5135 382D 3432 3231 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0001 FFFF " +

                    // Model Id 103 at 40072.
                    "0067 0032 " +
                    // Model Id 103 is 50 registers.
                    "0C81 042B 042A 042C FFFF 1DE8 1DDE 1DEC 1147 1140 1141 FFFF 373A 0001 1387 FFFE " +
                    "373A 0001 0001 0001 D8F0 FFFC 02BD 2174 0001 0609 FFFF FFFF 8000 3869 0001 0217 " +
                    "0302 8000 8000 FFFF 0004 0006 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 " +
                    "0000 0000 " +

                    // Model Id 120 at 40124.
                    "0078 001A " +
                    // Model Id 120 is 26 registers.
                    "0004 4844 0001 4844 0001 445C 445C BBA4 BBA4 0001 0087 0000 FFFF 0001 FFFF 0001 " +
                    "FFFD FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF " +

                    // Model Id 121 at 40152.
                    "0079 001E " +
                    // Model Id 121 is 30 registers.
                    "4844 FFFF 8000 FFFF FFFF 4844 445C 445C BBA4 BBA4 014D 8000 8000 8000 8000 FFFF " +
                    "FFFF FFFF FFFF FFFF 0001 8000 8000 8000 0001 0001 FFFF 8000 8000 8000 " +

                    // Model Id 122 at 40184.
                    "007A 002C " +
                    // Model Id 122 is 44 registers.
                    "0001 0000 0001 0000 0000 1B5E 3DA0 0000 0000 1B65 36C0 0000 0000 0000 0000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 8000 8000 FFFF 8000 0000 " +
                    "0000 0000 0000 0000 0000 0000 0000 2E30 F1B7 0000 01B1 0003 " +


                    // Model Id 123 at 40230.
                    "007B 0018 " +
                    // Model Id 123 is 24 registers.
                    "0000 003C 0001 03E8 0000 003C 0000 0000 2710 0000 003C 0000 0000 0000 0000 8000 " +
                    "0000 003C 0000 0000 0000 FFFF FFFC FFFF " +


                    // Model Id 126 at 40256.
                    "007E 00E2 " +
                    // Model Id 126 is 226 registers.
                    "0001 0000 0000 FFFF FFFF 0004 000A FFFF FFFF 0000 000A 0000 0384 01B4 0398 0000 " +
                    "0438 0000 044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 6D6F 6465 6C20 3100 0000 0000 0000 0000 FFFF FFFF FFFF 0000 " +
                    "000A 0000 0384 01B4 0398 0000 0438 0000 044C FE4C 044C FE4C 044C FE4C 044C FE4C " +
                    "044C FE4C 044C FE4C 044C FE4C FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 6D6F 6465 6C20 3200 0000 0000 " +
                    "0000 0000 FFFF FFFF FFFF 0000 000A 0000 0384 01B4 0398 0000 0438 0000 044C FE4C " +
                    "044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "6D6F 6465 6C20 3300 0000 0000 0000 0000 FFFF FFFF FFFF 0000 000A 0000 0384 01B4 " +
                    "0398 0000 0438 0000 044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C 044C FE4C " +
                    "044C FE4C FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 6D6F 6465 6C20 3400 0000 0000 0000 0000 FFFF FFFF " +
                    "FFFF 0000 " +

                    // Model Id 127 at 40484.
                    "007F 000A " +
                    // Model Id 127 is 10 registers.
                    "0028 139C 138D 0001 0000 0258 0000 FFFE 0000 FFFF " +

                    // Model Id 129 at 40496.
                    "0081 003C " +
                    // Model Id 129 is 60 registers.
                    "0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFF FFFF 0006 0064 0320 001E 012C 0000 " +
                    "0015 0000 0015 0000 0015 0000 0015 FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000 " +


                    // Model Id 130 at 40558.
                    "0082 003C " +
                    // Model Id 130 is 60 registers.
                    "0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFF FFFF 0006 000A 047E 0005 04E0 0000 " +
                    "04E0 0000 04E0 0000 04E0 0000 04E0 FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000 " +


                    // Model Id 132 at 40620.
                    "0084 00E2 " +
                    // Model Id 132 is 226 registers.
                    "0001 0000 0000 FFFF FFFF 0004 000A FFFF FFFF 0000 000A 0001 0384 03E8 03BC 03E8 " +
                    "043E 03E8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 6D6F 6465 6C20 3100 0000 0000 0000 0000 0009 1770 1770 0000 " +
                    "000A 0001 0384 03E8 03BC 03E8 043E 03E8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 " +
                    "0480 00C8 0480 00C8 0480 00C8 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 6D6F 6465 6C20 3200 0000 0000 " +
                    "0000 0000 0009 1770 1770 0000 000A 0001 0384 03E8 03BC 03E8 043E 03E8 0480 00C8 " +
                    "0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "6D6F 6465 6C20 3300 0000 0000 0000 0000 0009 1770 1770 0000 000A 0001 0384 03E8 " +
                    "03BC 03E8 043E 03E8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 0480 00C8 " +
                    "0480 00C8 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 FFFF 8000 " +
                    "FFFF 8000 FFFF 8000 FFFF 8000 6D6F 6465 6C20 3400 0000 0000 0000 0000 0009 1770 " +
                    "1770 0000 " +

                    // Model Id 135 at 40848.
                    "0087 003C " +
                    // Model Id 135 is 60 registers.
                    "0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFE FFFF 0006 000A 128E 000A 1194 0000 " +
                    "1194 0000 1194 0000 1194 0000 1194 FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000 " +


                    // Model Id 136 at 40910.
                    "0088 003C " +
                    // Model Id 136 is 60 registers.
                    "0001 0000 FFFF FFFF FFFF 0001 0006 FFFE FFFE FFFF 0006 000A 141E 000A 1964 0000 " +
                    "1964 0000 1964 0000 1964 0000 1964 FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000 " +


                    // Model Id 139 at 40972.
                    "008B 003C " +
                    // Model Id 139 is 60 registers.
                    "0001 0000 FFFF FFFF FFFF 0001 0001 FFFE FFFF 0001 0001 FFFF 01F4 FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000 " +


                    // Model Id 140 at 41034.
                    "008C 003C " +
                    // Model Id 140 is 60 registers.
                    "0001 0000 FFFF FFFF FFFF 0001 0001 FFFE FFFF 0001 0001 FFFF 04E0 FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF FFFF " +
                    "FFFF FFFF FFFF 6D6F 6465 6C20 3100 0000 0000 0000 0000 0000 " +


                    // Model Id 145 at 41096.
                    "0091 0008 " +
                    // Model Id 145 is 8 registers.
                    "EA60 FFFF FFFF FFFF 0064 FFFF FFFF FFFF " +


                    // Model Id 160 at 41106.
                    "00A0 00F8 " +
                    // Model Id 160 is 248 registers.
                    "FFFF FFFF 0001 8000 0000 0000 000C FFFF 0001 5056 3100 0000 0000 0000 0000 0000 " +
                    "0000 0080 2553 04CA 0000 0000 FFFF FFFF 8000 0004 0000 0000 0002 5056 3200 0000 " +
                    "0000 0000 0000 0000 0000 0080 24AD 04B6 0000 0000 FFFF FFFF 8000 0004 0000 0000 " +
                    "0003 5056 3300 0000 0000 0000 0000 0000 0000 0082 24CD 04C7 0000 0000 FFFF FFFF " +
                    "8000 0004 0000 0000 0004 5056 3400 0000 0000 0000 0000 0000 0000 0080 24BD 04B3 " +
                    "0000 0000 FFFF FFFF 8000 0004 0000 0000 0005 5056 3500 0000 0000 0000 0000 0000 " +
                    "0000 0080 2473 04B5 0000 0000 FFFF FFFF 8000 0004 0000 0000 0006 5056 3600 0000 " +
                    "0000 0000 0000 0000 0000 007F 248A 04A8 0000 0000 FFFF FFFF 8000 0004 0000 0000 " +
                    "0007 5056 3700 0000 0000 0000 0000 0000 0000 0080 248E 04B6 0000 0000 FFFF FFFF " +
                    "8000 0004 0000 0000 0008 5056 3800 0000 0000 0000 0000 0000 0000 0080 240F 049D " +
                    "0000 0000 FFFF FFFF 8000 0004 0000 0000 0009 5056 3900 0000 0000 0000 0000 0000 " +
                    "0000 0081 2465 04B8 0000 0000 FFFF FFFF 8000 0004 0000 0000 000A 5056 3130 0000 " +
                    "0000 0000 0000 0000 0000 0081 2445 04AA 0000 0000 FFFF FFFF 8000 0004 0000 0000 " +
                    "000B 5056 3131 0000 0000 0000 0000 0000 0000 0081 243A 04AD 0000 0000 FFFF FFFF " +
                    "8000 0004 0000 0000 000C 5056 3132 0000 0000 0000 0000 0000 0000 0081 2463 04AE " +
                    "0000 0000 FFFF FFFF 8000 0004 0000 0000 " +

                    // Model Id 65230 at 41356.
                    "FECE 0001 " +
                    // Model Id 65230 is 1 registers.
                    "0000 " +

                    // Model Id 65232 at 41359.
                    "FED0 0014 " +
                    // Model Id 65232 is 20 registers.
                    "0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001 0003 0005 FFFF " +
                    "FFFF 0000 0000 0000 " +


                    // The End Model (i.e. the standard "No more blocks" marker)

                    // - BlockId == 0xFFFF == 'NaN'
                    // - BlockLen == 0
                    "FFFF 0000"
                )
                .build()
}
