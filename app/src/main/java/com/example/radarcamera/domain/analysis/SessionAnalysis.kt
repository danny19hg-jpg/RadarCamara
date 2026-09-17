package com.example.radarcamera.domain.analysis

import com.example.radarcamera.data.local.PitchEntity

data class AnalysisSummary(val total:Int, val maximum:Double?, val minimum:Double?, val average:Double?)
enum class PitchColorKey { BLUE, PURPLE, ORANGE, TEAL, GREEN, CYAN, GRAY }
fun pitchColorKey(label:String):PitchColorKey = when(label.lowercase()) { "fastball","recta"->PitchColorKey.BLUE; "curveball","curva"->PitchColorKey.PURPLE; "slider","riseball"->PitchColorKey.ORANGE; "changeup","cambio"->PitchColorKey.TEAL; "sinker","dropball"->PitchColorKey.GREEN; "cutter","screwball"->PitchColorKey.CYAN; else->PitchColorKey.GRAY }
data class PitchPoint(val pitchId:String, val number:Int, val mph:Double, val label:String, val colorKey:PitchColorKey)
data class PitchTypeSummary(val label:String, val count:Int, val percentage:Double, val maximum:Double?, val minimum:Double?, val average:Double?, val points:List<PitchPoint>)
data class SessionAnalysis(val summary:AnalysisSummary, val points:List<PitchPoint>, val movingAverage3:List<Double>, val byType:List<PitchTypeSummary>)

object SessionAnalysisEngine {
    fun analyze(source: List<PitchEntity>): SessionAnalysis {
        val ordered = source.sortedWith(compareBy<PitchEntity> { it.number }.thenBy { it.receivedAt }.thenBy { it.id })
        val points = ordered.map { val label=it.pitchType.label.ifBlank { "Sin tipo" }; PitchPoint(it.id, it.number, it.mph, label, pitchColorKey(label)) }
        val finite = points.filter { it.mph.isFinite() }.map { it.mph }
        fun summary(values:List<Double>) = AnalysisSummary(values.size, values.maxOrNull(), values.minOrNull(), values.takeIf { it.isNotEmpty() }?.average())
        val groups = points.groupBy { it.label }.toSortedMap().map { (label, values) ->
            val speeds=values.filter { it.mph.isFinite() }.map { it.mph }
            PitchTypeSummary(label,values.size,if(points.isEmpty()) 0.0 else values.size.toDouble()/points.size,speeds.maxOrNull(),speeds.minOrNull(),speeds.takeIf { it.isNotEmpty() }?.average(),values)
        }
        val all=summary(finite)
        val trend=points.indices.map { index -> points.subList((index-2).coerceAtLeast(0),index+1).map{it.mph}.filter{it.isFinite()}.average().takeIf{it.isFinite()}?:0.0 }
        return SessionAnalysis(all.copy(total=points.size),points,trend,groups)
    }
}
