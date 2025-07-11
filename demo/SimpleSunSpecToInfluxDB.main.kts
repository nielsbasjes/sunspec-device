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
@file:DependsOn("nl.basjes.sunspec:sunspec-device:0.7.0")
@file:DependsOn("nl.basjes.modbus:modbus-api-plc4j:0.11.0")
@file:DependsOn("com.influxdb:influxdb-client-java:7.3.0")
@file:DependsOn("org.apache.logging.log4j:log4j-to-slf4j:2.25.0")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.17")

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

import nl.basjes.sunspec.device.SunspecDevice
import java.time.Instant
import java.util.Timer
import java.util.TimerTask

val modbusIp             :String = "sunspec.iot.basjes.nl"
val modbusPort           :Int    = 502 // The default MODBUS TCP port
val modbusUnit           :Int    = 126 // SMA uses 126, other vendor can differ

val databaseUrl          :String? = "http://localhost:8181"
val databaseToken        :String? = "apiv3_Something"
val databaseOrg          :String? = "basjes" // "Some org"
val databaseBucket       :String? = "energy" // The influxDB commands call this the database, the V2 API calls it the bucket.
val databaseMeasurement  :String? = "solar"  // Sometimes called the database table

val connectionString = "modbus-tcp:tcp://$modbusIp:$modbusPort?unit-identifier=$modbusUnit"

print("Modbus: Connecting...")
ModbusDevicePlc4j(connectionString).use { modbusDevice ->
    println(" done")

    val sunSpec = SunspecDevice.generate(modbusDevice) ?: throw ModbusException("Unable to generate SunSpec device")
    sunSpec.connect(modbusDevice, 100)

    // To get information about what your device actually provides uncomment this next few lines
//    sunSpec.updateAll()
//    println("All possible fields that provide a useful value:\n${sunSpec.toTable(true)}")
//    exitProcess(0)

    if (
        databaseUrl         == null ||
        databaseToken       == null ||
        databaseOrg         == null ||
        databaseBucket      == null ||
        databaseMeasurement == null
    ) {
        println("No database, outputting to console")
        runLoop(sunSpec, null, "console")
        return
    } else {
        println("Connecting to database $databaseUrl")
        InfluxDBClientFactory
            .create(
                databaseUrl,
                databaseToken.toCharArray(),
                databaseOrg,
                databaseBucket,
            ).use { influxDBClient ->
                if (!influxDBClient.ping()) {
                    System.err.println("Error pinging server.")
                    return
                }
                runLoop(sunSpec, influxDBClient.writeApiBlocking, databaseMeasurement)
            }
    }
}

fun fieldOrFail(field:Field?) = field ?: throw ModbusException("The desired field does not exist")

fun runLoop(device: SchemaDevice, writeApi: WriteApiBlocking?, databaseMeasurement: String) {

    // These are always needed as tags for the data in InfluxDB
    val manufacturer = fieldOrFail(device["Model 1"]["Manufacturer"])
    val model        = fieldOrFail(device["Model 1"]["Model"])
    val version      = fieldOrFail(device["Model 1"]["Version"])
    val serialNumber = fieldOrFail(device["Model 1"]["Serial Number"])

    // Use these fields as Measurements towards InfluxDB
    val allFields = mutableMapOf<String, Field>()

    // These are always needed as tags for the data in InfluxDB
    allFields.put("AC_Current",               fieldOrFail(device["Model 101"]["AC Current"              ]))
    allFields.put("Phase_A_Current",          fieldOrFail(device["Model 101"]["AC Current Phase A"      ]))
    allFields.put("Phase_B_Current",          fieldOrFail(device["Model 101"]["AC Current Phase B"      ]))
    allFields.put("Phase_C_Current",          fieldOrFail(device["Model 101"]["AC Current Phase C"      ]))
    allFields.put("Phase_Voltage_AB",         fieldOrFail(device["Model 101"]["AC Voltage Phase AB"     ]))
    allFields.put("Phase_Voltage_BC",         fieldOrFail(device["Model 101"]["AC Voltage Phase BC"     ]))
    allFields.put("Phase_Voltage_CA",         fieldOrFail(device["Model 101"]["AC Voltage Phase CA"     ]))
    allFields.put("Phase_Voltage_AN",         fieldOrFail(device["Model 101"]["AC Voltage Phase AN"     ]))
    allFields.put("Phase_Voltage_BN",         fieldOrFail(device["Model 101"]["AC Voltage Phase BN"     ]))
    allFields.put("Phase_Voltage_CN",         fieldOrFail(device["Model 101"]["AC Voltage Phase CN"     ]))
    allFields.put("AC_Power",                 fieldOrFail(device["Model 101"]["AC Power"                ]))
    allFields.put("Line_Frequency",           fieldOrFail(device["Model 101"]["AC Line Frequency"       ]))
    allFields.put("AC_Apparent_Power",        fieldOrFail(device["Model 101"]["AC Apparent Power"       ]))
    allFields.put("AC_Reactive_Power",        fieldOrFail(device["Model 101"]["AC Reactive Power"       ]))
    allFields.put("AC_Power_Factor",          fieldOrFail(device["Model 101"]["AC Power Factor"         ]))
    allFields.put("AC_Energy",                fieldOrFail(device["Model 101"]["AC Energy"               ]))
    allFields.put("DC_Current",               fieldOrFail(device["Model 101"]["DC Current"              ]))
    allFields.put("DC_Voltage",               fieldOrFail(device["Model 101"]["DC Voltage"              ]))
    allFields.put("DC_Power",                 fieldOrFail(device["Model 101"]["DC Power"                ]))
    allFields.put("Cabinet_Temperature",      fieldOrFail(device["Model 101"]["Cabinet Temperature"     ]))
    allFields.put("Heat_Sink_Temperature",    fieldOrFail(device["Model 101"]["Heat Sink Temperature"   ]))
    allFields.put("Transformer_Temperature",  fieldOrFail(device["Model 101"]["Transformer Temperature" ]))
    allFields.put("Other_Temperature",        fieldOrFail(device["Model 101"]["Other Temperature"       ]))

    allFields.put("Module_0_Input_ID",        fieldOrFail(device["Model 160"]["Module_0_Input ID"       ]))
    allFields.put("Module_0_DC_Current",      fieldOrFail(device["Model 160"]["Module_0_DC Current"     ]))
    allFields.put("Module_0_DC_Voltage",      fieldOrFail(device["Model 160"]["Module_0_DC Voltage"     ]))
    allFields.put("Module_0_DC_Power",        fieldOrFail(device["Model 160"]["Module_0_DC Power"       ]))
    allFields.put("Module_1_Input_ID",        fieldOrFail(device["Model 160"]["Module_1_Input ID"       ]))
    allFields.put("Module_1_DC_Current",      fieldOrFail(device["Model 160"]["Module_1_DC Current"     ]))
    allFields.put("Module_1_DC_Voltage",      fieldOrFail(device["Model 160"]["Module_1_DC Voltage"     ]))
    allFields.put("Module_1_DC_Power",        fieldOrFail(device["Model 160"]["Module_1_DC Power"       ]))

    // Make sure we are going to fetch all the indicated fields.
    allFields.forEach { (_, field) -> field.need() }

    println("Starting read loop")

    val timer = Timer("Fetcher")
    val timerTask: AliveTimerTask =
        object : AliveTimerTask() {
            override fun run() {
                try {
                    // Update all fields
                    device.update()
                    val point: Point =
                        Point
                            .measurement(databaseMeasurement)
                            // We are rounding the timestamp to seconds to make the graphs in influxdb work a bit better
                            .time(Instant.ofEpochSecond(Instant.now().epochSecond), WritePrecision.S)
                            .addTag("Manufacturer",  manufacturer.stringValue ?: "Unknown")
                            .addTag("Model",         model       .stringValue ?: "Unknown")
                            .addTag("Version",       version     .stringValue ?: "Unknown")
                            .addTag("Serial Number", serialNumber.stringValue ?: "Unknown")
                    allFields.forEach {
                        (label, field) ->
                        when(field.returnType) {
                            DOUBLE ->        field.doubleValue                ?.let { value -> point.addField(label, value) }
                            LONG ->          field.longValue                  ?.let { value -> point.addField(label, value) }
                            STRING ->        field.stringValue                ?.let { value -> point.addField(label, value) }
                            STRINGLIST ->    field.stringListValue?.toString()?.let { value -> point.addField(label, value) }
                            UNKNOWN -> TODO()
                            BOOLEAN -> TODO()
                        }
                    }

                    if (writeApi == null) {
                        println(point.toLineProtocol())
                    } else {
                        println("Writing to influxDB: ${point.time}")
                        writeApi.writePoint(point)
                    }
                } catch (e: Exception) {
                    System.err.println("Stopping because of exception: ${e.message}")
                    cancel()
                }
            }
        }

    timer.scheduleAtFixedRate(timerTask, 0L, 1000L)

    while (timerTask.isAlive) {
//        println("Still alive")
        Thread.sleep(1000) // Check every second
    }
    println("Stopping")
    timer.cancel()
}

abstract class AliveTimerTask : TimerTask() {
    var isAlive: Boolean = true

    override fun cancel(): Boolean {
        this.isAlive = false
        return super.cancel()
    }
}
