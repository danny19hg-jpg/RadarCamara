package com.example.radarcamera.domain

import java.time.LocalDate
import java.time.Period

enum class Sport(val label: String) { BASEBALL("Béisbol"), SOFTBALL("Softbol") }
enum class ThrowingHand(val label: String) { RIGHT("Derecha"), LEFT("Izquierda"), BOTH("Ambas") }

data class PlayerDraft(
    val name: String = "",
    val birthDate: String = "",
    val sport: Sport = Sport.BASEBALL,
    val position: String = "",
    val throwingHand: ThrowingHand = ThrowingHand.RIGHT,
    val heightCm: String = "",
    val weightKg: String = "",
    val category: String = "",
    val teamAcademy: String = ""
) {
    fun errors(today: LocalDate = LocalDate.now()): Map<String, String> = buildMap {
        if (name.trim().isEmpty() || name.trim().length > 100)
            put("name", "Escribe un nombre de 1 a 100 caracteres.")
        val birth = birthDateOrNull()
        if (birth == null || birth > today || birth < today.minusYears(120))
            put("birthDate", "Usa AAAA-MM-DD y una fecha válida dentro de los últimos 120 años.")
        if (!validOptionalNumber(heightCm, 300.0))
            put("heightCm", "La estatura debe ser mayor que 0 y hasta 300 cm, o quedar vacía.")
        if (!validOptionalNumber(weightKg, 500.0))
            put("weightKg", "El peso debe ser mayor que 0 y hasta 500 kg, o quedar vacío.")
    }

    fun birthDateOrNull(): LocalDate? = try {
        LocalDate.parse(birthDate.trim())
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }
}

fun parseOptionalDecimal(value: String): Double? =
    value.trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()

private fun validOptionalNumber(value: String, maximum: Double): Boolean {
    if (value.isBlank()) return true
    val number = parseOptionalDecimal(value) ?: return false
    return number.isFinite() && number > 0 && number <= maximum
}

fun ageOn(birthDate: LocalDate, date: LocalDate = LocalDate.now()): Int =
    Period.between(birthDate, date).years
