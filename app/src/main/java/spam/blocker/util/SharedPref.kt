package spam.blocker.util.SharedPref

import android.content.Context
import android.content.SharedPreferences
import spam.blocker.def.Def

open class SharedPref(private val ctx: Context) {
    private val prefs: SharedPreferences = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "settings"
    }

    fun readString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }
    fun writeString(key: String, value: String) {
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun readInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }
    fun writeInt(key: String, value: Int) {
        val editor = prefs.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun readLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }
    fun writeLong(key: String, value: Long) {
        val editor = prefs.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun readBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    fun writeBoolean(key: String, value: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun clear() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
//    fun exists(): Boolean {
//        val file = File(ctx.filesDir, "$PREFS_NAME.xml")
//        return file.exists()
//    }
}

class Temporary(ctx: Context) : SharedPref(ctx) {
    fun getLastCallToBlock() : Pair<String, Long> {
        return Pair(
            readString(Def.LAST_NUMBER_TO_BLOCK, ""),
            readLong(Def.LAST_CALLED_TIME, 0)
        )
    }
    fun setLastCallToBlock(number: String, timestamp: Long) {
        writeString(Def.LAST_NUMBER_TO_BLOCK, number)
        writeLong(Def.LAST_CALLED_TIME, timestamp)
    }
}
class Global(ctx: Context) : SharedPref(ctx) {
    fun isGloballyEnabled(): Boolean { return readBoolean(Def.SETTING_ENABLED, false) }
    fun setGloballyEnabled(enabled: Boolean) { writeBoolean(Def.SETTING_ENABLED, enabled) }
    fun toggleGloballyEnabled() { writeBoolean(Def.SETTING_ENABLED, !isGloballyEnabled()) }
    fun isCallEnabled(): Boolean { return readBoolean(Def.SETTING_CALL_ENABLED, true) }
    fun setCallEnabled(enabled: Boolean) { writeBoolean(Def.SETTING_CALL_ENABLED, enabled) }
    fun isSmsEnabled(): Boolean { return readBoolean(Def.SETTING_SMS_ENABLED, true) }
    fun setSmsEnabled(enabled: Boolean) { writeBoolean(Def.SETTING_SMS_ENABLED, enabled) }

    fun getThemeType(): Int { return readInt(Def.SETTING_THEME_TYPE, 0) }
    fun setThemeType(type: Int) { writeInt(Def.SETTING_THEME_TYPE, type) }

    fun getLanguage(): String { return readString(Def.SETTING_LANGUAGE, "en") }
    fun setLanguage(lang: String) { writeString(Def.SETTING_LANGUAGE, lang) }

    fun getActiveTab(): String { return readString(Def.SETTING_ACTIVE_TAB, "") }
    fun setActiveTab(tab: String) { writeString(Def.SETTING_ACTIVE_TAB, tab) }

    fun hasAskedForAllPermissions(): Boolean {
        return readBoolean(Def.SETTING_REQUIRE_PERMISSION_ONCE, false)
    }
    fun setAskedForAllPermission() {
        writeBoolean(Def.SETTING_REQUIRE_PERMISSION_ONCE, true)
    }
    fun hasPromptedForRunningInWorkProfile(): Boolean {
        return readBoolean(Def.SETTING_WARN_RUNNING_IN_WORK_PROFILE_ONCE, false)
    }
    fun setPromptedForRunningInWorkProfile() {
        writeBoolean(Def.SETTING_WARN_RUNNING_IN_WORK_PROFILE_ONCE, true)
    }

    fun getShowPassed(): Boolean {
        return readBoolean(Def.SETTING_SHOW_PASSED, true)
    }
    fun setShowPassed(enabled: Boolean) {
        writeBoolean(Def.SETTING_SHOW_PASSED, enabled)
    }
    fun getShowBlocked(): Boolean {
        return readBoolean(Def.SETTING_SHOW_BLOCKED, true)
    }
    fun setShowBlocked(enabled: Boolean) {
        writeBoolean(Def.SETTING_SHOW_BLOCKED, enabled)
    }
}


class Stir(ctx: Context) : SharedPref(ctx) {
    fun isEnabled(): Boolean {
        return readBoolean(Def.SETTING_STIR_ENABLED, false)
    }
    fun setEnabled(enabled: Boolean) {
        writeBoolean(Def.SETTING_STIR_ENABLED, enabled)
    }
    fun toggleEnabled() {
        setEnabled(!isEnabled())
    }
    fun isExclusive() : Boolean {
        return readBoolean(Def.SETTING_STIR_EXCLUSIVE, false)
    }
    fun setExclusive(exclusive: Boolean) {
        writeBoolean(Def.SETTING_STIR_EXCLUSIVE, exclusive)
    }
    fun toggleExclusive() {
        setExclusive(!isExclusive())
    }
    fun isIncludeUnverified() : Boolean {
        return readBoolean(Def.SETTING_STIR_INCLUDE_UNVERIFIED, false)
    }
    fun setIncludeUnverified(include: Boolean) {
        writeBoolean(Def.SETTING_STIR_INCLUDE_UNVERIFIED, include)
    }
}
class Contact(ctx: Context) : SharedPref(ctx) {
    fun isEnabled(): Boolean {
        return readBoolean(Def.SETTING_CONTACT_ENABLED, false)
    }
    fun setEnabled(enabled: Boolean) {
        writeBoolean(Def.SETTING_CONTACT_ENABLED, enabled)
    }
    fun isExclusive() : Boolean {
        return readBoolean(Def.SETTING_CONTACTS_EXCLUSIVE, false)
    }
    fun setExclusive(exclusive: Boolean) {
        writeBoolean(Def.SETTING_CONTACTS_EXCLUSIVE, exclusive)
    }
    fun toggleExclusive() {
        setExclusive(!isExclusive())
    }
}
class RepeatedCall(ctx: Context) : SharedPref(ctx) {
    fun setEnabled(enabled: Boolean) {
        writeBoolean(Def.SETTING_PERMIT_REPEATED, enabled)
    }
    fun isEnabled(): Boolean {
        return readBoolean(Def.SETTING_PERMIT_REPEATED, false)
    }
    fun getConfig(): Pair<Int, Int> {
        val times = readInt(Def.SETTING_REPEATED_TIMES, 1)
        val inXMin = readInt(Def.SETTING_REPEATED_IN_X_MIN, 5)
        return Pair(times, inXMin)
    }
    fun setConfig(times: Int, inXMin: Int) {
        writeInt(Def.SETTING_REPEATED_TIMES, times)
        writeInt(Def.SETTING_REPEATED_IN_X_MIN, inXMin)
    }
}

class Dialed(ctx: Context) : SharedPref(ctx) {
    fun setEnabled(enabled: Boolean) {
        writeBoolean(Def.SETTING_PERMIT_DIALED, enabled)
    }
    fun isEnabled(): Boolean {
        return readBoolean(Def.SETTING_PERMIT_DIALED, false)
    }
    fun getConfig(): Int {
        return readInt(Def.SETTING_DIALED_IN_X_DAY, 3)
    }
    fun setConfig(inXDay: Int) {
        writeInt(Def.SETTING_DIALED_IN_X_DAY, inXDay)
    }
}

class OffTime(ctx: Context) : SharedPref(ctx) {
    fun isEnabled(): Boolean {
        return readBoolean(Def.SETTING_ENABLE_OFF_TIME, false)
    }
    fun setEnabled(enabled: Boolean) {
        writeBoolean(Def.SETTING_ENABLE_OFF_TIME, enabled)
    }
    fun setStart(hour: Int, min: Int) {
        writeInt(Def.SETTING_OFF_TIME_START_HOUR, hour)
        writeInt(Def.SETTING_OFF_TIME_START_MIN, min)
    }
    fun setEnd(hour: Int, min: Int) {
        writeInt(Def.SETTING_OFF_TIME_END_HOUR, hour)
        writeInt(Def.SETTING_OFF_TIME_END_MIN, min)
    }
    fun getStart(): Pair<Int, Int> {
        return Pair(
            readInt(Def.SETTING_OFF_TIME_START_HOUR, 0),
            readInt(Def.SETTING_OFF_TIME_START_MIN, 0)
        )
    }
    fun getEnd(): Pair<Int, Int> {
        return Pair(
            readInt(Def.SETTING_OFF_TIME_END_HOUR, 0),
            readInt(Def.SETTING_OFF_TIME_END_MIN, 0)
        )
    }
}
class BlockType(ctx: Context) : SharedPref(ctx) {
    fun setType(type: Int) {
        writeInt(Def.SETTING_BLOCK_TYPE, type)
    }
    fun getType(): Int {
        return readInt(Def.SETTING_BLOCK_TYPE, Def.DEF_BLOCK_TYPE)
    }
}
class RecentApps(ctx: Context) : SharedPref(ctx) {
    fun getList(): List<String> {
        val s = readString(Def.SETTING_RECENT_APPS, "")
        if (s == "") {
            return listOf()
        }
        return s.split(",")
    }
    fun setList(list: List<String>) {
        writeString(Def.SETTING_RECENT_APPS, list.joinToString(","))
    }
    fun getConfig() : Int {
        return readInt(Def.SETTING_RECENT_APP_IN_X_MIN, 5)
    }
    fun setConfig(inXMin : Int) {
        writeInt(Def.SETTING_RECENT_APP_IN_X_MIN, inXMin)
    }
}

