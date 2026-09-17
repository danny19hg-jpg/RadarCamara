package com.example.radarcamera.backup

import com.example.radarcamera.data.local.*
import com.example.radarcamera.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.ZoneId

const val BACKUP_FORMAT_VERSION = 1

data class BackupManifest(val formatVersion: Int, val appVersion: String, val roomSchemaVersion: Int, val exportedAt: Long, val zoneId: String, val players: Int, val sessions: Int, val pitches: Int, val checksum: String)
data class BackupDocument(val manifest: BackupManifest, val coaches: List<CoachEntity>, val players: List<PlayerEntity>, val sessions: List<SessionEntity>, val pitches: List<PitchEntity>) {
    fun canonicalJson(): String = JSONObject().apply {
        put("formatVersion", BACKUP_FORMAT_VERSION); put("coaches", coaches.sortedBy { it.id }.toJsonArray { it.toJson() }); put("players", players.sortedBy { it.id }.toJsonArray { it.toJson() }); put("sessions", sessions.sortedBy { it.id }.toJsonArray { it.toJson() }); put("pitches", pitches.sortedBy { it.id }.toJsonArray { it.toJson() })
    }.toString()
    fun toJson(): String = JSONObject().apply {
        put("manifest", JSONObject().apply { put("formatVersion", manifest.formatVersion); put("appVersion", manifest.appVersion); put("roomSchemaVersion", manifest.roomSchemaVersion); put("exportedAt", manifest.exportedAt); put("zoneId", manifest.zoneId); put("players", manifest.players); put("sessions", manifest.sessions); put("pitches", manifest.pitches); put("checksum", manifest.checksum) })
        put("payload", JSONObject(canonicalJson()))
    }.toString()
    companion object {
        fun create(appVersion: String, coaches: List<CoachEntity>, players: List<PlayerEntity>, sessions: List<SessionEntity>, pitches: List<PitchEntity>, now: Long): BackupDocument {
            val draft = BackupDocument(BackupManifest(BACKUP_FORMAT_VERSION, appVersion, 7, now, ZoneId.systemDefault().id, players.size, sessions.size, pitches.size, ""), coaches, players, sessions, pitches)
            return draft.copy(manifest = draft.manifest.copy(checksum = sha256(draft.canonicalJson())))
        }
        fun parse(text: String): BackupDocument {
            val root = JSONObject(text); val manifest = root.getJSONObject("manifest"); val payload = root.getJSONObject("payload")
            val version = manifest.getInt("formatVersion"); require(version <= BACKUP_FORMAT_VERSION) { "El respaldo usa un formato más nuevo." }; require(payload.getInt("formatVersion") == version) { "Formato de respaldo inconsistente." }
            fun <T> read(name: String, mapper: (JSONObject) -> T) = payload.getJSONArray(name).mapTyped(mapper)
            val doc = BackupDocument(BackupManifest(version, manifest.getString("appVersion"), manifest.getInt("roomSchemaVersion"), manifest.getLong("exportedAt"), manifest.getString("zoneId"), manifest.getInt("players"), manifest.getInt("sessions"), manifest.getInt("pitches"), manifest.getString("checksum")), read("coaches", ::coachFromJson), read("players", ::playerFromJson), read("sessions", ::sessionFromJson), read("pitches", ::pitchFromJson))
            require(sha256(doc.canonicalJson()) == doc.manifest.checksum) { "El checksum del respaldo no coincide." }
            return doc
        }
    }
}

internal fun BackupDocument.validate() { require(manifest.formatVersion == BACKUP_FORMAT_VERSION) { "Formato de respaldo no compatible." }; require(players.map { it.id }.distinct().size == players.size) { "Hay jugadores duplicados." }; require(sessions.all { it.playerId in players.map { p -> p.id } }) { "Una sesión no tiene jugador." }; require(pitches.all { it.sessionId in sessions.map { s -> s.id } }) { "Un lanzamiento no tiene sesión." } }
private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject) = JSONArray().also { array -> forEach { array.put(mapper(it)) } }
private fun <T> JSONArray.mapTyped(mapper: (JSONObject) -> T): List<T> = (0 until length()).map { mapper(getJSONObject(it)) }
private fun CoachEntity.toJson() = JSONObject().apply { put("id",id);put("name",name);put("academy",academy);put("country",country);put("createdAt",createdAt);put("updatedAt",updatedAt);put("revision",revision);put("localProfile",localProfile) }
private fun PlayerEntity.toJson() = JSONObject().apply { put("id",id);put("name",name);put("birthDate",birthDate);put("sport",sport.name);put("position",position);put("throwingHand",throwingHand.name);put("heightCm",heightCm);put("weightKg",weightKg);put("category",category);put("teamAcademy",teamAcademy);put("createdAt",createdAt);put("updatedAt",updatedAt);put("revision",revision);put("archivedAt",archivedAt) }
private fun SessionEntity.toJson() = JSONObject().apply { put("id",id);put("playerId",playerId);put("sport",sport.name);put("initialPitchType",initialPitchType.name);put("currentPitchType",currentPitchType.name);put("target",target);put("recordingEnabled",recordingEnabled);put("startedAt",startedAt);put("endedAt",endedAt);put("discardedAt",discardedAt);put("createdAt",createdAt);put("updatedAt",updatedAt);put("revision",revision) }
private fun PitchEntity.toJson() = JSONObject().apply { put("id",id);put("sessionId",sessionId);put("eventId",eventId);put("number",number);put("mph",mph);put("pitchType",pitchType.name);put("receivedAt",receivedAt);put("videoStatus",videoStatus.name);put("videoUri",videoUri);put("videoReason",videoReason);put("videoRawPath",videoRawPath);put("videoRelativePath",videoRelativePath);put("videoDisplayName",videoDisplayName) }
private fun JSONObject.stringOrNull(name:String)=if(isNull(name)) null else getString(name)
private fun JSONObject.longOrNull(name:String)=if(isNull(name)) null else getLong(name)
private fun JSONObject.doubleOrNull(name:String)=if(isNull(name)) null else getDouble(name)
private fun coachFromJson(j:JSONObject)=CoachEntity(j.getString("id"),j.getString("name"),j.stringOrNull("academy"),j.stringOrNull("country"),j.getLong("createdAt"),j.getLong("updatedAt"),j.getLong("revision"),j.getInt("localProfile"))
private fun playerFromJson(j:JSONObject)=PlayerEntity(j.getString("id"),j.getString("name"),j.getString("birthDate"),Sport.valueOf(j.getString("sport")),j.getString("position"),ThrowingHand.valueOf(j.getString("throwingHand")),j.doubleOrNull("heightCm"),j.doubleOrNull("weightKg"),j.stringOrNull("category").orEmpty(),j.stringOrNull("teamAcademy"),j.getLong("createdAt"),j.getLong("updatedAt"),j.getLong("revision"),j.longOrNull("archivedAt"))
private fun sessionFromJson(j:JSONObject)=SessionEntity(j.getString("id"),j.getString("playerId"),Sport.valueOf(j.getString("sport")),PitchType.valueOf(j.getString("initialPitchType")),PitchType.valueOf(j.getString("currentPitchType")),j.getInt("target"),j.getBoolean("recordingEnabled"),j.getLong("startedAt"),j.longOrNull("endedAt"),j.longOrNull("discardedAt"),j.getLong("createdAt"),j.getLong("updatedAt"),j.getLong("revision"))
private fun pitchFromJson(j:JSONObject)=PitchEntity(j.getString("id"),j.getString("sessionId"),j.getLong("eventId"),j.getInt("number"),j.getDouble("mph"),PitchType.valueOf(j.getString("pitchType")),j.getLong("receivedAt"),PitchVideoStatus.valueOf(j.getString("videoStatus")),j.stringOrNull("videoUri"),j.stringOrNull("videoReason"),j.stringOrNull("videoRawPath"),j.stringOrNull("videoRelativePath"),j.stringOrNull("videoDisplayName"))
