package com.hj212.experiment4.b3;

import com.hj212.experiment4.fsm.StateMachine;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B3 源文件字符串提取器：状态图 {@code model/b3-string-extractor.puml} 的实现。
 *
 * <p>字符串由双引号界定；字符串内的反斜杠转义其后一个字符，
 * 即 {@code \"} 表示字符串内容里的一个双引号，{@code \\} 表示一个反斜杠，
 * 二者都不能作为字符串的界定符。</p>
 *
 * <p>状态图中写在 "/" 之后的动作（清空缓冲、追加字符、输出字符串）
 * 由 {@link #feed(char)} 按「迁移前状态 → 迁移后状态」派发执行。</p>
 */
public final class StringExtractor {

    private final StateMachine<ScanState, ScanEvent> machine;
    private final StringBuilder buffer = new StringBuilder();
    private final List<String> strings = new ArrayList<String>();

    private boolean unterminated;
    private boolean finished;

    public StringExtractor() {
        this.machine = buildMachine();
    }

    private StateMachine<ScanState, ScanEvent> buildMachine() {
        StateMachine.Builder<ScanState, ScanEvent> b = StateMachine.<ScanState, ScanEvent>builder(ScanState.SCANNING);

        b.transition(ScanState.SCANNING, CharRead.class, CharRead::isQuote, ScanState.IN_STRING);
        b.transition(ScanState.SCANNING, CharRead.class, c -> !c.isQuote(), ScanState.SCANNING);

        b.transition(ScanState.IN_STRING, CharRead.class, CharRead::isBackslash, ScanState.ESCAPE);
        b.transition(ScanState.IN_STRING, CharRead.class, CharRead::isQuote, ScanState.SCANNING);
        b.transition(ScanState.IN_STRING, CharRead.class,
                c -> !c.isQuote() && !c.isBackslash(), ScanState.IN_STRING);

        b.transition(ScanState.ESCAPE, CharRead.class, ScanState.IN_STRING);

        b.transition(ScanState.SCANNING, EndOfInput.class, ScanState.FINISHED);
        b.transition(ScanState.IN_STRING, EndOfInput.class, ScanState.FINISHED);
        b.transition(ScanState.ESCAPE, EndOfInput.class, ScanState.FINISHED);

        return b.build();
    }

    /**
     * 送入一个字符。
     *
     * @throws IllegalStateException 已经调用过 {@link #finish()} 之后继续送字符
     */
    public void feed(char ch) {
        if (finished) {
            throw new IllegalStateException("源文件已经结束，不能再送入字符");
        }
        ScanState before = machine.current();
        CharRead event = new CharRead(ch);
        machine.fire(event);
        ScanState after = machine.current();

        // 迁移上的动作
        if (before == ScanState.SCANNING && after == ScanState.IN_STRING) {
            buffer.setLength(0);                       // 读到左引号：开始一个新字符串
        } else if (before == ScanState.IN_STRING) {
            if (after == ScanState.SCANNING) {
                strings.add(buffer.toString());        // 读到右引号：输出一个字符串
            } else if (after == ScanState.IN_STRING) {
                buffer.append(ch);                     // 普通字符：追加
            }
        } else if (before == ScanState.ESCAPE && after == ScanState.IN_STRING) {
            buffer.append(ch);                         // 被转义的字符：原样保留，丢掉反斜杠
        }
    }

    /** 送入整个字符串（按字符依次投喂）。 */
    public void feed(CharSequence source) {
        for (int i = 0; i < source.length(); i++) {
            feed(source.charAt(i));
        }
    }

    /**
     * 源文件结束。
     *
     * @return 已提取出的全部字符串
     */
    public List<String> finish() {
        if (finished) {
            return Collections.unmodifiableList(strings);
        }
        ScanState before = machine.current();
        machine.fire(new EndOfInput());
        finished = true;
        if (before == ScanState.IN_STRING || before == ScanState.ESCAPE) {
            unterminated = true;
            buffer.setLength(0);                       // 丢弃残片
        }
        return Collections.unmodifiableList(strings);
    }

    /** 从文件中提取全部字符串。 */
    public static List<String> extractFrom(Path file, Charset charset) throws IOException {
        StringExtractor extractor = new StringExtractor();
        Reader reader = Files.newBufferedReader(file, charset);
        try {
            char[] chunk = new char[4096];
            int n;
            while ((n = reader.read(chunk)) > 0) {
                for (int i = 0; i < n; i++) {
                    extractor.feed(chunk[i]);
                }
            }
        } finally {
            reader.close();
        }
        return extractor.finish();
    }

    /** 从内存字符串中提取全部字符串。 */
    public static List<String> extract(String source) {
        StringExtractor extractor = new StringExtractor();
        extractor.feed(source);
        return extractor.finish();
    }

    // ------------------------------ 观测接口 ------------------------------

    public ScanState state() {
        return machine.current();
    }

    public List<String> strings() {
        return Collections.unmodifiableList(strings);
    }

    /** 是否因源文件在字符串中途结束而报错。 */
    public boolean isUnterminated() {
        return unterminated;
    }

    public List<String> trace() {
        return machine.trace();
    }
}
