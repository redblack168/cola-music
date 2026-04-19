package com.colamusic.core.lyrics.normalize

import android.content.Context
import com.colamusic.core.common.Logx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.atomic.AtomicReference

/**
 * Traditional → Simplified character-level converter.
 *
 * Loads a bundled OpenCC-style char dict at first use from assets/opencc/t2s_char.json
 * (one-to-one char map JSON: `{"愛":"爱","學":"学",...}`). Falls back to a small
 * built-in seed table if the asset is missing so the app still runs offline.
 *
 * Phrase-level folding (for cases like 默認 → 默认 vs 默认) is handled by the seed
 * + asset dict; longer-gram phrase dict can be added under assets/opencc/t2s_phrase.json
 * in phase 2 without code changes (loader will merge with priority).
 */
object OpenCCConverter {
    private val table = AtomicReference<Map<Char, Char>>(emptyMap())
    private val phraseTable = AtomicReference<Map<String, String>>(emptyMap())
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val chars = HashMap<Char, Char>(2048).apply { putAll(SEED_CHARS) }
            try {
                context.assets.open("opencc/t2s_char.json").use { input ->
                    val text = input.bufferedReader().readText()
                    val obj = Json.parseToJsonElement(text).jsonObject
                    obj.forEach { (k, v) ->
                        if (k.length == 1) {
                            val target = (v as? JsonPrimitive)?.jsonPrimitive?.content
                            if (!target.isNullOrEmpty()) chars[k[0]] = target[0]
                        }
                    }
                }
            } catch (_: Exception) {
                Logx.w("opencc", "t2s_char.json asset missing; using seed (${SEED_CHARS.size} chars)")
            }
            try {
                context.assets.open("opencc/t2s_phrase.json").use { input ->
                    val text = input.bufferedReader().readText()
                    val obj = Json.parseToJsonElement(text).jsonObject
                    val map = HashMap<String, String>(obj.size)
                    obj.forEach { (k, v) ->
                        val target = (v as? JsonPrimitive)?.jsonPrimitive?.content
                        if (!target.isNullOrEmpty()) map[k] = target
                    }
                    phraseTable.set(map)
                }
            } catch (_: Exception) {
                // phrase dict is optional
            }
            table.set(chars)
            loaded = true
            Logx.i("opencc", "loaded ${chars.size} chars, ${phraseTable.get().size} phrases")
        }
    }

    fun t2s(input: String): String {
        if (input.isEmpty()) return input
        val phrases = phraseTable.get()
        val pre = if (phrases.isEmpty()) input else applyPhrases(input, phrases)
        val chars = table.get()
        if (chars.isEmpty()) return pre
        val sb = StringBuilder(pre.length)
        for (c in pre) sb.append(chars[c] ?: c)
        return sb.toString()
    }

    private fun applyPhrases(input: String, phrases: Map<String, String>): String {
        var out = input
        for ((k, v) in phrases) if (out.contains(k)) out = out.replace(k, v)
        return out
    }

    /** Compact seed table for ~100 high-frequency trad chars that appear in pop titles.
     *  Full OpenCC asset will expand this at build time. */
    private val SEED_CHARS: Map<Char, Char> = buildMap {
        val src = "愛學國語經實對話點點萬個處開關間時記憶戰爭處裡應當樂樂顯現夢裡飄隨風雨雲電見聞雙節氣愛發揚臉紅綠藍傳記詞曲劇場場屆頭腦兒風聲張楊難過輕離開關話談設計條號碼編號終結總體"
        val dst = "爱学国语经实对话点点万个处开关间时记忆战争处里应当乐乐显现梦里飘随风雨云电见闻双节气爱发扬脸红绿蓝传记词曲剧场场届头脑儿风声张杨难过轻离开关话谈设计条号码编号终结总体"
        for (i in src.indices) if (i < dst.length && src[i] != dst[i]) put(src[i], dst[i])
        // high-frequency additions
        mapOf(
            '們' to '们', '這' to '这', '產' to '产', '樣' to '样', '請' to '请', '號' to '号',
            '無' to '无', '還' to '还', '讓' to '让', '過' to '过', '會' to '会', '麼' to '么',
            '為' to '为', '來' to '来', '時' to '时', '個' to '个', '實' to '实', '長' to '长',
            '體' to '体', '東' to '东', '見' to '见', '關' to '关', '間' to '间', '難' to '难',
            '從' to '从', '現' to '现', '聽' to '听', '說' to '说', '書' to '书', '車' to '车',
            '業' to '业', '貝' to '贝', '員' to '员', '頭' to '头', '專' to '专', '處' to '处',
            '變' to '变', '銀' to '银', '機' to '机', '選' to '选', '樹' to '树', '總' to '总',
            '門' to '门', '問' to '问', '語' to '语', '話' to '话', '點' to '点', '歲' to '岁',
            '舊' to '旧', '與' to '与', '豐' to '丰', '農' to '农', '麗' to '丽', '獨' to '独',
            '憶' to '忆', '戀' to '恋', '離' to '离', '開' to '开', '結' to '结', '終' to '终',
            '夢' to '梦', '藥' to '药', '錯' to '错', '邊' to '边', '場' to '场', '媽' to '妈',
            '覺' to '觉', '愛' to '爱', '發' to '发', '頂' to '顶', '極' to '极', '氣' to '气',
            '淚' to '泪', '淚' to '泪', '腦' to '脑', '飛' to '飞', '劍' to '剑', '彈' to '弹',
            '歸' to '归', '兒' to '儿', '給' to '给', '許' to '许', '遠' to '远', '靜' to '静',
            '麵' to '面', '過' to '过', '畢' to '毕', '繼' to '继', '紀' to '纪', '絕' to '绝',
            '線' to '线', '網' to '网', '視' to '视', '親' to '亲', '頓' to '顿', '強' to '强',
            '確' to '确', '認' to '认', '條' to '条', '質' to '质', '際' to '际', '醜' to '丑',
            '級' to '级', '歷' to '历', '麗' to '丽', '劇' to '剧', '錄' to '录', '華' to '华',
        ).forEach { (k, v) -> put(k, v) }
    }
}
