package com.hj212.experiment4.b1;

/** 事件：时钟到达整点（23:00 进入休眠，07:00 恢复烧水）。 */
public final class ClockEvent implements TankEvent {

    private final int hour;
    private final int minute;

    public ClockEvent(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public boolean at(int h, int m) {
        return hour == h && minute == m;
    }

    @Override
    public String toString() {
        return String.format("时钟(%02d:%02d)", hour, minute);
    }
}
