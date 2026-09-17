package com.example.radarcamera.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.radarcamera.backup.*
import com.example.radarcamera.data.local.RadarDatabase
import com.example.radarcamera.security.PinStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(val busy:Boolean=false, val message:String?=null, val preview:BackupDocument?=null, val error:String?=null)

class BackupViewModel(private val context: Context, database: RadarDatabase, private val pinStore: PinStore): ViewModel() {
    private val exporter=BackupExporter(database); private val validator=BackupValidator(); private val restorer=BackupRestorer(context.applicationContext, database)
    private val _state=MutableStateFlow(BackupUiState()); val state=_state.asStateFlow()
    fun suggestedName()="RadarCamera_${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))}.radarbackup"
    fun export(uri:Uri)=viewModelScope.launch { _state.value=_state.value.copy(busy=true,error=null,message=null); try { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exporter.export().toJson()) } ?: error("No se pudo abrir el destino."); _state.value=BackupUiState(message="Respaldo creado correctamente.") } catch(e:Exception) { _state.value=BackupUiState(error=e.message?:"No se pudo crear el respaldo.") } }
    fun inspect(uri:Uri)=viewModelScope.launch { _state.value=_state.value.copy(busy=true,error=null); try { val text=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("No se pudo leer el archivo."); _state.value=BackupUiState(preview=validator.validate(text)) } catch(e:Exception) { _state.value=BackupUiState(error=e.message?:"El respaldo no es válido.") } }
    fun restore(pin:String)=viewModelScope.launch { val document=_state.value.preview ?: return@launch; if(!pinStore.verificarPin(pin)){_state.value=_state.value.copy(error="PIN incorrecto.");return@launch}; _state.value=_state.value.copy(busy=true,error=null); try{restorer.restore(document);_state.value=BackupUiState(message="Datos restaurados. El PIN no fue modificado.")}catch(e:Exception){_state.value=_state.value.copy(busy=false,error=e.message?:"No se pudo restaurar.")}}
    fun clearMessage(){_state.value=_state.value.copy(message=null,error=null)}
}
