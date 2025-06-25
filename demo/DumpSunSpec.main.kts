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

@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
@file:DependsOn("nl.basjes.sunspec:sunspec-device:0.6.0")
@file:DependsOn("nl.basjes.modbus:modbus-api-plc4j:0.10.0")
@file:DependsOn("args4j:args4j:2.37")
@file:DependsOn("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.0")
@file:DependsOn("org.apache.logging.log4j:log4j-core:2.25.0")

import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.plc4j.ModbusDevicePlc4j
import nl.basjes.modbus.schema.toTable
import nl.basjes.modbus.schema.toYaml
import nl.basjes.sunspec.SUNSPEC_STANDARD_UNITID
import nl.basjes.sunspec.device.SunspecDevice
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option

main(*args)

fun main(vararg args: String) {
    val commandlineOptions = CommandOptions()
    val parser = CmdLineParser(commandlineOptions)
    try {
        println("Parsing arguments")
        parser.parseArgument(*args)

        if (!commandlineOptions.outputYaml && !commandlineOptions.outputTable) {
            throw CmdLineException(parser, "Need at least one output selected.", null)
        }

        println("Successfully parsed")
        println("Provided arguments:\n${commandlineOptions}")
    } catch (cle: CmdLineException) {
        println("Provided arguments:\n${commandlineOptions}")
        System.err.println("Errors: ${cle.message}")
        System.err.println()
        System.err.println("Usage: java jar <jar containing this class> <options>")
        parser.printUsage(System.err)
        return
    }

    val connectionString = "modbus-tcp:tcp://${commandlineOptions.modbusIp}:${commandlineOptions.modbusPort}?unit-identifier=${commandlineOptions.modbusUnit}"

    print("Modbus: Connecting...")
    ModbusDevicePlc4j(connectionString).use { modbusDevice ->
        println(" done")

        val sunSpec = SunspecDevice.generate(modbusDevice, "Demo", true) ?: throw ModbusException("Unable to generate SunSpec device")

        // Get everything
        sunSpec.updateAll()

        // Convert all available values into a test scenario
        sunSpec.createTestsUsingCurrentRealData()

        if (commandlineOptions.outputYaml) {
            println("#-------------- BEGIN: MODBUS SCHEMA --------------")
            println(sunSpec.toYaml())
            println("#-------------- END: MODBUS SCHEMA ----------------")
        }

        if (commandlineOptions.outputTable) {
            println(sunSpec.toTable(true))
        }
    }
}


@SuppressWarnings("CanBeFinal")
private class CommandOptions {
    @Option(name = "-ip", usage = "The modbus ip address of the device from which to read", required = true)
    var modbusIp: String? = null

    @Option(name = "-port", usage = "The modbus tcp port of the device from which to read (default: 502)")
    var modbusPort: Int = 502

    @Option(name = "-unit", usage = "The modbus unit id of the device from which to read (default: 126)")
    var modbusUnit: Int = SUNSPEC_STANDARD_UNITID

    @Option(name = "-yaml", usage = "Output the data as a Schema Yaml with tests")
    var outputYaml: Boolean = false

    @Option(name = "-table", usage = "Output the data as textual table.")
    var outputTable: Boolean = false

    override fun toString(): String {
        return "" +
            "- Modbus IP       = $modbusIp\n" +
            "- Modbus TCP Port = $modbusPort\n" +
            "- Modbus Unit     = $modbusUnit\n"
    }
}
