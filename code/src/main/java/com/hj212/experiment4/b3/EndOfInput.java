package com.hj212.experiment4.b3;

/** 事件：源文件读取结束（EOF）。 */
public final class EndOfInput implements ScanEvent {

    @Override
    public String toString() {
        return "源文件结束";
    }
}
