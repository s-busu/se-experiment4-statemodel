package com.hj212.experiment4.b2;

/** 事件：锁定计时器累计时长上报（对应状态图中的 after(120 秒)）。 */
public final class TimerElapsed implements PanelEvent {

    private final int seconds;

    public TimerElapsed(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }

    @Override
    public String toString() {
        return "计时(" + seconds + "秒)";
    }
}
