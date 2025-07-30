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
@file:DependsOn("nl.basjes.sunspec:sunspec-device:0.7.3")
@file:DependsOn("nl.basjes.modbus:modbus-api-j2mod:0.14.0")
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

val modbusHost           :String? = null // "sunspec.iot.basjes.nl"
val modbusPort           :Int    = 502 // The default MODBUS TCP port
val modbusUnit           :Int    = 126 // SMA uses 126, other vendors can differ

val mqttBrokerHost      :String? = null //"localhost"
val mqttBrokerPort      :Int     = 1883
val mqttTopic           :String? = "energy/solar"

// This is useful if you want to set a different hostname (shown in the HA Gauge label and such)
val homeAssistantDeviceName: String? = null

/** @return the relation between each field and the used name at the Json side */
fun allTheFieldsIWant(device: SchemaDevice): MutableList<Pair<String, Field>> {
    // Use these fields as Measurements
    val allFields = mutableListOf<Pair<String, Field>>()

    // From Model 1 the "Manufacturer", "Model", "Version" and "Serial Number" must ALWAYS be added
    allFields.add("Model_1_Manufacturer"    to device.wantField("Model 1",   "Manufacturer"              ))
    allFields.add("Model_1_Model"           to device.wantField("Model 1",   "Model"                     ))
    allFields.add("Model_1_Version"         to device.wantField("Model 1",   "Version"                   ))
    allFields.add("Model_1_Serial_Number"   to device.wantField("Model 1",   "Serial Number"             ))

    // You now need to add all fields you want below here
    // Example:
    //    This links the json field name going to MQTT to the Modbus Schema field from the device
    //    allFields.add("Model_101_AC_Current" to device.wantField("Model 101", "AC Current" ))
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // FIXME: Add all fields you need here (this script will produce all possible output if this is empty).
    //        You should really only include the fields you want to avoid performance problems.
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    return allFields
}

// ===================================================================================================

if (modbusHost == null) {
    println("Step 1: Edit the script and set the correct values for modbusHost, modbusPort and modbusUnit")
    exitProcess(0)
}

print("Modbus: Connecting...")
val modbusMaster = ModbusTCPMaster(modbusHost, modbusPort)
modbusMaster.connect()
ModbusDeviceJ2Mod(modbusMaster, modbusUnit). use { modbusDevice ->
    println(" done")

    // Connect to the SunSpec device and generate a SchemaDevice with all supported SunSpec Models and Fields.
    val sunSpec = SunspecDevice.generate(modbusDevice) ?: throw ModbusException("Unable to generate SunSpec device")
    sunSpec.connect(modbusDevice, 100)

    // To get information about what your every field your device CAN actually provide
    if (allTheFieldsIWant(sunSpec).size <= 4) {
        println("Step 2: Edit the script to include the fields you want.")
        println("Below is the list of fields your device currently supports.")
        println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv")

        showAllFields(sunSpec)

        println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^")
        println("This huge list thet just flashed by is the list of all possible fields.")
        println("Step 2: Edit the script to include the fields you want, you can copy and paste what was just printed.")

        exitProcess(0)
    }

    // If no broker is specified the output is sent to the console (useful for testing)
    if (mqttBrokerHost == null || mqttTopic == null) {
        println("Step 3: Edit the script to set the MQTT information.")
        println("No MQTT broker specified, outputting to console")
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
    allFields.forEach { it.second.need() }

    println("Trying to get ${allFields.size} fields.")
    allFields.forEach { println(it) }
    device.update()
    println("Found ${allFields.filter{ it.second.value != null }.size} fields to have a value.")
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
                val jsonFieldName = it.first
                val field = it.second
                when(field.returnType) {
                    DOUBLE     -> result.put(jsonFieldName, field.doubleValue     ?: 0.0)
                    LONG       -> result.put(jsonFieldName, field.longValue       ?: 0)
                    STRING     -> result.put(jsonFieldName, field.stringValue     ?: "")
                    STRINGLIST -> result.put(jsonFieldName, field.stringListValue ?: listOf<String>())
                    BOOLEAN    -> result.put(jsonFieldName, field.booleanValue    ?: "")
                    UNKNOWN    -> TODO("This shouldn't happen")
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

fun showAllFields(schemaDevice: SchemaDevice) {
    schemaDevice.updateAll()
    schemaDevice.blocks.forEach { block ->
        block.fields.forEach { field ->
            if (!field.isSystem) {
                println("""
    // [${field.block.id}]: ${field.id}
    // ${field.description}${if (field.unit.isBlank()) "" else "\n    // Unit: ${field.unit}"}
    // Seen value: ${field.value ?: "No value available. Not implemented/unused feature?"}
    allFields.add("${field.jsonFieldName()}" to device.wantField("${field.block.id}", "${field.id}"))

"""
                )
            }
        }
    }
}

fun generateHomeAssistantConfig(
    device: SchemaDevice,
    allFields: MutableList<Pair<String, Field>>,
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
    configFields.addAll(allFields.map { it.second })
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
            val jsonFieldName = it.first
            val field = it.second
            if (!field.isSystem && field.value != null) {
                // Building a name that looks 'ok' in Home Assistant
                var name = if (field.block.shortDescription.isNullOrBlank())
                    field.block.id
                else
                    field.block.shortDescription
                name += " - "
                if(field.id.contains("_")) {
                    name += field.id
                        // Make the name of fields in repeating blocks look better
                        .replace(Regex("_([0-9]+)_"), "[$1].")
                        .replace("_", ".")
                    if (!field.id.endsWith(field.shortDescription)) {
                        name += " - ${field.shortDescription}"
                    }
                } else {
                    name += field.shortDescription
                }

                println(
                    """
    - name: "$name"
      unique_id: "SunSpec-${manufacturer.stringValue}-${serialNumber.stringValue}-$jsonFieldName"
      state_topic: "$mqttTopic"
      value_template: "{{ value_json.$jsonFieldName ${if (field.returnType == DOUBLE) "| round(4, default=0)" else "" }}}"${if(field.unit.isNotBlank()) """
      unit_of_measurement: "${field.unit}"""" else ""}
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
