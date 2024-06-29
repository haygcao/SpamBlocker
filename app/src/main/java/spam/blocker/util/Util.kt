package spam.blocker.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.util.DisplayMetrics
import android.view.WindowManager
import spam.blocker.R
import spam.blocker.def.Def
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Util {
    companion object {
        fun fullDateString(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd\nHH:mm", Locale.getDefault())
            val date = Date(timestamp)
            return dateFormat.format(date)
        }

        fun hourMin(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = Date(timestamp)
            return dateFormat.format(date)
        }

        fun isToday(timestamp: Long): Boolean {
            val calendar = Calendar.getInstance()
            val currentDate = calendar.get(Calendar.DAY_OF_MONTH)
            calendar.timeInMillis = timestamp
            val date = calendar.get(Calendar.DAY_OF_MONTH)
            return currentDate == date
        }

        fun getDayOfWeek(ctx: Context, timestamp: Long): String {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysArray = ctx.resources.getStringArray(R.array.week).asList()
            return daysArray[dayOfWeek - 1]
        }

        fun isWithinAWeek(timestamp: Long): Boolean {
            val currentTimeMillis = System.currentTimeMillis()
            val difference = currentTimeMillis - timestamp
            val millisecondsInWeek = 7 * 24 * 60 * 60 * 1000 // 7 days in milliseconds
            return difference <= millisecondsInWeek
        }

        // check if a string only contains:
        //   digits spaces + - ( )
        val pattern = "^[0-9\\s+\\-()]*\$".toRegex()
        fun clearNumber(number: String): String {
            // check if it contains alphabetical characters like "Microsoft"
            if (!pattern.matches(number)) { // don't clear for enterprise string number
                return number
            }

            return number
                .trimStart('0') // remove leading "0"s
                .replace("-", "")
                .replace("+", "")
                .replace(" ", "")
                .replace("(", "")
                .replace(")", "")
        }

        fun formatTimeRange(
//            ctx: Context,
            stHour: Int, stMin: Int, etHour: Int, etMin: Int
        ): String {
            // not use fmt12h, "xx:xx AM - yy:yy PM" is too wide
//            val fmt24h = DateFormat.is24HourFormat(ctx)
//            if (fmt24h) {
                val startTime = String.format("%02d:%02d", stHour, stMin)
                val endTime = String.format("%02d:%02d", etHour, etMin)
                return "$startTime - $endTime"
//            } else {
//                val startTime = String.format(
//                    "%02d:%02d %s",
//                    if (stHour == 0 || stHour == 12) 12 else stHour % 12,
//                    stMin,
//                    if (stHour < 12) "AM" else "PM"
//                )
//                val endTime = String.format(
//                    "%02d:%02d %s",
//                    if (etHour == 0 || etHour == 12) 12 else etHour % 12,
//                    etMin,
//                    if (etHour < 12) "AM" else "PM"
//                )
//                return "$startTime - $endTime"
//            }
        }

        fun currentHourMin(): Pair<Int, Int> {
            val calendar = Calendar.getInstance()
            val currHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currMinute = calendar.get(Calendar.MINUTE)
            return Pair(currHour, currMinute)
        }
        fun isCurrentTimeWithinRange(stHour: Int, stMin: Int, etHour: Int, etMin: Int): Boolean {
            val (currHour, currMinute) = currentHourMin()
            val curr = currHour * 60 + currMinute

            val rangeStart = stHour * 60 + stMin
            val rangeEnd = etHour * 60 + etMin

            return if (rangeStart <= rangeEnd) {
                curr in rangeStart.. rangeEnd
            } else {
                curr >= rangeStart || curr <= rangeEnd
            }
        }

        private fun isRegexValid(regex: String): Boolean {
            return try {
                Regex(regex)
                true
            } catch (e: Exception) {
                false
            }
        }

        fun validateRegex(ctx: Context, regexStr: String) : String? {
            var s = regexStr
            if (s.isNotEmpty() && s.trim() != s)
                return ctx.getString(R.string.pattern_contain_invisible_characters)

            if (!isRegexValid(s))
                return ctx.getString(R.string.invalid_regex_pattern)

            if (s.startsWith("^"))
                s = s.substring(1)

            if (s.startsWith("+") || s.startsWith("\\+"))
                return ctx.getString(R.string.pattern_contain_leading_plus) + " " + ctx.getString(R.string.check_balloon_for_explanation)

            if (s.startsWith("0")) {
                return ctx.getString(R.string.pattern_contain_leading_zeroes) + " " + ctx.getString(R.string.check_balloon_for_explanation)
            }

            return null
        }

        fun flagsToRegexOptions(flags: Flag) : Set<RegexOption> {
            val opts = mutableSetOf<RegexOption>()
            if (flags.Has(Def.FLAG_REGEX_IGNORE_CASE)) {
                opts.add(RegexOption.IGNORE_CASE)
            }
            if (flags.Has(Def.FLAG_REGEX_MULTILINE)) {
                opts.add(RegexOption.MULTILINE)
            }
            if (flags.Has(Def.FLAG_REGEX_DOT_MATCH_ALL)) {
                opts.add(RegexOption.DOT_MATCHES_ALL)
            }
            if (flags.Has(Def.FLAG_REGEX_LITERAL)) {
                opts.add(RegexOption.LITERAL)
            }
            return opts

        }


        fun isInt(str: String): Boolean {
            return str.toIntOrNull() != null
        }

        private var cacheAppList : List<AppInfo>? = null
        private val lock_1 = Any()
        @SuppressLint("UseCompatLoadingForDrawables")
        fun listApps(ctx: Context): List<AppInfo> {
            synchronized(lock_1) {
                if (cacheAppList == null) {
                    val pm = ctx.packageManager

                    val packageInfos = getPackagesHoldingPermissions(pm, arrayOf(Manifest.permission.INTERNET))

                    cacheAppList = packageInfos.filter {
                        (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                    }.map {
                        val appInfo = it.applicationInfo

                        AppInfo().apply {
                            pkgName = it.packageName
                            label = appInfo.loadLabel(pm).toString()
                            icon = appInfo.loadIcon(pm)
                        }
                    }
                }
            }

            return cacheAppList!!
        }
        fun clearAppsCache() {
            synchronized(lock_1) {
                cacheAppList = null
            }
        }
        private fun getPackagesHoldingPermissions(pm: PackageManager, permissions: Array<String>): List<PackageInfo> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackagesHoldingPermissions(permissions, PackageManager.PackageInfoFlags.of(0L))
            } else {
                pm.getPackagesHoldingPermissions(permissions, 0)
            }
        }

        fun isPackageInstalled(ctx: Context, pkgName: String): Boolean {
            val pm = ctx.packageManager
            val flags = 0
            return try {
                pm.getPackageUid(pkgName, flags)
                true
            } catch (_: Exception) {
                false
            }
        }

        fun setLocale(ctx: Context, languageCode: String) {
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val resources = ctx.resources
            val configuration = resources.configuration
            configuration.setLocale(locale)

            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        fun getScreenHeight(ctx: Context) : Int {
            val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            return metrics.heightPixels
        }

        fun isRunningInWorkProfile(ctx: Context) : Boolean {
            val um = ctx.getSystemService(Context.USER_SERVICE) as UserManager

            return if (Build.VERSION.SDK_INT >= Def.ANDROID_11) { // android 10 ignored
                um.isManagedProfile
            } else
                false
        }
    }
}