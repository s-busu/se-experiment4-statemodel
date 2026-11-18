package com.hj212.experiment4.b3;

/** 事件：从源文件中读到一个字符。 */
public final class CharRead implements ScanEvent {

    private final char value;

    public CharRead(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public boolean isQuote() {
        return value == '"';
    }

    public boolean isBackslash() {
        return value == '\\';
    }

    @Override
    public String toString() {
        return "读到'" + value + "'";
    }
}
