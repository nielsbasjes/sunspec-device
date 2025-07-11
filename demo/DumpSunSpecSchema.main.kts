#!/usr/bin/env kotlin
/*
 *
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
 *
 */

@file:DependsOn("nl.basjes.sunspec:sunspec-device:0.7.0")
@file:DependsOn("nl.basjes.modbus:modbus-api-plc4j:0.11.0")

import nl.basjes.modbus.device.api.MODBUS_STANDARD_TCP_PORT
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.plc4j.ModbusDevicePlc4j
import nl.basjes.modbus.schema.toYaml
import nl.basjes.sunspec.SUNSPEC_STANDARD_UNITID
import nl.basjes.sunspec.device.SunspecDevice

val modbusIp          = "sunspec.iot.basjes.nl"
val modbusPort        = MODBUS_STANDARD_TCP_PORT
val modbusUnit        = SUNSPEC_STANDARD_UNITID
val connectionString  = "modbus-tcp:tcp://${modbusIp}:${modbusPort}?unit-identifier=${modbusUnit}"

print("Modbus: Connecting...")
ModbusDevicePlc4j(connectionString).use { modbusDevice ->
    println(" done")

    // Create a Modbus Schema Device for this specific SunSpec device
    val sunSpec = SunspecDevice.generate(modbusDevice, "Demo", true) ?: throw ModbusException("Unable to generate SunSpec device")

    // Force all defined Fields to be fetched from the physical device
    sunSpec.updateAll()

    // Now create a test scenario from the currently available values
    sunSpec.createTestsUsingCurrentRealData()

    // And print the entire thing as a reusable schema yaml
    println("#-------------- BEGIN: MODBUS SCHEMA --------------")
    println(sunSpec.toYaml())
    println("#-------------- END: MODBUS SCHEMA ----------------")
}
