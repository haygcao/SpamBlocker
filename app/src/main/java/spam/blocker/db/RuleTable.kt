package spam.blocker.db

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import kotlinx.serialization.Serializable
import spam.blocker.R
import spam.blocker.def.Def
import spam.blocker.util.Flag
import spam.blocker.util.Schedule
import spam.blocker.util.SpannableUtil


@Serializable
class PatternRule {

    var id: Long = 0
    var pattern: String = ""

    // for now, this is only used as ParticularNumber
    var patternExtra: String = ""

    var patternFlags = Flag(Def.FLAG_REGEX_IGNORE_CASE or Def.FLAG_REGEX_DOT_MATCH_ALL)
    var patternExtraFlags = Flag(Def.FLAG_REGEX_IGNORE_CASE or Def.FLAG_REGEX_DOT_MATCH_ALL)
    var description: String = ""
    var priority: Int = 1
    var isBlacklist = true
    var flagCallSms = Flag(Def.FLAG_FOR_BOTH_SMS_CALL) // it applies to SMS or Call or both
    var importance = Def.DEF_SPAM_IMPORTANCE // notification importance
    var schedule = ""
    var blockType = Def.DEF_BLOCK_TYPE

    override fun toString(): String {
        return "id: $id, pattern: $pattern, patternExtra: $patternExtra, patternFlags: $patternFlags, patternExtraFlags: $patternExtraFlags, desc: $description, priority: $priority, flagCallSms: $flagCallSms, isBlacklist: $isBlacklist, importance: $importance, schedule: $schedule, blockType: $blockType"
    }

    fun isForCall(): Boolean {
        return flagCallSms.Has(Def.FLAG_FOR_CALL)
    }

    fun patternStr(): String {
        return if (patternExtra != "")
            "$pattern   <-   $patternExtra"
        else
            pattern
    }
    fun patternStrColorful(ctx: Context): SpannableStringBuilder {
        val green = ctx.resources.getColor(R.color.dark_sea_green, null)
        val red = ctx.resources.getColor(R.color.salmon, null)
        val scheduleColor = ctx.resources.getColor(R.color.schedule, null)
        val flagsColor = Color.MAGENTA

        val ratioFlags = 0.9f

        val patternColor = if (isBlacklist) red else green

        val sb = SpannableStringBuilder()

        // 1. Time schedule
        val sch = Schedule.parseFromStr(schedule)
        if (sch.enabled)
            SpannableUtil.append(sb, sch.toDisplayStr(ctx) + "\n", scheduleColor, relativeSize = 0.8f)

        // 2. imdlc
        // format:
        //   imdl .*   <-   imdl particular.*
        val imdlc = patternFlags.toStr(Def.MAP_REGEX_FLAGS, Def.LIST_REGEX_FLAG_INVERSE)
        SpannableUtil.append(sb, if (imdlc.isEmpty()) "" else "$imdlc ", flagsColor, relativeSize = ratioFlags)

        // 3. Particular Number
        SpannableUtil.append(sb, pattern, patternColor, bold = true)
        if (patternExtra != "") {
            SpannableUtil.append(sb, "   <-   ", Color.LTGRAY)

            val imdlcEx = patternExtraFlags.toStr(Def.MAP_REGEX_FLAGS, Def.LIST_REGEX_FLAG_INVERSE)
            SpannableUtil.append(sb, if (imdlcEx.isEmpty()) "" else "$imdlcEx ", flagsColor, relativeSize = ratioFlags)
            SpannableUtil.append(sb, patternExtra, patternColor)
        }

        return sb
    }


    fun isForSms(): Boolean {
        return flagCallSms.Has(Def.FLAG_FOR_SMS)
    }

    fun isWhitelist(): Boolean {
        return !isBlacklist
    }

    fun setForCall(enabled: Boolean) {
        flagCallSms.set(Def.FLAG_FOR_CALL, enabled)
    }

    fun setForSms(enabled: Boolean) {
        flagCallSms.set(Def.FLAG_FOR_SMS, enabled)
    }
}


abstract class RuleTable {

    abstract fun tableName(): String

    @SuppressLint("Range")
    private fun listByFilter(ctx: Context, filterSql: String): List<PatternRule> {
        val ret: MutableList<PatternRule> = mutableListOf()

        val db = Db.getInstance(ctx).readableDatabase
        val cursor = db.rawQuery(filterSql, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val f = PatternRule()

                    f.id = it.getLong(it.getColumnIndex(Db.COLUMN_ID))
                    f.pattern = it.getString(it.getColumnIndex(Db.COLUMN_PATTERN))
                    f.patternExtra = it.getStringOrNull(it.getColumnIndex(Db.COLUMN_PATTERN_EXTRA)) ?: ""
                    f.patternFlags = Flag(it.getInt(it.getColumnIndex(Db.COLUMN_PATTERN_FLAGS)))
                    f.patternExtraFlags = Flag(it.getInt(it.getColumnIndex(Db.COLUMN_PATTERN_EXTRA_FLAGS)))
                    f.description = it.getString(it.getColumnIndex(Db.COLUMN_DESC))
                    f.priority = it.getInt(it.getColumnIndex(Db.COLUMN_PRIORITY))
                    f.isBlacklist = it.getInt(it.getColumnIndex(Db.COLUMN_IS_BLACK)) == 1
                    f.flagCallSms = Flag(it.getInt(it.getColumnIndex(Db.COLUMN_FLAG_CALL_SMS)))
                    f.importance = it.getIntOrNull(it.getColumnIndex(Db.COLUMN_IMPORTANCE)) ?: Def.DEF_SPAM_IMPORTANCE
                    f.schedule = it.getStringOrNull(it.getColumnIndex(Db.COLUMN_SCHEDULE)) ?: ""
                    f.blockType = it.getIntOrNull(it.getColumnIndex(Db.COLUMN_BLOCK_TYPE)) ?: Def.DEF_BLOCK_TYPE
                    ret += f
                } while (it.moveToNext())
            }
            return ret
        }
    }

    @SuppressLint("Range")
    fun findPatternRuleById(ctx: Context, id: Long): PatternRule? {
        val db = Db.getInstance(ctx).readableDatabase
        val sql = "SELECT * FROM ${tableName()} WHERE ${Db.COLUMN_ID} = $id"

        val cursor = db.rawQuery(sql, null)

        cursor.use {
            if (it.moveToFirst()) {
                val f = PatternRule()
                f.id = it.getLong(it.getColumnIndex(Db.COLUMN_ID))
                f.pattern = it.getString(it.getColumnIndex(Db.COLUMN_PATTERN))
                f.patternExtra = it.getStringOrNull(it.getColumnIndex(Db.COLUMN_PATTERN_EXTRA)) ?: ""
                f.patternFlags = Flag(it.getInt(it.getColumnIndex(Db.COLUMN_PATTERN_FLAGS)))
                f.patternExtraFlags = Flag(it.getInt(it.getColumnIndex(Db.COLUMN_PATTERN_EXTRA_FLAGS)))
                f.description = it.getString(it.getColumnIndex(Db.COLUMN_DESC))
                f.priority = it.getInt(it.getColumnIndex(Db.COLUMN_PRIORITY))
                f.isBlacklist = it.getInt(it.getColumnIndex(Db.COLUMN_IS_BLACK)) == 1
                f.flagCallSms = Flag(it.getInt(it.getColumnIndex(Db.COLUMN_FLAG_CALL_SMS)))
                f.importance = it.getIntOrNull(it.getColumnIndex(Db.COLUMN_IMPORTANCE)) ?: Def.DEF_SPAM_IMPORTANCE
                f.schedule = it.getStringOrNull(it.getColumnIndex(Db.COLUMN_SCHEDULE)) ?: ""
                f.blockType = it.getIntOrNull(it.getColumnIndex(Db.COLUMN_BLOCK_TYPE)) ?: Def.DEF_BLOCK_TYPE

                return f
            } else {
                return null
            }
        }
    }

    fun listAll(ctx: Context): List<PatternRule> {
        return listRules(ctx, Def.FLAG_FOR_BOTH_SMS_CALL)
    }

    fun listRules(
        ctx: Context,
        flagCallSms: Int
    ): List<PatternRule> {
        val where = arrayListOf<String>()

        // call/sms
        val sms = Flag(flagCallSms).Has(Def.FLAG_FOR_SMS)
        val call = Flag(flagCallSms).Has(Def.FLAG_FOR_CALL)
        if (sms and !call) {
            where.add("(${Db.COLUMN_FLAG_CALL_SMS} & ${Def.FLAG_FOR_SMS}) = ${Def.FLAG_FOR_SMS}")
        } else if (call and !sms) {
            where.add("(${Db.COLUMN_FLAG_CALL_SMS} & ${Def.FLAG_FOR_CALL}) = ${Def.FLAG_FOR_CALL}")
        }

        // build where clause
        var whereStr = ""
        if (where.size > 0) {
            whereStr = " WHERE (${where.joinToString(separator = " and ") { "($it)" }})"
        }

        val sql =
            "SELECT * FROM ${tableName()} $whereStr ORDER BY ${Db.COLUMN_PRIORITY} DESC, ${Db.COLUMN_DESC} ASC, ${Db.COLUMN_PATTERN} ASC"

//        Log.d(Def.TAG, sql)

        return listByFilter(ctx, sql)
    }

    fun addNewRule(ctx: Context, f: PatternRule): Long {
        val db = Db.getInstance(ctx).writableDatabase
        val cv = ContentValues()
        cv.put(Db.COLUMN_PATTERN, f.pattern)
        cv.put(Db.COLUMN_PATTERN_EXTRA, f.patternExtra)
        cv.put(Db.COLUMN_PATTERN_FLAGS, f.patternFlags.value)
        cv.put(Db.COLUMN_PATTERN_EXTRA_FLAGS, f.patternExtraFlags.value)
        cv.put(Db.COLUMN_DESC, f.description)
        cv.put(Db.COLUMN_PRIORITY, f.priority)
        cv.put(Db.COLUMN_FLAG_CALL_SMS, f.flagCallSms.value)
        cv.put(Db.COLUMN_IS_BLACK, if (f.isBlacklist) 1 else 0)
        cv.put(Db.COLUMN_IMPORTANCE, f.importance)
        cv.put(Db.COLUMN_SCHEDULE, f.schedule)
        cv.put(Db.COLUMN_BLOCK_TYPE, f.blockType)

        return db.insert(tableName(), null, cv)
    }

    fun addRuleWithId(ctx: Context, f: PatternRule) {
        val db = Db.getInstance(ctx).writableDatabase
        val cv = ContentValues()
        cv.put(Db.COLUMN_ID, f.id)
        cv.put(Db.COLUMN_PATTERN, f.pattern)
        cv.put(Db.COLUMN_PATTERN_EXTRA, f.patternExtra)
        cv.put(Db.COLUMN_PATTERN_FLAGS, f.patternFlags.value)
        cv.put(Db.COLUMN_PATTERN_EXTRA_FLAGS, f.patternExtraFlags.value)
        cv.put(Db.COLUMN_DESC, f.description)
        cv.put(Db.COLUMN_PRIORITY, f.priority)
        cv.put(Db.COLUMN_FLAG_CALL_SMS, f.flagCallSms.value)
        cv.put(Db.COLUMN_IS_BLACK, if (f.isBlacklist) 1 else 0)
        cv.put(Db.COLUMN_IMPORTANCE, f.importance)
        cv.put(Db.COLUMN_SCHEDULE, f.schedule)
        cv.put(Db.COLUMN_BLOCK_TYPE, f.blockType)

        db.insert(tableName(), null, cv)
    }

    fun updateRuleById(ctx: Context, id: Long, f: PatternRule): Boolean {
        val db = Db.getInstance(ctx).writableDatabase
        val cv = ContentValues()
        cv.put(Db.COLUMN_PATTERN, f.pattern)
        cv.put(Db.COLUMN_PATTERN_EXTRA, f.patternExtra)
        cv.put(Db.COLUMN_PATTERN_FLAGS, f.patternFlags.value)
        cv.put(Db.COLUMN_PATTERN_EXTRA_FLAGS, f.patternExtraFlags.value)
        cv.put(Db.COLUMN_DESC, f.description)
        cv.put(Db.COLUMN_PRIORITY, f.priority)
        cv.put(Db.COLUMN_FLAG_CALL_SMS, f.flagCallSms.value)
        cv.put(Db.COLUMN_IS_BLACK, if (f.isBlacklist) 1 else 0)
        cv.put(Db.COLUMN_IMPORTANCE, f.importance)
        cv.put(Db.COLUMN_SCHEDULE, f.schedule)
        cv.put(Db.COLUMN_BLOCK_TYPE, f.blockType)

        return db.update(tableName(), cv, "${Db.COLUMN_ID} = $id", null) >= 0
    }

    fun delById(ctx: Context, id: Long): Boolean {
        val db = Db.getInstance(ctx).writableDatabase
        val sql = "DELETE FROM ${tableName()} WHERE ${Db.COLUMN_ID} = $id"
        val cursor = db.rawQuery(sql, null)

        return cursor.use {
            it.moveToFirst()
        }
    }

    fun clearAll(ctx: Context) {
        val db = Db.getInstance(ctx).writableDatabase
        val sql = "DELETE FROM ${tableName()}"
        db.execSQL(sql)
    }
}

open class NumberRuleTable : RuleTable() {
    override fun tableName(): String {
        return Db.TABLE_NUMBER_RULE
    }
}

open class ContentRuleTable : RuleTable() {
    override fun tableName(): String {
        return Db.TABLE_CONTENT_RULE
    }
}

open class QuickCopyRuleTable : RuleTable() {
    override fun tableName(): String {
        return Db.TABLE_QUICK_COPY_RULE
    }
}
