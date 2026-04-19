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
 * (one-to-one char map JSON: `{"愛":"爱","學":"学",...}`). Falls back to a
 * built-in seed table so the app still runs offline and unit tests work on the
 * JVM without an Android context.
 *
 * Phrase-level folding (for cases like 默認 → 默认 vs 默认) is handled by the seed
 * + asset dict; longer-gram phrase dict can be added under assets/opencc/t2s_phrase.json
 * without code changes (loader merges with priority).
 */
object OpenCCConverter {

    /** Compact seed table for ~150 high-frequency trad chars that appear in pop
     *  titles. Always active (even before [ensureLoaded] runs). */
    val SEED_CHARS: Map<Char, Char> = buildMap {
        mapOf(
            '愛' to '爱', '學' to '学', '國' to '国', '語' to '语', '經' to '经',
            '實' to '实', '對' to '对', '話' to '话', '點' to '点', '萬' to '万',
            '處' to '处', '開' to '开', '關' to '关', '間' to '间', '時' to '时',
            '記' to '记', '憶' to '忆', '戰' to '战', '爭' to '争', '裡' to '里',
            '應' to '应', '當' to '当', '樂' to '乐', '顯' to '显', '現' to '现',
            '夢' to '梦', '飄' to '飘', '隨' to '随', '風' to '风', '雲' to '云',
            '電' to '电', '見' to '见', '聞' to '闻', '雙' to '双', '節' to '节',
            '氣' to '气', '發' to '发', '揚' to '扬', '臉' to '脸', '紅' to '红',
            '綠' to '绿', '藍' to '蓝', '傳' to '传', '詞' to '词', '曲' to '曲',
            '劇' to '剧', '場' to '场', '屆' to '届', '頭' to '头', '腦' to '脑',
            '兒' to '儿', '聲' to '声', '張' to '张', '楊' to '杨', '難' to '难',
            '過' to '过', '輕' to '轻', '離' to '离', '談' to '谈', '設' to '设',
            '計' to '计', '條' to '条', '號' to '号', '碼' to '码', '編' to '编',
            '終' to '终', '結' to '结', '總' to '总', '體' to '体', '們' to '们',
            '這' to '这', '產' to '产', '樣' to '样', '請' to '请', '無' to '无',
            '還' to '还', '讓' to '让', '會' to '会', '麼' to '么', '為' to '为',
            '來' to '来', '個' to '个', '長' to '长', '東' to '东', '從' to '从',
            '聽' to '听', '說' to '说', '書' to '书', '車' to '车', '業' to '业',
            '貝' to '贝', '員' to '员', '專' to '专', '變' to '变', '銀' to '银',
            '機' to '机', '選' to '选', '樹' to '树', '門' to '门', '問' to '问',
            '歲' to '岁', '舊' to '旧', '與' to '与', '豐' to '丰', '農' to '农',
            '麗' to '丽', '獨' to '独', '戀' to '恋', '藥' to '药', '錯' to '错',
            '邊' to '边', '媽' to '妈', '覺' to '觉', '頂' to '顶', '極' to '极',
            '淚' to '泪', '飛' to '飞', '劍' to '剑', '彈' to '弹', '歸' to '归',
            '給' to '给', '許' to '许', '遠' to '远', '靜' to '静', '麵' to '面',
            '畢' to '毕', '繼' to '继', '紀' to '纪', '絕' to '绝', '線' to '线',
            '網' to '网', '視' to '视', '親' to '亲', '頓' to '顿', '強' to '强',
            '確' to '确', '認' to '认', '質' to '质', '際' to '际', '醜' to '丑',
            '級' to '级', '歷' to '历', '錄' to '录', '華' to '华', '輯' to '辑',
            '專' to '专', '歡' to '欢', '聲' to '声', '團' to '团', '歷' to '历',
        ).forEach { (k, v) -> put(k, v) }
        // Large secondary table covering ~400 more pop-music-common chars.
        putAll(OpenCCSeed.EXTRA)
    }

    private val table = AtomicReference<Map<Char, Char>>(SEED_CHARS)
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
}
