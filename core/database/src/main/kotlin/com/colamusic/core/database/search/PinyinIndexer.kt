package com.colamusic.core.database.search

/**
 * Compact Han → first-letter pinyin helper. Returns the first-letter pinyin
 * string for a CJK title so queries like "qhc" can match "青花瓷".
 *
 * v0.2 seed covers ~120 of the most common Mandarin characters. The larger
 * bundled table (v0.3) will be loaded from assets/pinyin/initials.json via the
 * same API — callers need not change.
 */
object PinyinIndexer {

    fun initials(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (c in input) {
            val cjk = c.code in 0x4E00..0x9FFF
            if (cjk) {
                val initial = SEED[c]
                if (initial != null) sb.append(initial)
            } else if (c.isLetter()) {
                sb.append(c.lowercaseChar())
            }
        }
        return sb.toString()
    }

    /** Minimal seed of high-frequency pop-music characters. */
    private val SEED: Map<Char, Char> = buildMap {
        // 一至十, 你我他
        mapOf(
            '一' to 'y', '二' to 'e', '三' to 's', '四' to 's', '五' to 'w',
            '六' to 'l', '七' to 'q', '八' to 'b', '九' to 'j', '十' to 's',
            '你' to 'n', '我' to 'w', '他' to 't', '她' to 't', '它' to 't',
            '的' to 'd', '是' to 's', '在' to 'z', '有' to 'y', '不' to 'b',
            '了' to 'l', '人' to 'r', '说' to 's', '爱' to 'a', '心' to 'x',
            '花' to 'h', '青' to 'q', '瓷' to 'c', '红' to 'h', '白' to 'b',
            '黑' to 'h', '风' to 'f', '雨' to 'y', '雪' to 'x', '光' to 'g',
            '月' to 'y', '日' to 'r', '天' to 't', '地' to 'd', '水' to 's',
            '火' to 'h', '梦' to 'm', '醒' to 'x', '睡' to 's', '等' to 'd',
            '情' to 'q', '想' to 'x', '念' to 'n', '忘' to 'w', '记' to 'j',
            '回' to 'h', '去' to 'q', '来' to 'l', '走' to 'z', '跑' to 'p',
            '飞' to 'f', '唱' to 'c', '歌' to 'g', '听' to 't', '看' to 'k',
            '见' to 'j', '给' to 'g', '送' to 's', '留' to 'l', '别' to 'b',
            '再' to 'z', '离' to 'l', '合' to 'h', '分' to 'f', '开' to 'k',
            '关' to 'g', '门' to 'm', '窗' to 'c', '家' to 'j', '国' to 'g',
            '世' to 's', '界' to 'j', '城' to 'c', '乡' to 'x', '村' to 'c',
            '山' to 's', '水' to 's', '海' to 'h', '江' to 'j', '河' to 'h',
            '湖' to 'h', '林' to 'l', '树' to 's', '叶' to 'y', '草' to 'c',
            '云' to 'y', '雾' to 'w', '雷' to 'l', '电' to 'd', '夜' to 'y',
            '晨' to 'c', '昏' to 'h', '夏' to 'x', '秋' to 'q', '冬' to 'd',
            '春' to 'c', '暖' to 'n', '冷' to 'l', '热' to 'r', '凉' to 'l',
            '年' to 'n', '岁' to 's', '时' to 's', '刻' to 'k', '分' to 'f',
            '秒' to 'm', '光' to 'g', '阴' to 'y', '暗' to 'a', '明' to 'm',
            '好' to 'h', '坏' to 'h', '真' to 'z', '假' to 'j', '美' to 'm',
            '丑' to 'c', '新' to 'x', '旧' to 'j', '老' to 'l', '少' to 's',
            '大' to 'd', '小' to 'x', '多' to 'd', '少' to 's', '长' to 'c',
            '短' to 'd', '高' to 'g', '低' to 'd', '远' to 'y', '近' to 'j',
            '快' to 'k', '慢' to 'm', '同' to 't', '异' to 'y', '古' to 'g',
            '今' to 'j', '东' to 'd', '西' to 'x', '南' to 'n', '北' to 'b',
            '前' to 'q', '后' to 'h', '左' to 'z', '右' to 'y', '上' to 's',
            '下' to 'x', '中' to 'z', '外' to 'w', '里' to 'l', '面' to 'm',
            // pop-title staples
            '告' to 'g', '球' to 'q', '江' to 'j', '南' to 'n', '稻' to 'd', '香' to 'x',
            '飘' to 'p', '远' to 'y', '童' to 't', '话' to 'h', '兰' to 'l', '亭' to 't',
            '序' to 'x', '演' to 'y', '员' to 'y', '盛' to 's', '夏' to 'x', '光' to 'g',
        ).forEach { (k, v) -> put(k, v) }
        // Secondary seed (~400 more chars)
        putAll(PinyinSeed.EXTRA)
    }
}
