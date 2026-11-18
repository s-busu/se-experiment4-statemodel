package com.hj212.experiment4.b1;

/** 事件：水箱烧坏（干烧等异常导致的硬件故障）。 */
public final class BoilerFault implements TankEvent {

    private final String reason;

    public BoilerFault() {
        this("水箱烧坏");
    }

    public BoilerFault(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return reason;
    }
}
