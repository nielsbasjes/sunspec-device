package nl.basjes.sunspec.device

import nl.basjes.modbus.device.api.Address
import nl.basjes.modbus.device.api.ModbusDevice
import nl.basjes.modbus.device.api.RegisterValue
import nl.basjes.modbus.device.exception.ModbusException
import nl.basjes.modbus.schema.Block
import nl.basjes.modbus.schema.Field
import nl.basjes.modbus.schema.SchemaDevice
import nl.basjes.modbus.schema.ReturnType
import nl.basjes.modbus.schema.utils.CodeGeneration.convertToCodeCompliantName
import nl.basjes.modbus.schema.utils.StringTable
import nl.basjes.sunspec.SUNSPEC_MODEL_ID_REGISTERS
import nl.basjes.sunspec.SUNSPEC_MODEL_L_REGISTERS
import nl.basjes.sunspec.SUNS_CHAIN_END_MODEL_ID
import nl.basjes.sunspec.SUNS_HEADER_MODEL_ID
import nl.basjes.sunspec.model.SunSpec
import nl.basjes.sunspec.model.entities.Group
import nl.basjes.sunspec.model.entities.Point
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant
import java.util.*

object SunspecDevice {
    private val LOG: Logger = LogManager.getLogger()

    private fun getFirstRegisterValue(field: Field): RegisterValue?  {
        field.initialize()
        val parsedExpression = field.parsedExpression ?: return null  // Nothing to do
        val registerValues = parsedExpression.getRegisterValues(field.block.schemaDevice)
        if (registerValues.isEmpty()) {
            return null // Nothing to do
        }
        return registerValues[0]
    }

    private fun setCommentOnFirstRegisterValue(field: Field, comment: String) {
        getFirstRegisterValue(field)?.comment = comment
    }

    /**
     * Determine the Sunspec Schema for the provided device
     * @param modbusDevice The ModbusDevice for which the Schema is needed
     * @return The SunSpec schema device for this specific device.
     */
    @JvmStatic
    @Throws(ModbusException::class)
    fun generate(
        modbusDevice: ModbusDevice,
        /**
         * A human-readable description of this schema device.
         */
        description: String = "",
    ): SchemaDevice? {
        // We need 2 things:
        // - The official SunSpec schemas
        // - The actual device to know what it supports.
        val sunSpec = SunSpec()

        // Get the list of all SunSpec model actually present in this device
        val deviceSunSpecModels = SunSpecDeviceModelFinder.findDeviceSunSpecModels(modbusDevice)

        if (deviceSunSpecModels.isEmpty()) {
            LOG.error("Unable to find any SunSpec models")
            return null
        }

        val table = StringTable()
        table.withHeaders(
            "Model", "Address", "Length", "Label"
        )
        for (deviceSunSpecModel in deviceSunSpecModels) {
            val model = sunSpec.getModel(deviceSunSpecModel.id)
            if (model == null) {
                table.addRow(
                    deviceSunSpecModel.id.toString(),
                    deviceSunSpecModel.address.toCleanFormat(),
                    deviceSunSpecModel.registers.toString(),
                    "Non existent model"
                )
            } else {
                table.addRow(
                    deviceSunSpecModel.id.toString(),
                    deviceSunSpecModel.address.toCleanFormat(),
                    deviceSunSpecModel.registers.toString(),
                    model.cleanLabel ?: ""
                )
            }
        }
        LOG.info("Detected models: \n{}", table)

        val schemaDevice = SchemaDevice(description)
        // Ensure this new SchemaDevice is connected to the same ModbusDevice
        // DO NOT use the optimized version because some devices (i.e. mine does) will give
        // a modbus level error if you fetch a register for something it does not have.
        // Such a situation will make the entire block fail, so for starters we fetch them one by one.
        schemaDevice.connectBase(modbusDevice)

        for (deviceSunSpecModel in deviceSunSpecModels) {
            val modelId = deviceSunSpecModel.id
            val modelLength = deviceSunSpecModel.registers
            val modelAddress = deviceSunSpecModel.address

            // Special case is the header
            if (modelId == SUNS_HEADER_MODEL_ID) {
                val sunSHeaderBlock = Utils.addSunSHeaderBlock(schemaDevice, deviceSunSpecModel.address)
                setCommentOnFirstRegisterValue(sunSHeaderBlock, "SunS header")
                continue
            }

            // Special case is the end of the chain
            if (modelId == SUNS_CHAIN_END_MODEL_ID) {
                val endChainBlock = Utils.addSunSpecEndOfChainBlock(schemaDevice)
                val (endChainId, _) = Utils.addModelHeaderFields(endChainBlock, modelAddress)
                setCommentOnFirstRegisterValue(endChainId, "NO MORE MODELS")
                break // This should be the last in the chain
            }

            // Find the official SunSpec specification for this one.
            val sunSpecModel = sunSpec.getModel(modelId)

            if (sunSpecModel == null) {
                LOG.fatal("Unable to get model for ID: {}", modelId)
                continue
            }

            // Create and add a new Block for this SunSpec model.
            val modelDescription =
                if (sunSpecModel.group.description.isNullOrBlank()) {
                    "[Model " + sunSpecModel.id + "]:" + sunSpecModel.group.label
                } else {
                    "[Model " + sunSpecModel.id + "]:" + sunSpecModel.group.label + ": " + sunSpecModel.group.description
                }

            val block = Block
                .builder()
                .schemaDevice(schemaDevice)
                .id("Model " + sunSpecModel.id)
                .description(modelDescription)
                .build()

            addGroup(block, "", sunSpecModel.group, modelAddress, modelId, modelLength)
        }


        if (!schemaDevice.initialize()) {
            throw ModbusException("Unable to initialize the schema device")
        }

        // Determine a better description if none was set
        if (description.isBlank()) {
            schemaDevice["Model 1"]?.let {
                val manufacturer = it["Mn"]
                val model =  it["Md"]
                val serialNr =  it["SN"]
                val version =  it["Vr"]
                requireNotNull(manufacturer)
                requireNotNull(model)
                requireNotNull(serialNr)
                requireNotNull(version)

                manufacturer.need()
                model.need()
                serialNr.need()
                version.need()
                schemaDevice.update(1000)
                schemaDevice.description = "A schema specifically for the SunSpec device made by ${manufacturer.stringValue} model ${model.stringValue} using version ${version.stringValue} (SN: ${serialNr.stringValue})"
            }
        }

        return schemaDevice
    }

    @Throws(ModbusException::class)
    private fun addGroup(
        block: Block,
        prefix: String,
        group: Group,
        thisModelAddress: Address,
        modelId: Int,
        modelLength: Int?
    ) {
        // Add the Points that are directly in this model.

        var pointCounter = 0
        if (modelLength == null) {
            // I.e. a multi level deep nested group
            pointCounter = 3 // Always do the last in the switch below
        }
        for (point in group.points) {
            pointCounter++
            val field = createAndAddFieldToModel(block, thisModelAddress, point, prefix)

            when (pointCounter) {
                // 1: The ID
                1 -> getFirstRegisterValue(field)?.let {
                    it.comment =
                        "--------------------------------------\n" +
                        "Model $modelId [Header @ ${it.address.toCleanFormat()}]: ${group.label}"
                    }

                // 2: The Length
                2 -> {}
                // 3: The first data register
                3 -> getFirstRegisterValue(field)?.let {
                    it.comment = "Model $modelId [Data @ ${it.address.toCleanFormat()} - ${it.address.increment(modelLength!!).toCleanFormat()}]: $modelLength registers"
                }
                else -> {}
            }
        }
        for (subGroup in group.groups) {
            // The "count" of a subgroup can be a number OR the NAME of the field which contains the number.
            val countString = subGroup.count
            var count = 0
            if (countString != null) {
                try {
                    count = countString.toInt()
                } catch (nfe: NumberFormatException) {
                    val countField = block.getField(countString)
                        ?: throw ModbusException("Unable to find the count field named \"$countString\" for this group")
                    countField.update()
                    val countValue = countField.longValue
                        ?: throw ModbusException("Unable to read the value of the count field named \"$countString\" for this group")
                    count = Math.toIntExact(countValue)
                }
            }

            // The older models have a fixed set of points and a repeating set.
            // The number of repeats must be calculated from the sizes of the complete model, the fixed part and repeating part.
            if (count == 0 && modelLength != null && group.groups.size == 1) {
                if (group.dataSize == 0 || subGroup.dataSize == 0) {
                    throw ModbusException("SunSpec repeat block problem")
                }

                count = (SUNSPEC_MODEL_ID_REGISTERS + SUNSPEC_MODEL_L_REGISTERS + modelLength - group.dataSize) / subGroup.dataSize

                // Check for rounding errors
                require(group.dataSize + (subGroup.dataSize * count) == modelLength) {
                    throw ModbusException("SunSpec repeat block problem: ${group.dataSize} + (${subGroup.dataSize} * $count) == $modelLength)")
                }
            }

            var subGroupPrefix = prefix
            if (prefix.isEmpty()) {
                subGroupPrefix = "R"
            }

            for (index in 0 until count) {
                addGroup(
                    block,
                    subGroupPrefix + index + "_",
                    subGroup,
                    thisModelAddress.increment(group.dataSize + (index * subGroup.dataSize)),
                    modelId,
                    null
                )
            }
        }
    }


    // ----------------------------------------------------------------------------------------------------
    private fun mappingString(point: Point): String {
        val mappingString = StringBuilder()
        for (symbol in point.symbols) {
            mappingString.append(" ; ").append(symbol.value).append("->'").append(symbol.cleanName).append("'")
        }
        return mappingString.toString()
    }

    private fun Point.getCamelCaseName() = convertToCodeCompliantName(name, true)
    private fun Point.getCamelCaseSfName() = if (scalingFactor== null) { null } else { convertToCodeCompliantName(scalingFactor!!, true) }

    private fun createAndAddFieldToModel(block: Block, modelBaseAddress: Address, point: Point, prefix: String): Field {
        val outputType: ReturnType
        val functionName: String
        val additionalArguments: String

        val typeLoadingParameters: TypeMapping
        when (point.type) {
            Point.Type.PAD,
            Point.Type.SUNSSF,
            Point.Type.INT_16,
            Point.Type.INT_32,
            Point.Type.INT_64,
            Point.Type.RAW_16,
            Point.Type.UINT_16,
            Point.Type.UINT_32,
            Point.Type.UINT_64,
            Point.Type.COUNT,
            Point.Type.ACC_16,
            Point.Type.ACC_32,
            Point.Type.ACC_64,
            Point.Type.FLOAT_32,
            Point.Type.FLOAT_64,
            Point.Type.STRING,
            Point.Type.IPADDR,
            Point.Type.IPV_6_ADDR,
            Point.Type.EUI_48 -> {
                typeLoadingParameters = TYPE_MAPPINGS_NO_SYMBOLS[point.type]!!
                outputType = typeLoadingParameters.returnType
                functionName = typeLoadingParameters.functionName
                additionalArguments = typeLoadingParameters.notImplemented
            }

            Point.Type.BITFIELD_16,
            Point.Type.BITFIELD_32,
            Point.Type.BITFIELD_64,
            Point.Type.ENUM_16,
            Point.Type.ENUM_32 -> {
                if (point.symbols.isEmpty()) {
                    typeLoadingParameters = TYPE_MAPPINGS_NO_SYMBOLS[point.type]!!
                    outputType = typeLoadingParameters.returnType
                    functionName = typeLoadingParameters.functionName
                    additionalArguments = typeLoadingParameters.notImplemented
                } else {
                    typeLoadingParameters = TYPE_MAPPINGS_WITH_SYMBOLS[point.type]!!
                    outputType = typeLoadingParameters.returnType
                    functionName = typeLoadingParameters.functionName
                    additionalArguments = typeLoadingParameters.notImplemented + mappingString(point)
                }
            }

            else -> {
                LOG.warn(
                    "Model @{}: Dumping point {} with type {} as HexString",
                    modelBaseAddress, point.name, point.type
                )
                // Anything that we missed: Just dump it as a raw hex string
                outputType = ReturnType.STRING
                functionName = "hexstring"
                additionalArguments = ""
            }
        }

        val fieldBuilder = Field
            .builder()
            .block(block)
            .id(prefix + point.getCamelCaseName())
            .unit(point.units)
            .immutable(point.mutable == Point.Mutable.IMMUTABLE)
//            .returnType(outputType)

        if (point.type == Point.Type.SUNSSF) {
            fieldBuilder.description("[${block.id}](${point.name}): Scaling factor")
        } else {
            val label: String
            if (point.label.isNullOrEmpty()) {
                label = "[${block.id}](${point.name}): ${point.description?:""}"
            } else {
                label = "[${block.id}](${point.label}): ${point.description?:""}"
            }
            fieldBuilder.description(label)
        }

        // Some fields are considered to be system fields (i.e. not directly usable applications fields)
        if (point.type == Point.Type.SUNSSF || point.type == Point.Type.PAD ||
            (point.isImmutable() && (point.name == "ID" || point.name == "L"))
        ) {
            fieldBuilder.system(true).immutable(true)
        }

        val registerAddress = modelBaseAddress.increment(point.offsetInGroup)

        var expression =
            functionName + "(" + registerAddress + (if (point.size == 1) "" else "#" + point.size) + additionalArguments + ")"

        val camelCaseSfName: String? = point.getCamelCaseSfName()
        if (!camelCaseSfName.isNullOrEmpty()) {
            expression += " * (10^$camelCaseSfName)"
            // If there is an SF then the output type is usually a double
//            fieldBuilder.returnType(ReturnType.DOUBLE)
        }

        if (point.isTimeInstant) {
            val y2kEpochOffset = Instant.parse("2000-01-01T00:00:00.000Z").toEpochMilli()
            expression =  "($expression*1000+$y2kEpochOffset)"
        }

        return fieldBuilder
            .expression(expression)
            .build()
    }


    data class TypeMapping(val returnType: ReturnType, val functionName: String, val notImplemented: String = "")

    // Maps Point.Type to  outputType, LoadFunction, notImplemented when NO symbols are present
    private val TYPE_MAPPINGS_NO_SYMBOLS: MutableMap<Point.Type, TypeMapping> = TreeMap()

    // Maps Point.Type to  outputType, LoadFunction, notImplemented when there ARE symbols present
    private val TYPE_MAPPINGS_WITH_SYMBOLS: MutableMap<Point.Type, TypeMapping> = TreeMap()

    init {
        // PAD is for applications a useless value because it is always 0x8000
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.PAD] =           TypeMapping( ReturnType.STRING,     "hexstring")
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.SUNSSF] =        TypeMapping( ReturnType.LONG,       "int16",      ";0x8000"                                                     )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.INT_16] =        TypeMapping( ReturnType.LONG,       "int16",      ";0x8000"                                                     )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.INT_32] =        TypeMapping( ReturnType.LONG,       "int32",      ";0x8000 0x0000"                                              )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.INT_64] =        TypeMapping( ReturnType.LONG,       "int64",      ";0x8000 0x0000 0x0000 0x0000"                                )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.RAW_16] =        TypeMapping( ReturnType.STRING,     "hexstring")
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.UINT_16] =       TypeMapping( ReturnType.LONG,       "uint16",     "; 0xFFFF ;0x8000"                                            )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.UINT_32] =       TypeMapping( ReturnType.LONG,       "uint32",     "; 0xFFFF 0xFFFF ;0x8000 0x0000"                              )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.UINT_64] =       TypeMapping( ReturnType.LONG,       "uint64",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF ;0x8000 0x0000 0x0000 0x0000"  )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.COUNT] =         TypeMapping( ReturnType.LONG,       "uint16",     "; 0x0000"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.ACC_16] =        TypeMapping( ReturnType.LONG,       "uint16",     "; 0x0000"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.ACC_32] =        TypeMapping( ReturnType.LONG,       "uint32",     "; 0x0000 0x0000"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.ACC_64] =        TypeMapping( ReturnType.LONG,       "uint64",     "; 0x0000 0x0000 0x0000 0x0000"                               )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.FLOAT_32] =      TypeMapping( ReturnType.DOUBLE,     "ieee754_32", "; 0x7FC0 0x0000"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.FLOAT_64] =      TypeMapping( ReturnType.DOUBLE,     "ieee754_64", "; 0x7FF8 0x0000 0x0000 0x0000"                               )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.STRING] =        TypeMapping( ReturnType.STRING,     "utf8")
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.IPADDR] =        TypeMapping( ReturnType.STRING,     "hexstring",  "; 0x0000 0x0000"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.IPV_6_ADDR] =    TypeMapping( ReturnType.STRING,     "hexstring",  "; 0x0000 0x0000 0x0000 0x0000"                               )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.EUI_48] =        TypeMapping( ReturnType.STRING,     "eui48",      "; 0x0000 0x0000 0x0000 0x0000"                               )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.ENUM_16] =       TypeMapping( ReturnType.LONG,       "int16",      "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.ENUM_32] =       TypeMapping( ReturnType.LONG,       "int32",      "; 0xFFFF 0xFFFF"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.BITFIELD_16] =   TypeMapping( ReturnType.LONG,       "int16",      "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.BITFIELD_32] =   TypeMapping( ReturnType.LONG,       "int32",      "; 0xFFFF 0xFFFF"                                             )
        TYPE_MAPPINGS_NO_SYMBOLS[Point.Type.BITFIELD_64] =   TypeMapping( ReturnType.LONG,       "int64",      "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF"                               )
        TYPE_MAPPINGS_WITH_SYMBOLS[Point.Type.ENUM_16] =     TypeMapping( ReturnType.STRING,     "enum",       "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[Point.Type.ENUM_32] =     TypeMapping( ReturnType.STRING,     "enum",       "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[Point.Type.BITFIELD_16] = TypeMapping( ReturnType.STRINGLIST, "bitset",     "; 0xFFFF"                                                    )
        TYPE_MAPPINGS_WITH_SYMBOLS[Point.Type.BITFIELD_32] = TypeMapping( ReturnType.STRINGLIST, "bitset",     "; 0xFFFF 0xFFFF ; 0x8000 0xFFFF"                             ) // TODO: The 0x8000 0xFFFF is NOT in the SPEC
        TYPE_MAPPINGS_WITH_SYMBOLS[Point.Type.BITFIELD_64] = TypeMapping( ReturnType.STRINGLIST, "bitset",     "; 0xFFFF 0xFFFF 0xFFFF 0xFFFF"                               )
    }
}

