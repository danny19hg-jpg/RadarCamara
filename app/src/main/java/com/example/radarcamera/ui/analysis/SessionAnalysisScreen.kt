package com.example.radarcamera.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.radarcamera.domain.analysis.*
import com.example.radarcamera.ui.common.*

@Composable fun SessionAnalysisScreen(viewModel:SessionAnalysisViewModel,onBack:()->Unit,onDetail:()->Unit) {
 val state by viewModel.state.collectAsStateWithLifecycle()
 CoachingPage("Análisis de sesión",onBack) {
  when(val current=state) {
   SessionAnalysisUiState.Loading -> LoadingMessage()
   SessionAnalysisUiState.Empty -> Text("Esta sesión no tiene lanzamientos registrados.")
   SessionAnalysisUiState.NotFound -> Text("La sesión no está disponible para análisis.")
   is SessionAnalysisUiState.Error -> ErrorMessage(current.message)
   is SessionAnalysisUiState.Content -> {
    val a=current.analysis
    Text("${current.playerName} · ${current.sport}"); Text("Total: ${a.summary.total}")
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){ Metric("Máxima",a.summary.maximum);Metric("Mínima",a.summary.minimum);Metric("Promedio",a.summary.average) }
    Text("Distribución de lanzamientos")
    a.byType.forEach{Text("${it.label}: ${it.count} (${String.format("%.1f",it.percentage*100)}%)")}
    Text("Sesión completa")
    FullSessionChart(a.points,a.movingAverage3)
    Text("Promedio móvil (3)", color=Color(0xFFFFC107))
    Text("La tendencia general puede mezclar distintos tipos de lanzamiento. Para comparar rendimiento utiliza el análisis por tipo.")
    a.byType.forEach { Text("● ${it.label} · ${it.count}", color=colorFor(it.points.first().colorKey)) }
    var selected by remember(a.byType){mutableStateOf(a.byType.firstOrNull()?.label)}
    if(a.byType.isNotEmpty()) {
     AnalysisTypeSelector(selected!!,a.byType.map{it.label}){selected=it}
     val type=a.byType.first{it.label==selected}
     Text("${type.label}: ${type.count} lanzamientos")
     SpeedChart(type.points)
    }
    Button(onClick=onDetail,modifier=Modifier.fillMaxWidth()){Text("Ver lanzamientos y videos")}
   }
  }
 }
}
@Composable private fun Metric(label:String,value:Double?) { Column { Text(label); Text(value?.let { String.format("%.1f MPH",it) }?:"—") } }
@Composable private fun AnalysisTypeSelector(selected:String,options:List<String>,onSelected:(String)->Unit) { var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text("Tipo: $selected")}; DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { options.forEach { DropdownMenuItem(text={Text(it)},onClick={expanded=false;onSelected(it)}) } } } }
@Composable private fun SpeedChart(points:List<PitchPoint>) {
 Column { Text("Velocidad por lanzamiento"); Canvas(Modifier.fillMaxWidth().height(180.dp)) {
  if(points.isNotEmpty()) { val speeds=points.map{it.mph}; val min=speeds.minOrNull()!!; val max=speeds.maxOrNull()!!; val range=(max-min).takeIf{it>0.0}?:1.0; val step=if(points.size==1) 0f else size.width/(points.size-1); val path=androidx.compose.ui.graphics.Path()
   points.forEachIndexed { i,p -> val x=if(points.size==1)size.width/2 else i*step; val y=size.height-((p.mph-min)/range).toFloat()*size.height; if(i==0)path.moveTo(x,y)else path.lineTo(x,y) }
   drawPath(path,Color(0xFF1976D2),style=Stroke(3f)); points.forEachIndexed { i,p -> val x=if(points.size==1)size.width/2 else i*step; val y=size.height-((p.mph-min)/range).toFloat()*size.height; drawCircle(colorFor(p.colorKey),5f,androidx.compose.ui.geometry.Offset(x,y)) }
  }
 } }
}
private fun colorFor(key:PitchColorKey)=when(key){PitchColorKey.BLUE->Color(0xFF1976D2);PitchColorKey.PURPLE->Color(0xFF7B1FA2);PitchColorKey.ORANGE->Color(0xFFF57C00);PitchColorKey.TEAL->Color(0xFF00897B);PitchColorKey.GREEN->Color(0xFF388E3C);PitchColorKey.CYAN->Color(0xFF039BE5);PitchColorKey.GRAY->Color(0xFF757575)}
@Composable private fun FullSessionChart(points:List<PitchPoint>,trend:List<Double>) {
 var selected by remember(points){mutableStateOf<PitchPoint?>(null)}
 Column {
  Canvas(Modifier.fillMaxWidth().height(190.dp).pointerInput(points) { detectTapGestures { offset ->
   if(points.isNotEmpty()) { val index=(offset.x / size.width * (points.size-1)).toInt().coerceIn(0,points.lastIndex); selected=points[index] }
  } }) {
   if(points.isNotEmpty()) {
    val values=points.map{it.mph}+trend; val min=values.minOrNull()!!; val range=(values.maxOrNull()!!-min).takeIf{it>0.0}?:1.0
    fun x(i:Int)=if(points.size==1)size.width/2 else i*size.width/(points.size-1)
    fun y(v:Double)=size.height-((v-min)/range).toFloat()*size.height
    val line=androidx.compose.ui.graphics.Path(); points.forEachIndexed{i,p->if(i==0)line.moveTo(x(i),y(p.mph))else line.lineTo(x(i),y(p.mph))}; drawPath(line,Color.Gray,style=Stroke(2f))
    val avg=androidx.compose.ui.graphics.Path(); trend.forEachIndexed{i,v->if(i==0)avg.moveTo(x(i),y(v))else avg.lineTo(x(i),y(v))}; drawPath(avg,Color(0xFFFFC107),style=Stroke(3f))
    points.forEachIndexed{i,p->drawCircle(colorFor(p.colorKey),if(selected?.pitchId==p.pitchId)9f else 6f,androidx.compose.ui.geometry.Offset(x(i),y(p.mph)))}
   }
  }
  selected?.let{Text("#${it.number} · ${it.label} · ${String.format("%.1f",it.mph)} MPH")}
 }
}
