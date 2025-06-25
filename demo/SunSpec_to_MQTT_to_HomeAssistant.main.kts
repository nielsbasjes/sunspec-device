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

@file:DependsOn("nl.basjes.sunspec:sunspec-device:0.6.0")
@file:DependsOn("nl.basjes.modbus:modbus-api-plc4j:0.10.0")
@file:DependsOn("org.json:json:20250517")
@file:DependsOn("de.kempmobil.ktor.mqtt:mqtt-core-jvm:0.6.1")
@file:DependsOn("de.kempmobil.ktor.mqtt:mqtt-client-jvm:0.6.1")
@file:DependsOn("org.apache.logging.log4j:log4j-to-slf4j:2.25.0")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.17")

import de.kempmobil.ktor.mqtt.MqttClient
import de.kempmobil.ktor.mqtt.PublishRequest
import de.kempmobil.ktor.mqtt.QoS
import de.kempmobil.ktor.mqtt.TimeoutException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
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
import nl.basjes.modbus.schema.toTable

import nl.basjes.sunspec.device.SunspecDevice
import org.json.JSONObject
import java.time.Instant
import java.util.Timer
import java.util.TimerTask
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.hours

val modbusIp             :String = "sunspec.iot.basjes.nl"
val modbusPort           :Int    = 502 // The default MODBUS TCP port
val modbusUnit           :Int    = 126 // SMA uses 126, other vendors can differ

val mqttBrokerHost      :String? = "localhost"
val mqttBrokerPort      :Int     = 1883
val mqttTopic           :String? = "energy/solar"

//val mqttUser            :String? = null TODO: If you need it you must change the code a bit.
//val mqttPassword        :String? = null TODO: If you need it you must change the code a bit.

// This is useful if you want to set a different hostname (shown in the HA Gauge label and such)
val homeAssistantDeviceName: String? = null

fun allTheFieldsIWant(device: SchemaDevice): List<Field> {
    // Use these fields as Measurements
    val allFields = mutableListOf<Field>()

    // Brute force want all fields
//    device
//        .fields
//        .filter { !it.isSystem }
//        .forEach { allFields.add(it) }


    // Or a smarter more efficient selection

    // These are always needed
    allFields.add(device.wantField("Model 1", "Mn"              ))
    allFields.add(device.wantField("Model 1", "Md"              ))
    allFields.add(device.wantField("Model 1", "Vr"              ))
    allFields.add(device.wantField("Model 1", "SN"              ))

    allFields.add(device.wantField("Model 101", "A"             ))
    allFields.add(device.wantField("Model 101", "AphA"          ))
    allFields.add(device.wantField("Model 101", "AphB"          ))
    allFields.add(device.wantField("Model 101", "AphC"          ))
    allFields.add(device.wantField("Model 101", "PPVphAB"       ))
    allFields.add(device.wantField("Model 101", "PPVphBC"       ))
    allFields.add(device.wantField("Model 101", "PPVphCA"       ))
    allFields.add(device.wantField("Model 101", "PhVphA"        ))
    allFields.add(device.wantField("Model 101", "PhVphB"        ))
    allFields.add(device.wantField("Model 101", "PhVphC"        ))
    allFields.add(device.wantField("Model 101", "W"             ))
    allFields.add(device.wantField("Model 101", "Hz"            ))
    allFields.add(device.wantField("Model 101", "VA"            ))
    allFields.add(device.wantField("Model 101", "VAr"           ))
    allFields.add(device.wantField("Model 101", "PF"            ))
    allFields.add(device.wantField("Model 101", "WH"            ))
    allFields.add(device.wantField("Model 101", "DCA"           ))
    allFields.add(device.wantField("Model 101", "DCV"           ))
    allFields.add(device.wantField("Model 101", "DCW"           ))
    allFields.add(device.wantField("Model 101", "TmpCab"        ))
    allFields.add(device.wantField("Model 101", "TmpSnk"        ))
    allFields.add(device.wantField("Model 101", "TmpTrns"       ))
    allFields.add(device.wantField("Model 101", "TmpOt"         ))

    allFields.add(device.wantField("Model 160", "Module_0_ID"   ))
    allFields.add(device.wantField("Model 160", "Module_0_DCA"  ))
    allFields.add(device.wantField("Model 160", "Module_0_DCV"  ))
    allFields.add(device.wantField("Model 160", "Module_0_DCW"  ))
    allFields.add(device.wantField("Model 160", "Module_1_ID"   ))
    allFields.add(device.wantField("Model 160", "Module_1_DCA"  ))
    allFields.add(device.wantField("Model 160", "Module_1_DCV"  ))
    allFields.add(device.wantField("Model 160", "Module_1_DCW"  ))
    return allFields
}

// ===================================================================================================


val connectionString = "modbus-tcp:tcp://$modbusIp:$modbusPort?unit-identifier=$modbusUnit"

print("Modbus: Connecting...")
ModbusDevicePlc4j(connectionString).use { modbusDevice ->
    println(" done")

    // Connect to the SunSpec device and generate a SchemaDevice with all supported SunSpec Models and Fields.
    val sunSpec = SunspecDevice.generate(modbusDevice) ?: throw ModbusException("Unable to generate SunSpec device")
    sunSpec.connect(modbusDevice, 100)

    // To get information about what your every field your device CAN actually provide: uncomment this next line
//    showAllFieldsWithUsableValues(sunSpec)

    // If no broker is specified the output is sent to the console (useful for testing)
    if (mqttBrokerHost == null || mqttTopic == null) {
        println("No database, outputting to console")
        runLoop(sunSpec, null, "console")
        return
    }

    // If we do have the broker and topic the data is sent to the MQTT broker
    println("Connecting to mqtt $mqttBrokerHost:$mqttBrokerPort")
    // TODO: Username + password ...
    val mqttClient = MqttClient(mqttBrokerHost, mqttBrokerPort) {}
    runBlocking {
        mqttClient.connect().onFailure { throw IOException("Connection failed: $it") }
    }
    runLoop(sunSpec, mqttClient, mqttTopic)

}

fun SchemaDevice.wantField(blockId:String, fieldId:String) =
    this[blockId][fieldId] ?: throw ModbusException("The desired field \"${fieldId}\" in the block \"${blockId}\" does not exist")

@OptIn(DelicateCoroutinesApi::class)
fun runLoop(device: SchemaDevice, mqttClient: MqttClient?, mqttTopic: String) {

    // Use these fields as Measurements
    val allFields = allTheFieldsIWant(device)

    // We need all the field we want.
    allFields.forEach { it.need() }

    println("Trying to get ${allFields.size} fields.")
    allFields.forEach { println(it) }
    device.update()
    println("Found ${allFields.filter{ it.value != null }.size} fields to have a value.")
    println(device.toTable(onlyUseFullFields = true))

    // ----------------------------------------------------------------------------------------

    // OPTIONAL: Generate the config for Home Assistant
    generateHomeAssistantConfig(device, allFields)

    // ----------------------------------------------------------------------------------------

    println("Starting read loop")

    val timer = Timer("Fetcher")
    val timerTask: AliveTimerTask =
        object : AliveTimerTask() {
            override fun run() {
                try {
                    // Update all fields
                    device.update()

                    val result = JSONObject()

                    // We are rounding the timestamp to seconds to make the graphs in influxdb work a bit better
                    val now = Instant.now()
                    result.put("timestamp", now.toEpochMilli())

                    allFields.forEach {
                        val jsonFieldName = it.jsonFieldName()
                        when(it.returnType) {
                            DOUBLE ->        result.put(jsonFieldName, it.doubleValue     )
                            LONG ->          result.put(jsonFieldName, it.longValue       )
                            STRING ->        result.put(jsonFieldName, it.stringValue     )
                            STRINGLIST ->    result.put(jsonFieldName, it.stringListValue )
                            UNKNOWN -> TODO()
                            BOOLEAN -> TODO()
                        }
                    }

                    if (mqttClient == null) {
                        println(result)
                    } else {
                        GlobalScope.launch {
                            print("Writing to MQTT: $now .. ")
                            mqttClient
                                .publish(PublishRequest(mqttTopic) {
                                    desiredQoS = QoS.AT_LEAST_ONCE
                                    messageExpiryInterval = 12.hours
                                    payload(result.toString())
                                })
                                .onSuccess { println("COMPLETED") }
                                .onFailure { println("FAILED with $it") }
                        }
                    }

                } catch (e: TimeoutException) {
                    System.err.println("Got a TimeoutException from MQTT (ignoring): ${e.message}")
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

fun Field.jsonFieldName() = "${this.block.id} ${this.id}".replace(Regex("[^a-zA-Z0-9_]"), "_")

fun showAllFieldsWithUsableValues(schemaDevice: SchemaDevice) {
    schemaDevice.updateAll()
    println("All possible fields that provide a useful value:\n${schemaDevice.toTable(true)}")
    exitProcess(0)
}

fun generateHomeAssistantConfig(
    device: SchemaDevice,
    allFields: List<Field>,
) {
    // Generate the config for Home Assistant
    // We first fetch all fields that have been asked for
    // and then for all fields that actually have a value and are not marked as "system"
    // a config for Home Assistant is generated (must be copied to the Home Assistant setup manually)

    // These are always needed
    val manufacturer = device.wantField("Model 1", "Mn")
    val model        = device.wantField("Model 1", "Md")
    val version      = device.wantField("Model 1", "Vr")
    val serialNumber = device.wantField("Model 1", "SN")

    val configFields = mutableListOf<Field>()
    configFields.add(manufacturer)
    configFields.add(model)
    configFields.add(version)
    configFields.add(serialNumber)
    configFields.addAll(allFields)
    configFields.distinct()

    allFields.forEach { it.need() }
    device.update(1000)
    allFields.forEach { it.unNeed() }

    println("""
# ----------------------------------------------------------------------------------------
# HomeAssistant definitions for all requested fields
mqtt:
  sensor:
    """.trimIndent())
    allFields
        .forEach {
            if (!it.isSystem && it.value != null) {
                val jsonFieldName = it.jsonFieldName()
                // Building a name that looks 'ok' in Home Assistant
                var name = if (it.block.shortDescription.isNullOrBlank())
                    it.block.id
                else
                    it.block.shortDescription
                name += " - "
                if(it.id.contains("_")) {
                    name += it.id
                        .replace(Regex("_([0-9]+)_"), "[$1].")
                        .replace("_", ".")
                    if (!it.id.endsWith(it.shortDescription)) {
                        name += " - ${it.shortDescription}"
                    }
                } else {
                    name += it.shortDescription
                }

                println(
                    """
    - name: "$name"
      unique_id: "SunSpec-${manufacturer.stringValue}-${serialNumber.stringValue}-$jsonFieldName"
      state_topic: "$mqttTopic"
      value_template: "{{ value_json.$jsonFieldName ${if (it.returnType == DOUBLE) "| round(4, default=0)" else "" }}}"${if(it.unit.isNotBlank()) """
      unit_of_measurement: "${it.unit}"""" else ""}
      icon: mdi:solar-panel
      device:
        name: "${if(homeAssistantDeviceName.isNullOrBlank()) "${manufacturer.stringValue} ${model.stringValue}" else homeAssistantDeviceName}"
        manufacturer: "${manufacturer.stringValue}"
        model: "${model.stringValue}"
        identifiers: "${serialNumber.stringValue}"
        sw_version: "${version.stringValue}"
      """
                )
            }
        }
    println("""
# ----------------------------------------------------------------------------------------
""")

//    exitProcess(0)
}
