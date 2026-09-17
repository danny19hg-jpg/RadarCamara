package com.example.radarcamera.domain.analysis

import com.example.radarcamera.data.local.PitchEntity
import com.example.radarcamera.domain.PitchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisEngineTest {
    private fun pitch(id:String, number:Int, mph:Double, type:PitchType=PitchType.BASEBALL_FASTBALL, receivedAt:Long=number.toLong()) = PitchEntity(id,"s",number.toLong(),number,mph,type,receivedAt)
    @Test fun emptyAnalysisHasNoInvalidMetrics() { val analysis=SessionAnalysisEngine.analyze(emptyList()); assertEquals(0,analysis.summary.total); assertTrue(analysis.byType.isEmpty()) }
    @Test fun computesOrderedSummaryAndDistributionWithoutRoundingSourceValues() {
        val analysis=SessionAnalysisEngine.analyze(listOf(pitch("late",2,71.25,PitchType.BASEBALL_SLIDER),pitch("first",1,70.0),pitch("tie",2,72.0,PitchType.BASEBALL_SLIDER,3)))
        assertEquals(listOf("first","late","tie"),analysis.points.map{it.pitchId})
        assertEquals(72.0,analysis.summary.maximum!!,0.0); assertEquals(70.0,analysis.summary.minimum!!,0.0); assertEquals(71.0833333333,analysis.summary.average!!,0.000001)
        assertEquals(2,analysis.byType.single{it.label=="Slider"}.count); assertEquals(2.0/3.0,analysis.byType.single{it.label=="Slider"}.percentage,0.000001)
    }
    @Test fun equalSpeedsAndSinglePointRemainValid() { val analysis=SessionAnalysisEngine.analyze(listOf(pitch("one",1,65.5))); assertEquals(65.5,analysis.summary.maximum!!,0.0); assertEquals(1,analysis.points.size) }
    @Test fun movingAverageUsesCurrentAndTwoPreviousPitches() { val a=SessionAnalysisEngine.analyze(listOf(pitch("a",1,12.0),pitch("b",2,9.0),pitch("c",3,6.0),pitch("d",4,12.0))); assertEquals(listOf(12.0,10.5,9.0,9.0),a.movingAverage3) }
    @Test fun paletteIsStableAndUnknownIsGray() { assertEquals(PitchColorKey.BLUE,pitchColorKey("Fastball")); assertEquals(PitchColorKey.GREEN,pitchColorKey("Dropball")); assertEquals(PitchColorKey.GRAY,pitchColorKey("Sin tipo")) }
}
