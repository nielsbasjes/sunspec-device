package nl.basjes.sunspec.schema.generate

import nl.basjes.modbus.schema.toSchemaDevice
import nl.basjes.modbus.schema.toYaml
import nl.basjes.sunspec.schema.generate.Main.getSchema
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestGeneratingSunSpecSchema{

    private val log: Logger = LogManager.getLogger()

    @Test
    fun generateSunSpecSchema() {
        val schema = getSchema(DeviceSMASunnyBoy36.getDevice(), "description")
        log.info("\n$schema")

        log.info("Creating a new SchemaDevice from the obtained schema")
        val schemaDevice = schema.toSchemaDevice()
        require(schemaDevice.initialize()) { "Unable to initialize schemaDevice" }

        schemaDevice.resolveAllImmutableFields()

        val recreatedSchema = schemaDevice.toYaml()

        assertEquals(schema, recreatedSchema)

        println(recreatedSchema)

    }
}
