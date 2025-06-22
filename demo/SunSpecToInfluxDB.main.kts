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

@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")
@file:DependsOn("nl.basjes.sunspec:sunspec-device:0.5.0")
@file:DependsOn("nl.basjes.modbus:modbus-api-plc4j:0.9.0")
@file:DependsOn("com.influxdb:influxdb-client-java:7.3.0")
@file:DependsOn("args4j:args4j:2.37")
@file:DependsOn("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.0")
@file:DependsOn("org.apache.logging.log4j:log4j-core:2.25.0")

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.WriteApiBlocking
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.plc4j.ModbusDevicePlc4j
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.ReturnType.BOOLEAN
import nl.basjes.modbus.schema.ReturnType.DOUBLE
import nl.basjes.modbus.schema.ReturnType.LONG
import nl.basjes.modbus.schema.ReturnType.STRING
import nl.basjes.modbus.schema.ReturnType.STRINGLIST
import nl.basjes.modbus.schema.ReturnType.UNKNOWN
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.get
import nl.basjes.sunspec.SUNSPEC_STANDARD_UNITID
import nl.basjes.sunspec.device.SunspecDevice
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.Timer
import java.util.TimerTask

private val logger = LoggerFactory.getLogger("SunSpec To InfluxDB")

main(*args)

fun main(vararg args: String) {
    val commandlineOptions = CommandOptions()
    val parser = CmdLineParser(commandlineOptions)
    try {
        println("Parsing arguments")
        parser.parseArgument(*args)
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

    val modbusIp       = commandlineOptions.modbusIp
    val modbusPort     = commandlineOptions.modbusPort
    val modbusUnit     = commandlineOptions.modbusUnit
    val databaseUrl    = commandlineOptions.databaseUrl
    val databaseName   = commandlineOptions.databaseName
    val databaseToken  = commandlineOptions.databaseToken
    val databaseOrg    = commandlineOptions.databaseOrg
    val databaseBucket = commandlineOptions.databaseBucket

    val connectionString = "modbus-tcp:tcp://$modbusIp:$modbusPort?unit-identifier=$modbusUnit"

    print("Modbus: Connecting...")
    ModbusDevicePlc4j(connectionString).use { modbusDevice ->
        println(" done")

        val sunSpec = SunspecDevice.generate(modbusDevice) ?: throw ModbusException("Unable to generate SunSpec device")

        sunSpec.connect(modbusDevice, 100)

        if (
            databaseUrl     == null ||
            databaseToken   == null ||
            databaseName    == null ||
            databaseOrg     == null ||
            databaseBucket  == null
        ) {
            logger.info("No database, outputting to console")
            runLoop(sunSpec, null, "console")
            return
        }

        logger.info("Connecting to database {}", commandlineOptions.databaseUrl)
        InfluxDBClientFactory
            .create(
                databaseUrl,
                databaseToken.toCharArray(),
                databaseOrg,
                databaseBucket,
            ).use { influxDBClient ->
                if (!influxDBClient.ping()) {
                    logger.error("Error pinging server.")
                    return
                }
                runLoop(sunSpec, influxDBClient.writeApiBlocking, databaseName)
            }
    }
}

@Throws(InterruptedException::class)
private fun runLoop(device: SchemaDevice, writeApi: WriteApiBlocking?, databaseName: String) {
    val model1            = device["Model 1"]   ?: throw ModbusException("Model 1 is null")
    val model101          = device["Model 101"] ?: throw ModbusException("Model 101 is null")
    val model122          = device["Model 122"] ?: throw ModbusException("Model 122 is null")
    val model160          = device["Model 160"] ?: throw ModbusException("Model 160 is null")

    val allFields = mutableListOf<Field>()
//    allFields.addAll(model_1.fields)
    model1.needAll()
    allFields.addAll(model101.fields.filter { !it.isSystem })
    allFields.addAll(model122.fields.filter { !it.isSystem })
    allFields.addAll(model160.fields.filter { !it.isSystem })

    allFields.forEach { it.need() }

    logger.info("Starting read loop")

    val timer = Timer("Fetcher")
    val timerTask: AliveTimerTask =
        object : AliveTimerTask() {
            override fun run() {
//                logger.info("tick (alive={})", this.isAlive)
                try {
                    // Update all fields
                    device.update()
                    val point: Point =
                        Point
                            .measurement(databaseName)
                            // We are rounding the timestamp to seconds to make the graphs in influxdb work a bit better
                            .time(Instant.ofEpochSecond(Instant.now().epochSecond), WritePrecision.S)
                            .addTag("Manufacturer",  device["Model 1"]["Mn"]?.stringValue ?: "Unknown")
                            .addTag("Model",         device["Model 1"]["Md"]?.stringValue ?: "Unknown")
                            .addTag("Version",       device["Model 1"]["Vr"]?.stringValue ?: "Unknown")
                            .addTag("Serial Number", device["Model 1"]["SN"]?.stringValue ?: "Unknown")
                    allFields.forEach {
                        val label = it.id
                        when(it.returnType) {
                            DOUBLE ->        it.doubleValue                ?.let { value -> point.addField(label, value) }
                            LONG ->          it.longValue                  ?.let { value -> point.addField(label, value) }
                            STRING ->        it.stringValue                ?.let { value -> point.addField(label, value) }
                            STRINGLIST ->    it.stringListValue?.toString()?.let { value -> point.addField(label, value) }
                            UNKNOWN -> TODO()
                            BOOLEAN -> TODO()
                        }
                    }

                    if (writeApi == null) {
                        logger.info("{}", point.toLineProtocol())
                    } else {
                        logger.info("Writing to influxDB")
                        writeApi.writePoint(point)
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Stopping because of exception: {}",
                        e.message,
                    )
                    cancel()
                }
            }
        }

    timer.scheduleAtFixedRate(timerTask, 0L, 1000L)

    while (timerTask.isAlive) {
//        logger.info("Still alive")
        Thread.sleep(1000) // Check every second
    }
    logger.info("Stopping")
    timer.cancel()
}

abstract class AliveTimerTask : TimerTask() {
    var isAlive: Boolean = true

    override fun cancel(): Boolean {
        this.isAlive = false
        return super.cancel()
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

    @Option(
        name = "-databaseUrl",
        usage = "The URL of the InfluxDB database",
        depends = ["-databaseUrl", "-databaseName", "-databaseToken", "-databaseOrg", "-databaseBucket"],
    )
    var databaseUrl: String? = null

    @Option(
        name = "-databaseName",
        usage = "The NAME of the InfluxDB database",
        depends = ["-databaseUrl", "-databaseName", "-databaseToken", "-databaseOrg", "-databaseBucket"],
    )
    var databaseName: String? = null

    @Option(
        name = "-databaseToken",
        usage = "The API token of the InfluxDB database",
        depends = ["-databaseUrl", "-databaseName", "-databaseToken", "-databaseOrg", "-databaseBucket"],
    )
    var databaseToken: String? = null

    @Option(
        name = "-databaseOrg",
        usage = "The Organization of the InfluxDB database",
        depends = ["-databaseUrl", "-databaseName", "-databaseToken", "-databaseOrg", "-databaseBucket"],
    )
    var databaseOrg: String? = null

    @Option(
        name = "-databaseBucket",
        usage = "The Bucket of the InfluxDB database",
        depends = ["-databaseUrl", "-databaseName", "-databaseToken", "-databaseOrg", "-databaseBucket"],
    )
    var databaseBucket: String? = null
    override fun toString(): String {
        return "" +
            "- Modbus Ip       = $modbusIp\n" +
            "- Modbus Port     = $modbusPort\n" +
            "- Modbus Unit     = $modbusUnit\n" +
            "- Database Url    = $databaseUrl\n" +
            "- Database Name   = $databaseName\n" +
            "- Database Token  = $databaseToken\n" +
            "- Database Org    = $databaseOrg\n" +
            "- Database Bucket = $databaseBucket\n"
    }
}
