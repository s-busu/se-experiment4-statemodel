package com.hj212.experiment4.b2;

/** 事件：户主敲击键盘上的一个按键。 */
public final class KeyPress implements PanelEvent {

    private final char key;

    public KeyPress(char key) {
        this.key = key;
    }

    public char getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "按键('" + key + "')";
    }
}
