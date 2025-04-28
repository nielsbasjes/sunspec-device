package nl.basjes.sunspec.model.entities

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON Schema for SunSpec information model definition
 */
@Serializable
class SunSpecModel (
    @Required
    @SerialName(value = "id")       val id: Int,

    @Required
    @SerialName(value = "group")    val group: Group,

    @SerialName("label")            val label:       String?       = null,
    @SerialName("desc")             val description: String?       = null,
    @SerialName("detail")           val detail:      String?       = null,
    @SerialName("notes")            val notes:       String?       = null,
    @SerialName("comments")         val comments:    List<String>? = null,
) {
    val cleanLabel        = label       ?: group.label
    val cleanDescription  = description ?: group.description
    val cleanDetail       = detail      ?: group.detail
    val cleanNotes        = notes       ?: group.notes
    val cleanComments     = comments    ?: group.comments

    fun init() {
        Utils.fixProblemsInStandardSunSpec(this)

        // Mark the 2 model header points as such.
        if (group.points.size >= 2) {
            if (group.points[0].name == "ID" &&
                group.points[1].name == "L") {
                group.points[0].isModelHeader = true
                group.points[1].isModelHeader = true
            }
        }
        group.init()
    }

    override fun toString(): String {
        return "SunSpecModel(id=$id, label=$cleanLabel)"
    }
}
