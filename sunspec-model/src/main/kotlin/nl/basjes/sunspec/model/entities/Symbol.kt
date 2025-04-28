package nl.basjes.sunspec.model.entities

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Symbol(
    @Required
    @SerialName("name")     val name: String,

    @Required
    @SerialName("value")    val value: Int,

    @SerialName("label")    val label: String? = null,
    @SerialName("desc")     val description: String? = null,
    @SerialName("detail")   val detail: String? = null,
    @SerialName("notes")    val notes: String? = null,
    @SerialName("comments") val comments: List<String> = ArrayList(),
) {
    val cleanName = name.trim { it <= ' ' }.replace('-', '_').replace(' ', '_')
}
