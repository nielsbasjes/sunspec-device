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

import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import nl.basjes.modbus.device.api.MODBUS_STANDARD_TCP_PORT
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.j2mod.ModbusDeviceJ2Mod
import nl.basjes.modbus.device.plc4j.ModbusDevicePlc4j
import nl.basjes.modbus.schema.toYaml
import nl.basjes.sunspec.device.SunspecDevice.generate
import nl.basjes.sunspec.devices.DeviceOther
import nl.basjes.sunspec.devices.DeviceSMASunnyBoy36
import nl.basjes.sunspec.devices.DeviceSMASunnyBoy36_20250410
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.test.Ignore
import kotlin.test.Test

internal class TestSunspecDeviceGenerator {
    @Test
    @Throws(ModbusException::class)
    fun checkSunSpecDump() {
        dumpSunSpec(DeviceSMASunnyBoy36.device)
    }

    @Test
    @Throws(ModbusException::class)
    fun checkSunSpecDump2025() {
        dumpSunSpec(DeviceSMASunnyBoy36_20250410.device)
    }

    @Test
    @Throws(ModbusException::class)
    fun checkSunSpecDumpOther() {
        dumpSunSpec(DeviceOther.device)
    }

    val hostname: String = "sunspec.iot.basjes.nl"
    val port: Int = MODBUS_STANDARD_TCP_PORT
    val unitId: Int = SUNSPEC_STANDARD_UNITID

    @Ignore("Requires real device")
    @Test
    @Throws(ModbusException::class)
    fun showRealSunSpecDevicePlc4J() {
        val connectionString = "modbus-tcp:tcp://$hostname:$port?unit-identifier=$unitId"
        try {
            ModbusDevicePlc4j(connectionString).use { modbusDevice ->
                dumpSunSpec(modbusDevice)
            }
        } catch (e: Exception) {
            throw ModbusException("Unable to connect to the master", e)
        }
    }

    @Ignore("Requires real device")
    @Test
    @Throws(ModbusException::class)
    fun showRealSunSpecDeviceJ2Mod() {
        val master: AbstractModbusMaster = ModbusTCPMaster(hostname, port)
        try {
            master.connect()
            ModbusDeviceJ2Mod(master, unitId).use { modbusDevice ->
                dumpSunSpec(modbusDevice)
            }
        } catch (e: Exception) {
            throw ModbusException("Unable to connect to the master", e)
        }
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()

        @Throws(ModbusException::class)
        fun dumpSunSpec(modbusDevice: ModbusDevice) {
            // For SunSpec we generate the Schema based upon the SunSpec specification and
            // the exact capabilities of the device at hand.
            val schemaDevice = generate(modbusDevice, "")

            LOG.error("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv")

            checkNotNull(schemaDevice)
            schemaDevice.updateAll()

            LOG.error("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")

//            LOG.warn("The schema device we now have: {}\n{}", schemaDevice.description, schemaDevice.toTable(false))

            LOG.error("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv")
            schemaDevice.createTestsUsingCurrentRealData()

            //        LOG.warn("\n{}", LoaderKt.toSchema(schemaDevice.getTests().get(0).getRegisterBlocks().get(0)));
            LOG.warn("\n{}", schemaDevice.toYaml())
//            LOG.warn("\n{}", schemaDevice.tests.first().registerBlocks.first().toSchema())

            LOG.error("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")

            //        LOG.info("\n{}", schemaDevice.toTable(false));
        }
    }
}
