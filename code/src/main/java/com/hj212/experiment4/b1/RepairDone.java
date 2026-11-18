package com.hj212.experiment4.b1;

/** 事件：维修完成，水箱恢复正常。 */
public final class RepairDone implements TankEvent {

    @Override
    public String toString() {
        return "维修完成";
    }
}
