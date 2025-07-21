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
@file:DependsOn("nl.basjes.modbus:modbus-api-j2mod:0.12.0")
@file:DependsOn("org.json:json:20250517")
@file:DependsOn("de.kempmobil.ktor.mqtt:mqtt-core-jvm:0.6.2")
@file:DependsOn("de.kempmobil.ktor.mqtt:mqtt-client-jvm:0.6.2")
@file:DependsOn("org.apache.logging.log4j:log4j-to-slf4j:2.25.1")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.17")

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import de.kempmobil.ktor.mqtt.MqttClient
import de.kempmobil.ktor.mqtt.PublishRequest
import de.kempmobil.ktor.mqtt.QoS
import de.kempmobil.ktor.mqtt.TimeoutException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.j2mod.ModbusDeviceJ2Mod
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
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.hours

val modbusHost           :String = "sunspec.iot.basjes.nl"
val modbusPort           :Int    = 502 // The default MODBUS TCP port
val modbusUnit           :Int    = 126 // SMA uses 126, other vendors can differ

val mqttBrokerHost      :String? = null //"localhost"
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
    allFields.add(device.wantField("Model 1",   "Manufacturer"              ))
    allFields.add(device.wantField("Model 1",   "Model"                     ))
    allFields.add(device.wantField("Model 1",   "Version"                   ))
    allFields.add(device.wantField("Model 1",   "Serial Number"             ))

    allFields.add(device.wantField("Model 101", "AC Current"                ))
    allFields.add(device.wantField("Model 101", "AC Current Phase A"        ))
    allFields.add(device.wantField("Model 101", "AC Current Phase B"        ))
    allFields.add(device.wantField("Model 101", "AC Current Phase C"        ))
    allFields.add(device.wantField("Model 101", "AC Voltage Phase AB"       ))
    allFields.add(device.wantField("Model 101", "AC Voltage Phase BC"       ))
    allFields.add(device.wantField("Model 101", "AC Voltage Phase CA"       ))
    allFields.add(device.wantField("Model 101", "AC Voltage Phase AN"       ))
    allFields.add(device.wantField("Model 101", "AC Voltage Phase BN"       ))
    allFields.add(device.wantField("Model 101", "AC Voltage Phase CN"       ))
    allFields.add(device.wantField("Model 101", "AC Power"                  ))
    allFields.add(device.wantField("Model 101", "AC Line Frequency"         ))
    allFields.add(device.wantField("Model 101", "AC Apparent Power"         ))
    allFields.add(device.wantField("Model 101", "AC Reactive Power"         ))
    allFields.add(device.wantField("Model 101", "AC Power Factor"           ))
    allFields.add(device.wantField("Model 101", "AC Energy"                 ))
    allFields.add(device.wantField("Model 101", "DC Current"                ))
    allFields.add(device.wantField("Model 101", "DC Voltage"                ))
    allFields.add(device.wantField("Model 101", "DC Power"                  ))
    allFields.add(device.wantField("Model 101", "Cabinet Temperature"       ))
    allFields.add(device.wantField("Model 101", "Heat Sink Temperature"     ))
    allFields.add(device.wantField("Model 101", "Transformer Temperature"   ))
    allFields.add(device.wantField("Model 101", "Other Temperature"         ))

    allFields.add(device.wantField("Model 160", "Module_0_Input ID"         ))
    allFields.add(device.wantField("Model 160", "Module_0_DC Current"       ))
    allFields.add(device.wantField("Model 160", "Module_0_DC Voltage"       ))
    allFields.add(device.wantField("Model 160", "Module_0_DC Power"         ))
    allFields.add(device.wantField("Model 160", "Module_1_Input ID"         ))
    allFields.add(device.wantField("Model 160", "Module_1_DC Current"       ))
    allFields.add(device.wantField("Model 160", "Module_1_DC Voltage"       ))
    allFields.add(device.wantField("Model 160", "Module_1_DC Power"         ))
    return allFields
}

// ===================================================================================================


print("Modbus: Connecting...")
val modbusMaster = ModbusTCPMaster(modbusHost, modbusPort)
modbusMaster.connect()
ModbusDeviceJ2Mod(modbusMaster, modbusUnit). use { modbusDevice ->
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
    runBlocking {
        mqttClient.disconnect()
    }
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

    val interval = 1000L

    println("Starting read loop")

    while (true) {
        try {
            runBlocking {
                // Wait until the current time is a multiple of the configured interval
                val now = Instant.now().toEpochMilli()
                val sleepTime = (((now / interval) + 1) * interval) - now
                if (sleepTime > 0) delay(sleepTime)
            }
            // Update all fields
            val startUpdate = Instant.now()
            print("Doing update at: $startUpdate .. ")
                    device.update()
            val finishUpdate = Instant.now()
            println("done in ${finishUpdate.toEpochMilli() -  startUpdate.toEpochMilli()} milliseconds.")

            val result = JSONObject()

            // We are rounding the timestamp to seconds to make the graphs in influxdb work a bit better
            val now = Instant.now()
            result.put("timestamp", now.toEpochMilli())
            result.put("timestampString", now)

            allFields.forEach {
                val jsonFieldName = it.jsonFieldName()
                when(it.returnType) {
                    DOUBLE     -> result.put(jsonFieldName, it.doubleValue     ?: 0.0)
                    LONG       -> result.put(jsonFieldName, it.longValue       ?: 0)
                    STRING     -> result.put(jsonFieldName, it.stringValue     ?: "")
                    STRINGLIST -> result.put(jsonFieldName, it.stringListValue ?: listOf<String>())
                    BOOLEAN    -> result.put(jsonFieldName, it.booleanValue     ?: "")
                    UNKNOWN    -> TODO()
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
            System.err.println("Got a TimeoutException from MQTT (ignoring 1): $e --> ${e.message} ==> ${e.printStackTrace()}")
        } catch (e: java.util.concurrent.TimeoutException) {
            System.err.println("Got a java.util.concurrent.TimeoutException (ignoring 2): $e --> ${e.message} ==> ${e.printStackTrace()}")
                } catch (e: Exception) {
            System.err.println("Got an exception: $e --> ${e.message} ==> ${e.printStackTrace()}")
            println("Stopping")
            return
        }
    }

}

fun Field.jsonFieldName() = "${this.block.id} ${this.id}".replace(Regex("[^a-zA-Z0-9_]"), "_")

fun showAllFieldsWithUsableValues(schemaDevice: SchemaDevice) {
    schemaDevice.updateAll()
    println("All possible fields that provide a useful value:\n${schemaDevice.toTable(false)}")
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
    val manufacturer = device.wantField("Model 1", "Manufacturer")
    val model        = device.wantField("Model 1", "Model")
    val version      = device.wantField("Model 1", "Version")
    val serialNumber = device.wantField("Model 1", "Serial Number")

    val configFields = mutableListOf<Field>()
    configFields.add(manufacturer)
    configFields.add(model)
    configFields.add(version)
    configFields.add(serialNumber)
    configFields.addAll(allFields)
    configFields.distinct()

    configFields.forEach { it.need() }
    device.update(1000)
    configFields.forEach { it.unNeed() }

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
                        // Make the name of fields in repeating blocks look better
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
