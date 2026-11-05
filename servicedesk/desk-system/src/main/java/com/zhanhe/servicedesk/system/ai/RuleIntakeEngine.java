package com.zhanhe.servicedesk.system.ai;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * 规则引擎（无模型时的等价能力）。
 *
 * <p>它存在的意义不是"凑一个降级方案"，而是划清边界：
 * 「缺项判定、类型归类、影响面判断」本质是规则问题，规则能覆盖就不必花模型的钱；
 * 模型只负责规则覆盖不到的口语表达（同一件事的千百种说法）。</p>
 *
 * <p>注意：规则引擎在本地运行，处理的是原始文本，不涉及任何外发，所以不做脱敏。</p>
 */
@Component
public class RuleIntakeEngine
{
    private static final List<String> DEVICE_TOKENS = Arrays.asList(
            "thinkpad", "macbook", "ultrasharp", "u2723qe", "笔记本", "台式机", "电脑", "显示器", "打印机",
            "键盘", "鼠标", "耳机", "投影仪", "投影", "扫描仪", "服务器", "交换机", "路由器", "网线",
            "vpn", "邮箱", "内网", "外网", "钉钉", "企业微信", "微信", "office", "wps", "chrome", "java",
            "idea", "驱动", "账号", "工号", "门禁");

    private static final List<String> BLOCKING_WORDS = Arrays.asList(
            "无法工作", "不能工作", "没法工作", "无法办公", "不能办公", "业务中断", "停工", "停摆", "全员", "所有人都",
            "整个部门", "没法干活", "无法干活", "不能干活", "干不了活", "影响工作");

    private static final List<String> MULTI_WORDS = Arrays.asList(
            "多人", "大家", "同事们", "我们组", "我们部门", "整个区域", "整层", "都断了", "都连不上", "都打不开");

    private static final List<String> ACCOUNT_WORDS = Arrays.asList(
            "账号", "帐号", "密码", "登录", "登陆", "权限", "验证码", "锁定", "冻结", "开通", "工号", "认证");

    private static final List<String> NETWORK_WORDS = Arrays.asList(
            "网络", "断网", "连不上网", "上不了网", "vpn", "内网", "外网", "网线", "路由", "网速", "掉线", "丢包", "ping");

    private static final List<String> SOFTWARE_WORDS = Arrays.asList(
            "安装", "装不了", "软件", "office", "wps", "驱动", "升级", "客户端", "钉钉", "企业微信", "浏览器", "插件", "病毒");

    private static final List<String> DEVICE_WORDS = Arrays.asList(
            "电脑", "笔记本", "台式机", "显示器", "打印机", "键盘", "鼠标", "黑屏", "开机", "蓝屏", "风扇", "主板",
            "屏幕", "闪屏", "耳机", "投影", "硬盘", "充不进电", "电池", "摄像头", "电源", "死机", "开不了机", "不开机");

    private static final List<String> TRIED_WORDS = Arrays.asList(
            "重启", "重装", "重新安装", "换线", "换了个", "重插", "已尝试", "试过", "重新登录", "清除缓存", "拔插", "关机再开");

    public IntakeResult run(String title, String description)
    {
        String text = join(title, description);
        IntakeResult result = new IntakeResult();
        result.setSource(SlotSchema.SOURCE_RULE);

        String device = firstMatch(text, DEVICE_TOKENS);
        result.setDeviceInfo(SlotSchema.truncate(device, SlotSchema.MAX_DEVICE));

        String symptom = extractSymptom(text);
        result.setSymptom(SlotSchema.truncate(symptom, SlotSchema.MAX_SYMPTOM));

        result.setImpactScope(detectImpact(text));
        String tried = extractTried(text);
        result.setTriedActions(SlotSchema.truncate(tried, SlotSchema.MAX_TRIED));

        String type = detectType(text);
        result.setSuggestedType(type);
        String hitWord = hitWordFor(type, text);
        result.setTypeBasis(SlotSchema.TYPE_OTHER.equals(type)
                ? "未命中明确类别关键词，归为其他"
                : "规则命中「" + hitWord + "」类关键词");
        result.setSuggestedPriority(PriorityRule.suggest(result.getImpactScope(), text));
        return result;
    }

    static String join(String title, String description)
    {
        String t = title == null ? "" : title.trim();
        String d = description == null ? "" : description.trim();
        if (t.isEmpty())
        {
            return d;
        }
        if (d.isEmpty())
        {
            return t;
        }
        return t + "。" + d;
    }

    private static String firstMatch(String text, List<String> tokens)
    {
        for (String token : tokens)
        {
            String t = token.trim();
            if (t.isEmpty())
            {
                continue;
            }
            // ASCII 品牌词允许带型号后缀：ThinkPad T14 / Dell U2723QE
            if (t.matches("[a-z0-9]+"))
            {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile(java.util.regex.Pattern.quote(t) + "[\\s\\-]?[a-z0-9\\-]{0,12}",
                                java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(text);
                if (m.find())
                {
                    return m.group().trim();
                }
                continue;
            }
            int idx = text.indexOf(t);
            if (idx >= 0)
            {
                return text.substring(idx, idx + t.length());
            }
        }
        return null;
    }

    static String detectImpact(String text)
    {
        String lower = text.toLowerCase();
        for (String word : BLOCKING_WORDS)
        {
            if (lower.contains(word))
            {
                return SlotSchema.IMPACT_BLOCKING;
            }
        }
        for (String word : MULTI_WORDS)
        {
            if (lower.contains(word))
            {
                return SlotSchema.IMPACT_MULTI;
            }
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? SlotSchema.IMPACT_UNKNOWN : SlotSchema.IMPACT_SINGLE;
    }

    /**
     * 按「关键词命中次数」归类，而不是「先命中哪个算哪个」。
     * 原因：真实描述经常混着多类词（"装不了提示权限不足"既有安装也有权限），
     * 首个命中会误判，命中次数更能反映主要诉求；分数相同按「账号 > 网络 > 软件 > 设备」的稳定顺序。
     */
    static String detectType(String text)
    {
        String lower = text.toLowerCase();
        int account = countHits(lower, ACCOUNT_WORDS);
        int network = countHits(lower, NETWORK_WORDS);
        int software = countHits(lower, SOFTWARE_WORDS);
        int device = countHits(lower, DEVICE_WORDS);

        int max = Math.max(Math.max(account, network), Math.max(software, device));
        if (max == 0)
        {
            return SlotSchema.TYPE_OTHER;
        }
        if (account == max)
        {
            return SlotSchema.TYPE_ACCOUNT;
        }
        if (network == max)
        {
            return SlotSchema.TYPE_NETWORK;
        }
        if (software == max)
        {
            return SlotSchema.TYPE_SOFTWARE;
        }
        return SlotSchema.TYPE_DEVICE;
    }

    private static int countHits(String text, List<String> words)
    {
        int hits = 0;
        for (String word : words)
        {
            if (text.contains(word))
            {
                hits++;
            }
        }
        return hits;
    }

    private static String hitWordFor(String type, String text)
    {
        String lower = text.toLowerCase();
        List<String> words;
        if (SlotSchema.TYPE_ACCOUNT.equals(type))
        {
            words = ACCOUNT_WORDS;
        }
        else if (SlotSchema.TYPE_NETWORK.equals(type))
        {
            words = NETWORK_WORDS;
        }
        else if (SlotSchema.TYPE_SOFTWARE.equals(type))
        {
            words = SOFTWARE_WORDS;
        }
        else if (SlotSchema.TYPE_DEVICE.equals(type))
        {
            words = DEVICE_WORDS;
        }
        else
        {
            return "通用";
        }
        for (String word : words)
        {
            if (lower.contains(word))
            {
                return word;
            }
        }
        return "通用";
    }

    private static boolean containsAny(String text, List<String> words)
    {
        for (String word : words)
        {
            if (text.contains(word))
            {
                return true;
            }
        }
        return false;
    }

    /** 取包含故障关键词的从句作为现象；取不到就取首句。保证结果是原文片段，证据校验必然通过 */
    static String extractSymptom(String text)
    {
        for (String clause : splitClauses(text))
        {
            if (containsAny(clause.toLowerCase(), Arrays.asList(
                    "无法", "不能", "打不开", "连不上", "报错", "失败", "蓝屏", "黑屏", "卡", "慢", "掉线", "闪",
                    "没反应", "异常", "错误", "崩", "断")))
            {
                return clamp(clause, 200);
            }
        }
        return clamp(splitClauses(text).isEmpty() ? text : splitClauses(text).get(0), 200);
    }

    static String extractTried(String text)
    {
        for (String clause : splitClauses(text))
        {
            if (containsAny(clause.toLowerCase(), TRIED_WORDS))
            {
                return clamp(clause, 120);
            }
        }
        return null;
    }

    private static List<String> splitClauses(String text)
    {
        if (text == null)
        {
            return java.util.Collections.emptyList();
        }
        String[] parts = text.split("[。；;！!？?\\n，,]");
        List<String> list = new java.util.ArrayList<>();
        for (String p : parts)
        {
            String v = p.trim();
            if (!v.isEmpty())
            {
                list.add(v);
            }
        }
        return list;
    }

    private static String clamp(String text, int max)
    {
        if (text == null)
        {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
