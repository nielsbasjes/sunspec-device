/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.sunspec

import nl.basjes.modbus.device.api.AddressClass.HOLDING_REGISTER
import nl.basjes.modbus.device.memory.MockedModbusDevice
import nl.basjes.modbus.device.memory.MockedModbusDevice.Companion.builder
import nl.basjes.modbus.schema.get
import nl.basjes.modbus.schema.toTable
import nl.basjes.sunspec.device.SunspecDevice
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestRepeatBlocks {

    val device: MockedModbusDevice
        get() =
            builder()
                .withRegisters(
                    HOLDING_REGISTER,
                    40000,
                    """
                    # --------------------------------------
                    # SunS header
                    5375 6E53

                    # --------------------------------------
                    # Model 160 [Header @ hr:41104]: Multiple MPPT Inverter Extension Model
                    00A0 00F8

                    # Model 160 [Data @ hr:41106 - hr:41354]: 248 registers
                    FFFF FFFF 0001 8000 0000 0000 000C FFFF 0001 5056
                    3100 0000 0000 0000 0000 0000 0000 0080 2553 04CA
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0002 5056
                    3200 0000 0000 0000 0000 0000 0000 0080 24AD 04B6
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0003 5056
                    3300 0000 0000 0000 0000 0000 0000 0082 24CD 04C7
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0004 5056
                    3400 0000 0000 0000 0000 0000 0000 0080 24BD 04B3
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0005 5056
                    3500 0000 0000 0000 0000 0000 0000 0080 2473 04B5
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0006 5056
                    3600 0000 0000 0000 0000 0000 0000 007F 248A 04A8
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0007 5056
                    3700 0000 0000 0000 0000 0000 0000 0080 248E 04B6
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0008 5056
                    3800 0000 0000 0000 0000 0000 0000 0080 240F 049D
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 0009 5056
                    3900 0000 0000 0000 0000 0000 0000 0081 2465 04B8
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 000A 5056
                    3130 0000 0000 0000 0000 0000 0000 0081 2445 04AA
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 000B 5056
                    3131 0000 0000 0000 0000 0000 0000 0081 243A 04AD
                    0000 0000 FFFF FFFF 8000 0004 0000 0000 000C 5056
                    3132 0000 0000 0000 0000 0000 0000 0081 2463 04AE
                    0000 0000 FFFF FFFF 8000 0004 0000 0000

                    # --------------------------------------
                    # NO MORE MODELS
                    FFFF 0000
                    """.trimIndent(),
                ).build()

    @Test
    fun verifyRepeatingBlocks() {
        val sunSpecSchemaDevice = SunspecDevice.generate(device) ?: throw AssertionError("Should get a SchemaDevice")

        val fields = sunSpecSchemaDevice["Model 160"]?.fields
        assertNotNull(fields)

        // Ensure that all repeats of the block are present
        assertTrue(fields.size > 100)

        sunSpecSchemaDevice.updateAll()
        sunSpecSchemaDevice.createTestsUsingCurrentRealData()
        println(sunSpecSchemaDevice.toTable())
    }

}


