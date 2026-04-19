package com.colamusic.core.database.search

/**
 * Expanded Han → first-letter-pinyin seed (~400 additional entries).
 * Merged into [PinyinIndexer.SEED] at init to keep the primary file readable.
 */
internal object PinyinSeed {
    val EXTRA: Map<Char, Char> = buildMap {
        mapOf(
            // Common verbs / adjectives / particles
            '想' to 'x', '念' to 'n', '思' to 's', '记' to 'j', '忘' to 'w',
            '等' to 'd', '待' to 'd', '望' to 'w', '盼' to 'p', '求' to 'q',
            '要' to 'y', '做' to 'z', '作' to 'z', '为' to 'w', '把' to 'b',
            '被' to 'b', '让' to 'r', '使' to 's', '令' to 'l', '令' to 'l',
            '许' to 'x', '该' to 'g', '能' to 'n', '可' to 'k', '会' to 'h',
            '该' to 'g', '应' to 'y', '将' to 'j', '会' to 'h', '要' to 'y',
            // Pop title staples
            '夜' to 'y', '星' to 'x', '空' to 'k', '梦' to 'm', '醒' to 'x',
            '睡' to 's', '醉' to 'z', '暗' to 'a', '亮' to 'l', '影' to 'y',
            '声' to 's', '静' to 'j', '闹' to 'n', '动' to 'd', '停' to 't',
            '飞' to 'f', '落' to 'l', '升' to 's', '降' to 'j', '过' to 'g',
            '度' to 'd', '刻' to 'k', '秒' to 'm', '时' to 's', '辰' to 'c',
            '逝' to 's', '永' to 'y', '恒' to 'h', '瞬' to 's', '间' to 'j',
            '一' to 'y', '生' to 's', '活' to 'h', '命' to 'm', '运' to 'y',
            '路' to 'l', '街' to 'j', '道' to 'd', '巷' to 'x', '桥' to 'q',
            '港' to 'g', '湾' to 'w', '岸' to 'a', '滩' to 't', '浪' to 'l',
            '潮' to 'c', '波' to 'b', '涛' to 't', '浮' to 'f', '沉' to 'c',
            // Common CN pop artists' name chars
            '邓' to 'd', '周' to 'z', '王' to 'w', '陈' to 'c', '林' to 'l',
            '李' to 'l', '张' to 'z', '刘' to 'l', '黄' to 'h', '赵' to 'z',
            '吴' to 'w', '杨' to 'y', '孙' to 's', '朱' to 'z', '胡' to 'h',
            '高' to 'g', '徐' to 'x', '罗' to 'l', '曹' to 'c', '郑' to 'z',
            '谢' to 'x', '杜' to 'd', '宋' to 's', '韩' to 'h', '唐' to 't',
            '蔡' to 'c', '许' to 'x', '彭' to 'p', '吕' to 'l', '董' to 'd',
            '蒋' to 'j', '苏' to 's', '叶' to 'y', '沈' to 's', '姚' to 'y',
            '卢' to 'l', '何' to 'h', '余' to 'y', '袁' to 'y', '邹' to 'z',
            '田' to 't', '黎' to 'l', '段' to 'd', '梁' to 'l', '薛' to 'x',
            '贺' to 'h', '雷' to 'l', '汪' to 'w', '毛' to 'm', '尚' to 's',
            '易' to 'y', '庞' to 'p', '谭' to 't', '廖' to 'l', '孔' to 'k',
            '阎' to 'y', '常' to 'c', '武' to 'w', '任' to 'r', '康' to 'k',
            '郭' to 'g', '阳' to 'y', '夏' to 'x', '戚' to 'q', '翁' to 'w',
            '凌' to 'l', '柳' to 'l', '侯' to 'h', '连' to 'l', '龙' to 'l',
            '莫' to 'm', '符' to 'f', '甄' to 'z', '钱' to 'q', '皮' to 'p',
            // Entertainment / media keywords
            '电' to 'd', '视' to 's', '剧' to 'j', '影' to 'y', '片' to 'p',
            '集' to 'j', '季' to 'j', '章' to 'z', '回' to 'h', '卷' to 'j',
            '章' to 'z', '篇' to 'p', '章' to 'z', '幕' to 'm', '剧' to 'j',
            '话' to 'h', '语' to 'y', '歌' to 'g', '曲' to 'q', '旋' to 'x',
            '律' to 'l', '谱' to 'p', '词' to 'c', '赋' to 'f', '诗' to 's',
            '调' to 'd', '乐' to 'y', '音' to 'y', '响' to 'x', '唱' to 'c',
            '奏' to 'z', '鸣' to 'm', '弹' to 't', '拨' to 'b', '敲' to 'q',
            // Mood / color / nature
            '爱' to 'a', '恨' to 'h', '喜' to 'x', '怒' to 'n', '哀' to 'a',
            '惧' to 'j', '悲' to 'b', '伤' to 's', '泣' to 'q', '泪' to 'l',
            '笑' to 'x', '哭' to 'k', '叹' to 't', '呼' to 'h', '吸' to 'x',
            '喊' to 'h', '唤' to 'h', '叫' to 'j', '骂' to 'm', '吵' to 'c',
            '谈' to 't', '言' to 'y', '说' to 's', '讲' to 'j', '论' to 'l',
            '诉' to 's', '告' to 'g', '问' to 'w', '答' to 'd', '回' to 'h',
            '应' to 'y', '接' to 'j', '送' to 's', '迎' to 'y', '别' to 'b',
            // Body / feelings
            '头' to 't', '脑' to 'n', '眼' to 'y', '眉' to 'm', '鼻' to 'b',
            '嘴' to 'z', '唇' to 'c', '齿' to 'c', '舌' to 's', '脸' to 'l',
            '耳' to 'e', '颈' to 'j', '肩' to 'j', '手' to 's', '足' to 'z',
            '胸' to 'x', '腰' to 'y', '腹' to 'f', '背' to 'b', '腿' to 't',
            '膝' to 'x', '踝' to 'h', '指' to 'z', '掌' to 'z', '爪' to 'z',
            '心' to 'x', '肺' to 'f', '肝' to 'g', '胆' to 'd', '脾' to 'p',
            // Numbers and time
            '零' to 'l', '百' to 'b', '千' to 'q', '万' to 'w', '亿' to 'y',
            '旧' to 'j', '古' to 'g', '老' to 'l', '少' to 's', '青' to 'q',
            '壮' to 'z', '盛' to 's', '衰' to 's', '灭' to 'm', '活' to 'h',
            // Weather
            '晴' to 'q', '阴' to 'y', '雨' to 'y', '雪' to 'x', '霜' to 's',
            '雾' to 'w', '雷' to 'l', '电' to 'd', '云' to 'y', '霞' to 'x',
            '虹' to 'h', '风' to 'f', '寒' to 'h', '冷' to 'l', '凉' to 'l',
            '暖' to 'n', '热' to 'r', '冬' to 'd', '夏' to 'x', '秋' to 'q',
            '春' to 'c', '岁' to 's', '月' to 'y', '年' to 'n', '日' to 'r',
            // Colors
            '红' to 'h', '橙' to 'c', '黄' to 'h', '绿' to 'l', '青' to 'q',
            '蓝' to 'l', '紫' to 'z', '黑' to 'h', '白' to 'b', '灰' to 'h',
            '粉' to 'f', '橘' to 'j', '褐' to 'h', '棕' to 'z', '银' to 'y',
            '金' to 'j', '彩' to 'c', '色' to 's', '缤' to 'b', '纷' to 'f',
            // Location
            '京' to 'j', '沪' to 'h', '穗' to 's', '津' to 'j', '深' to 's',
            '港' to 'g', '澳' to 'a', '台' to 't', '京' to 'j', '城' to 'c',
            // Relationships
            '爸' to 'b', '爹' to 'd', '娘' to 'n', '母' to 'm', '父' to 'f',
            '兄' to 'x', '弟' to 'd', '姐' to 'j', '妹' to 'm', '姊' to 'z',
            '哥' to 'g', '叔' to 's', '伯' to 'b', '姑' to 'g', '姨' to 'y',
            '舅' to 'j', '婶' to 's', '嫂' to 's', '儿' to 'e', '子' to 'z',
            '女' to 'n', '孩' to 'h', '郎' to 'l', '妇' to 'f', '妻' to 'q',
            '夫' to 'f', '婿' to 'x', '媳' to 'x', '亲' to 'q', '戚' to 'q',
            '朋' to 'p', '友' to 'y', '知' to 'z', '己' to 'j', '敌' to 'd',
            // Common verbs
            '打' to 'd', '跳' to 't', '跑' to 'p', '走' to 'z', '爬' to 'p',
            '游' to 'y', '泳' to 'y', '读' to 'd', '写' to 'x', '画' to 'h',
            '唱' to 'c', '跳' to 't', '演' to 'y', '奏' to 'z', '拍' to 'p',
            '摄' to 's', '录' to 'l', '播' to 'b', '放' to 'f', '倾' to 'q',
            '坠' to 'z', '落' to 'l', '起' to 'q', '升' to 's', '悬' to 'x',
            '舞' to 'w', '动' to 'd', '静' to 'j', '止' to 'z', '停' to 't',
        ).forEach { (k, v) -> put(k, v) }
    }
}
