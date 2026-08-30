/*
 * Modbus Schema Toolkit
 * Copyright (C) 2019-2026 Niels Basjes
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

import nl.basjes.modbus.device.api.MODBUS_STANDARD_TCP_PORT
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.ModbusDeviceTcpConfig
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.j2mod.toModbusDeviceJ2Mod
import nl.basjes.modbus.device.plc4j.toModbusDevicePlc4j
import nl.basjes.modbus.device.testcases.sunspec.DeviceFimerPVSDated20240722
import nl.basjes.modbus.device.testcases.sunspec.DeviceSMASunnyBoy36Dated20230810
import nl.basjes.modbus.device.testcases.sunspec.DeviceSMASunnyBoy36Dated20250518
import nl.basjes.modbus.device.testcases.sunspec.DeviceSMASunnyBoy36Dated20250608
import nl.basjes.modbus.device.testcases.sunspec.DeviceSolarEdgeDated20191001
import nl.basjes.modbus.device.testcases.sunspec.EmulatedDER
import nl.basjes.modbus.schema.toTable
import nl.basjes.sunspec.device.SunspecDevice.generate
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Ignore
import kotlin.test.Test

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class TestSunspecDeviceGenerator {

    @Test
    @Order(2019)
    fun checkSolarEdge2019() {
        val device = DeviceSolarEdgeDated20191001.device
        device.logRequests = false
        dumpSunSpec(device)
    }

    @Test
    @Order(2023)
    fun checkSMASunnyBoy2023() {
        val device = DeviceSMASunnyBoy36Dated20230810.device
        device.logRequests = false
        dumpSunSpec(device)
    }

    @Test
    @Order(2024)
    fun checkFimerPVS2024() {
        val device = DeviceFimerPVSDated20240722.device
        device.logRequests = false
        dumpSunSpec(device)
    }

    @Test
    @Order(20250)
    fun checkSMASunnyBoy2025() {
        val device = DeviceSMASunnyBoy36Dated20250518.device
        device.logRequests = false
        dumpSunSpec(device)
    }

    @Test
    @Order(20251)
    fun checkSMASunnyBoy2025Night() {
        val device = DeviceSMASunnyBoy36Dated20250608.device
        device.logRequests = false
        dumpSunSpec(device)
    }

    @Test
    @Order(99999)
    fun checkSunSpecEmulatedDER() {
        val device = EmulatedDER.device
        device.logRequests = true
        dumpSunSpec(device)
    }

    val hostname: String = "sunspec.iot.basjes.nl"
    val port: Int = MODBUS_STANDARD_TCP_PORT
    val unitId: Int = SUNSPEC_STANDARD_UNITID

    @Ignore("Requires real device")
    @Test
    fun showRealSunSpecDevicePlc4J() {
        try {
            ModbusDeviceTcpConfig(hostname, port, unitId)
                .toModbusDevicePlc4j()
                .use {
                    dumpSunSpec(it)
                }
        } catch (e: Exception) {
            throw ModbusException("Unable to connect to the master", e)
        }
    }

    @Ignore("Requires real device")
    @Test
    fun showRealSunSpecDeviceJ2Mod() {
        try {
            ModbusDeviceTcpConfig(hostname, port, unitId)
                .toModbusDeviceJ2Mod()
                .use {
                    dumpSunSpec(it)
                }
        } catch (e: Exception) {
            throw ModbusException("Unable to connect to the master", e)
        }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()

        fun dumpSunSpec(modbusDevice: ModbusDevice) {
            // For SunSpec we generate the Schema based upon the SunSpec specification and
            // the exact capabilities of the device at hand.
            val schemaDevice = generate(modbusDevice, "Testing Testing")

            LOG.error("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv")

            checkNotNull(schemaDevice)
            schemaDevice.updateAll()

            LOG.error("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")

//            LOG.warn("The schema device we now have: {}\n{}", schemaDevice.description, schemaDevice.toTable(false))

            LOG.error("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv")
            schemaDevice.createTestsUsingCurrentRealData()

            //        LOG.warn("\n{}", LoaderKt.toSchema(schemaDevice.getTests().get(0).getRegisterBlocks().get(0)));
//            LOG.warn("\n{}", schemaDevice.toYaml())
//            LOG.warn("\n{}", schemaDevice.tests.first().registerBlocks.first().toSchema())

            LOG.error("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")

            LOG.info("\n{}", schemaDevice.toTable())
        }
    }
}
