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
package nl.basjes.sunspec.schema.generate

import com.ghgande.j2mod.modbus.facade.AbstractModbusMaster
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.device.j2mod.ModbusDeviceJ2Mod
import nl.basjes.modbus.schema.toYaml
import nl.basjes.sunspec.SUNSPEC_STANDARD_UNITID
import nl.basjes.sunspec.device.SunspecDevice.generate
import picocli.CommandLine
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Spec
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(name = "SunSpecDump", version = ["1.0"], mixinStandardHelpOptions = true)
class Main : Callable<Int> {
    @Option(
        names = ["-ip", "--ipaddress"],
        description = ["The hostname/IP address of the modbus device"],
        required = true
    )
    private val ipaddress: String? = null

    @Option(
        names = ["-p", "--port"],
        description = ["Use modbus port"],
        showDefaultValue = CommandLine.Help.Visibility.ON_DEMAND,
        defaultValue = "502"
    )
    private val port = 0

    @Option(
        names = ["-desc", "--description"],
        description = ["The description of this device in the generated Schema file"],
        showDefaultValue = CommandLine.Help.Visibility.ON_DEMAND,
        defaultValue = "A SunSpec Device"
    )
    private val deviceDescription: String? = null

    @Spec
    private val spec: CommandSpec? = null

    @Throws(Exception::class)
    override fun call(): Int {
        getModbusDevice(ipaddress, port).use { modbusDevice ->
            val schema = getSchema(modbusDevice, deviceDescription)
            println("#-------------- BEGIN GENERATED CODE SNIPPET --------------")
            println(schema)
            println("#-------------- END GENERATED CODE SNIPPET --------------")
        }
        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            exitProcess(CommandLine(Main()).execute(*args))
        }

        @Throws(Exception::class)
        fun getSchema(host: String?, port: Int, description: String?): String {
            return getSchema(getModbusDevice(host, port), description)
        }

        /**
         * Load the data from a real device using the provided ipaddress
         */
        @Throws(Exception::class)
        fun getModbusDevice(host: String?, port: Int): ModbusDevice {
            val master: AbstractModbusMaster = ModbusTCPMaster(host, port)
            master.connect()
            println("Connected.")
            return ModbusDeviceJ2Mod(master, SUNSPEC_STANDARD_UNITID)
        }

        @Throws(ModbusException::class)
        fun getSchema(modbusDevice: ModbusDevice?, description: String?): String {
            val schemaDevice = generate(modbusDevice!!, description!!)
                ?: throw ModbusException("Unable to obtain the schema")
            return schemaDevice.toYaml()
        }
    }
}
