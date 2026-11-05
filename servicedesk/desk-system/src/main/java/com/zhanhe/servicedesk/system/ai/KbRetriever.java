package com.zhanhe.servicedesk.system.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.zhanhe.servicedesk.system.mapper.ItTicketMapper;

/**
 * 历史同类工单检索（BM25，纯本地计算，不调用模型）。
 *
 * <p>为什么这块刻意不用大模型：</p>
 * <ul>
 *   <li>相似工单本质是「关键词 + 词频」问题，BM25 更快（毫秒级）、更便宜（零成本）、更可解释；</li>
 *   <li>引用的是<b>真实工单号与真实处理说明</b>，不存在模型改写导致的信息失真；</li>
 *   <li>召回质量不足时，升级路径是 MySQL ngram 全文索引或向量库，业务代码不受影响（换 Retriever 实现即可）。</li>
 * </ul>
 *
 * <p>中文没有空格，按「2-gram + ASCII 单词」切分；字段加权让标题与设备名权重高于长描述。</p>
 */
@Component
public class KbRetriever
{
    private static final double K1 = 1.2;

    private static final double B = 0.75;

    /**
     * 相对相关度下限：低于该比例的命中直接丢掉。
     * 中文 2-gram 很容易因为"办公""我们组"这类通用词产生弱匹配，
     * 把 9% 这种噪声条目推给 IT，反而增加判断成本——宁可少给，也不要凑数。
     */
    private static final int MIN_RELEVANCE = 15;

    private final ItTicketMapper ticketMapper;

    public KbRetriever(ItTicketMapper ticketMapper)
    {
        this.ticketMapper = ticketMapper;
    }

    /**
     * @param query          查询文本（工单标题+描述，或 IT 手输关键词）
     * @param excludeTicketId 需要排除的工单（当前工单自己）
     * @param topK           返回条数
     */
    public List<TicketDoc> search(String query, Long excludeTicketId, int topK)
    {
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty())
        {
            return Collections.emptyList();
        }
        List<TicketDoc> docs = loadDocs(excludeTicketId);
        if (docs.isEmpty())
        {
            return Collections.emptyList();
        }

        // tf: docIndex -> term -> weight
        List<Map<String, Double>> termFreqs = new ArrayList<>(docs.size());
        Map<String, Integer> docFreq = new HashMap<>();
        double totalLength = 0;

        for (TicketDoc doc : docs)
        {
            Map<String, Double> tf = new LinkedHashMap<>();
            accumulate(tf, doc.getTitle(), 2.0);
            accumulate(tf, doc.getDeviceInfo(), 2.0);
            accumulate(tf, doc.getSymptom(), 1.0);
            accumulate(tf, doc.getDescription(), 1.0);
            accumulate(tf, doc.getProcessNote(), 1.5);
            termFreqs.add(tf);
            totalLength += tf.values().stream().mapToDouble(Double::doubleValue).sum();
            for (String term : tf.keySet())
            {
                docFreq.merge(term, 1, Integer::sum);
            }
        }

        int n = docs.size();
        double avgLen = totalLength / n;

        for (int i = 0; i < n; i++)
        {
            Map<String, Double> tf = termFreqs.get(i);
            double docLen = tf.values().stream().mapToDouble(Double::doubleValue).sum();
            double score = 0;
            for (String term : queryTerms)
            {
                Double freq = tf.get(term);
                if (freq == null)
                {
                    continue;
                }
                int df = docFreq.getOrDefault(term, 0);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                score += idf * (freq * (K1 + 1)) / (freq + K1 * (1 - B + B * docLen / (avgLen == 0 ? 1 : avgLen)));
            }
            docs.get(i).setScore(score);
        }

        docs.removeIf(d -> d.getScore() <= 0);
        docs.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (docs.size() > topK)
        {
            docs = new ArrayList<>(docs.subList(0, topK));
        }
        double max = docs.isEmpty() ? 0 : docs.get(0).getScore();
        for (TicketDoc doc : docs)
        {
            doc.setRelevance(max <= 0 ? 0 : (int) Math.round(doc.getScore() / max * 100));
        }
        docs.removeIf(d -> d.getRelevance() < MIN_RELEVANCE);
        return docs;
    }

    private List<TicketDoc> loadDocs(Long excludeTicketId)
    {
        List<Map<String, Object>> rows = ticketMapper.selectTicketsForRetrieval();
        List<TicketDoc> docs = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows)
        {
            Long id = toLong(row.get("ticketId"));
            if (excludeTicketId != null && excludeTicketId.equals(id))
            {
                continue;
            }
            TicketDoc doc = new TicketDoc();
            doc.setTicketId(id);
            doc.setTicketNo(toStr(row.get("ticketNo")));
            doc.setTicketType(toStr(row.get("ticketType")));
            doc.setTitle(toStr(row.get("title")));
            doc.setDescription(toStr(row.get("description")));
            doc.setDeviceInfo(toStr(row.get("deviceInfo")));
            doc.setSymptom(toStr(row.get("symptom")));
            doc.setImpactScope(toStr(row.get("impactScope")));
            doc.setProcessNote(toStr(row.get("processNote")));
            docs.add(doc);
        }
        return docs;
    }

    private static void accumulate(Map<String, Double> tf, String text, double weight)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }
        for (String token : tokenize(text))
        {
            tf.merge(token, weight, Double::sum);
        }
    }

    /**
     * 分词：ASCII 单词按空白/标点切；中文按 2-gram（单字串保留单字）。
     */
    static List<String> tokenize(String text)
    {
        if (text == null || text.isBlank())
        {
            return Collections.emptyList();
        }
        String normalized = EvidenceValidator.normalize(text);
        List<String> tokens = new ArrayList<>();
        StringBuilder ascii = new StringBuilder();
        char prevCjk = 0;
        for (int i = 0; i < normalized.length(); i++)
        {
            char c = normalized.charAt(i);
            if (isAsciiWordChar(c))
            {
                ascii.append(c);
                prevCjk = 0;
                continue;
            }
            flushAscii(ascii, tokens);
            if (isCjk(c))
            {
                if (prevCjk != 0)
                {
                    tokens.add("" + prevCjk + c);
                }
                else
                {
                    tokens.add(String.valueOf(c));
                }
                prevCjk = c;
            }
            else
            {
                prevCjk = 0;
            }
        }
        flushAscii(ascii, tokens);
        return tokens;
    }

    private static void flushAscii(StringBuilder ascii, List<String> tokens)
    {
        if (ascii.length() > 0)
        {
            tokens.add(ascii.toString());
            ascii.setLength(0);
        }
    }

    private static boolean isAsciiWordChar(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean isCjk(char c)
    {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }

    private static String toStr(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private static Long toLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
