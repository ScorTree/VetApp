import kotlinx.serialization.Serializable

@Serializable
data class CitaDTO(
    val cita_fecha: String,
    val cita_hora: String,
    val cita_motivo: String,
    val pac_id: Int
)
