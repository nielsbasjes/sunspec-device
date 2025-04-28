package nl.basjes.sunspec.device

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.Address.Companion.of
import nl.basjes.modbus.device.api.AddressClass
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.sunspec.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object SunSpecDeviceModelFinder {
    private val LOG: Logger = LogManager.getLogger()

    /**
     * Each SunSpec device supports a different set of SunSpec Models.
     * This function scans the actual device and reports the list of available models
     * @param modbusDevice The modbus device that needs to be scanned.
     * @return The list of models that were found
     */
    @Throws(ModbusException::class)
    fun findDeviceSunSpecModels(modbusDevice: ModbusDevice?): List<DeviceSunSpecModel> {
        val models: MutableList<DeviceSunSpecModel> = ArrayList()

        // Search for the SunSpec base definitions
        val ( sunSpecStartAddress, schemaDevice )  = findSunSpecDevice(modbusDevice)

        models.add(DeviceSunSpecModel(sunSpecStartAddress, SUNS_HEADER_MODEL_ID, 0))

        var nextModelAddress = sunSpecStartAddress.increment(2)
        while (true) {
            val readModelHeaderBlock = Block
                .builder()
                .schemaDevice(schemaDevice!!)
                .id("Block_at_" + nextModelAddress.toModiconX())
                .description("Model Header at $nextModelAddress")
                .build()

            val (readModelIdField, readModelLengthField) = Utils.addModelHeaderFields(readModelHeaderBlock, nextModelAddress)

            schemaDevice.updateAll()

            val longModelId = readModelIdField.longValue
            val longModelLength = readModelLengthField.longValue

            LOG.info(
                "At {}: Model id={} --> {} registers",
                nextModelAddress, longModelId, longModelLength
            )

            // The modelId we got is invalid.
            if (longModelId == null || longModelId < -1 || longModelId > 0xFFFF) {
                throw ModbusException("Sunspec: model id $longModelId is invalid")
            }
            val modelId = Math.toIntExact(longModelId)


            // The max 2048 is a guess based on the reality of the data models.
            if (longModelLength == null || longModelLength < 0 || longModelLength > 2048) {
                throw ModbusException("Sunspec: model $modelId: Invalid Model Length $longModelLength")
            }
            val modelLength = Math.toIntExact(longModelLength)

            // ===========================
            // All values seem valid

            // Now that we know how big this model is; we can calculate the address of the next model
            val thisModelAddress = nextModelAddress
            nextModelAddress =
                nextModelAddress.increment(SUNSPEC_MODEL_ID_REGISTERS + SUNSPEC_MODEL_L_REGISTERS + modelLength)

            // The last model in the list is only a terminating entry (no data)
            if (modelId == -1 || modelId == 0xFFFF || modelId == 0 || modelLength == 0) {
                models.add(DeviceSunSpecModel(thisModelAddress, SUNS_CHAIN_END_MODEL_ID, modelLength))
                break // We're done
            }

            models.add(DeviceSunSpecModel(thisModelAddress, modelId, modelLength))
        }
        return models
    }

    /**
     * Create a SchemaDevice with the SunSpec header
     * @param modbusDevice The device to connect to
     * @return A new instance of SchemaDevice with a single Block+Field for the header
     * @throws ModbusException In case of error.
     */
    @Throws(ModbusException::class)
    fun findSunSpecDevice(modbusDevice: ModbusDevice?): Pair<Address, SchemaDevice?> {
        var sunSpecStartAddress: Address? = null
        var schemaDevice: SchemaDevice? = null

        for (actualStartAddress in SUNSPEC_STANDARD_START_PHYSICAL_ADDRESS) {
            sunSpecStartAddress = of(AddressClass.HOLDING_REGISTER, actualStartAddress)
            LOG.info("Looking for SunSpec header at {}", sunSpecStartAddress)

            // Create the empty SchemaDevice
            schemaDevice = SchemaDevice
                .builder()
                .description("A SunSpec device with base $sunSpecStartAddress")
                .build()

            schemaDevice.connect(modbusDevice!!)

            val sunSHeaderField = Utils.addSunSHeaderBlock(schemaDevice, sunSpecStartAddress)
            schemaDevice.updateAll()
            val sunSHeaderFieldValue = sunSHeaderField.stringValue
            if (SUNSPEC_HEADER != sunSHeaderFieldValue) {
                LOG.info("There is no SunSpec header at {}", sunSpecStartAddress)
                sunSpecStartAddress = null
                schemaDevice = null
                continue
            }
            LOG.info("Found the SunSpec header at {}", sunSpecStartAddress)
            break
        }

        if (sunSpecStartAddress == null) {
            throw ModbusException("Unable to find the needed SunS header at any of " + SUNSPEC_STANDARD_START_PHYSICAL_ADDRESS.contentToString())
        }
        return Pair(sunSpecStartAddress, schemaDevice)
    }

    class DeviceSunSpecModel(val address: Address, val id: Int, val registers: Int) {
        override fun toString(): String {
            return "DeviceSunSpecModel(@$address, id=$id, registers=$registers}"
        }
    }
}
