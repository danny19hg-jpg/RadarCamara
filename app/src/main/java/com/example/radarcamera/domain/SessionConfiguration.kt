package com.example.radarcamera.domain

enum class PitchType(val sport: Sport, val label: String) {
    BASEBALL_FASTBALL(Sport.BASEBALL, "Fastball"), BASEBALL_CURVE(Sport.BASEBALL, "Curveball"),
    BASEBALL_SLIDER(Sport.BASEBALL, "Slider"), BASEBALL_CHANGEUP(Sport.BASEBALL, "Changeup"),
    BASEBALL_SINKER(Sport.BASEBALL, "Sinker"), BASEBALL_CUTTER(Sport.BASEBALL, "Cutter"),
    SOFTBALL_FASTBALL(Sport.SOFTBALL, "Fastball"),
    SOFTBALL_DROPBALL(Sport.SOFTBALL, "Dropball"), SOFTBALL_RISEBALL(Sport.SOFTBALL, "Riseball"),
    SOFTBALL_CHANGEUP(Sport.SOFTBALL, "Changeup"), SOFTBALL_CURVE(Sport.SOFTBALL, "Curveball"),
    SOFTBALL_SCREWBALL(Sport.SOFTBALL, "Screwball");
    companion object { fun forSport(sport: Sport) = entries.filter { it.sport == sport } }
}

class SessionGoal private constructor(private val preset: Int?, private val custom: String?) {
    fun valueOrNull(): Int? = (preset ?: custom?.trim()?.toIntOrNull())?.takeIf { it > 0 }
    fun errors(): List<String> = if (valueOrNull() == null) listOf("Escribe un objetivo entero positivo.") else emptyList()
    companion object { fun preset(value: Int) = SessionGoal(value, null); fun custom(value: String) = SessionGoal(null, value) }
}

data class SessionDraft(val playerId: String = "", val sport: Sport = Sport.BASEBALL, val pitchType: PitchType = PitchType.BASEBALL_FASTBALL, val goalMode: Int? = 0, val customGoal: String = "", val recordingEnabled: Boolean = false) {
    fun goal() = if (goalMode == null) SessionGoal.custom(customGoal) else SessionGoal.preset(goalMode)
    fun errors(): Map<String, String> = buildMap {
        if (playerId.isBlank()) put("player", "Selecciona un jugador activo.")
        if (pitchType.sport != sport) put("pitchType", "Selecciona un tipo válido para el deporte.")
    }
}
