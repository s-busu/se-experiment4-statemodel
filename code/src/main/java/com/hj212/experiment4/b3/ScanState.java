package com.hj212.experiment4.b3;

import com.hj212.experiment4.fsm.State;

/**
 * B3 字符串提取器的状态集合，与状态图
 * {@code model/b3-string-extractor.puml} 一一对应。
 */
public enum ScanState implements State {

    /** 扫描普通字符：字符串之外。 */
    SCANNING("扫描普通字符"),
    /** 字符串内：累积字符串内容。 */
    IN_STRING("字符串内"),
    /** 转义：刚读到反斜杠，等待被转义的字符。 */
    ESCAPE("转义"),
    /** 终态：源文件处理结束（状态图中的终态 [*]）。 */
    FINISHED("结束");

    private final String label;

    ScanState(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public State parent() {
        return null;
    }
}
