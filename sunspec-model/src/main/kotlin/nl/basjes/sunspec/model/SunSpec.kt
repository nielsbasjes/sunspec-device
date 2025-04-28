package nl.basjes.sunspec.model

import kotlinx.serialization.json.Json
import nl.basjes.sunspec.SUNSPEC_MODEL_FILENAMES
import nl.basjes.sunspec.model.entities.Group
import nl.basjes.sunspec.model.entities.SunSpecModel
import java.io.IOException


class SunSpec {
    val models: Map<Int, SunSpecModel>

    /**
     * @param modelNr The number of the model for which we need the definition
     * @return The instance of SunSpecModel for the requested model, or null if non-existent.
     */
    fun getModel(modelNr: Int): SunSpecModel? {
        return models[modelNr]
    }

    init {
        // isLenient is needed because sometimes count is an Integer and sometimes a String.
        val json = Json { isLenient = true }

        val sunSpecModels = SUNSPEC_MODEL_FILENAMES
            .map { modelFileName: String ->
                try {
                    val modelResource = this.javaClass.classLoader.getResource(modelFileName)
                        ?: throw IllegalStateException("Unable to locate $modelFileName ... this should never happen")
                    json.decodeFromString<SunSpecModel>(modelResource.readText())
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            .associateBy { it.id }
            .toMutableMap()

        val sunSHeaderGroup = Group(
                name        = "SunSHeader",
                type        = Group.Type.GROUP,
                label       = "Empty Group",
                description = "The SunS Header Model has no Points")
        val sunSHeaderModel = SunSpecModel(
                id          = 0,
                group       = sunSHeaderGroup,
                label       = "Start of the SunSpec Model chain",
                description = "The starting value of the chain of SunSpec models of a device")
        sunSpecModels[0] = sunSHeaderModel

        val endOfChainGroup = Group(
                name        = "EndOfChain",
                type        = Group.Type.GROUP,
                label       = "Empty Group",
                description = "The End of Chain Model has no Points")
        val endOfChainModelFFFF = SunSpecModel(
                id          = 0xFFFF,
                group       = endOfChainGroup,
                label       = "End of the SunSpec Model chain",
                description = "The closing empty model of chain of SunSpec models of a device")
        sunSpecModels[0XFFFF] = endOfChainModelFFFF

        sunSpecModels.values.forEach { obj: SunSpecModel -> obj.init() }

        models = sunSpecModels.toSortedMap()
    }
}
