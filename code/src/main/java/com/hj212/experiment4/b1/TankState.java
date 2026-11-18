package com.hj212.experiment4.b1;

import com.hj212.experiment4.fsm.State;

/**
 * B1 恒温水箱的状态集合，与状态图 {@code model/b1-water-tank.puml} 一一对应。
 *
 * <p>{@link #WORKING} 是组合状态，封装三个工作子状态；
 * {@link #SLEEP} 与 {@link #REPAIR} 是与它并列的顶层状态。</p>
 */
public enum TankState implements State {

    /** 组合状态：恒温烧水过程（启动采样，内部三个子状态循环）。 */
    WORKING("恒温烧水过程", null),
    /** 待机：温度不低于 20℃，继电器断开。 */
    IDLE("待机", WORKING),
    /** 加热烧水：继电器闭合，温度上升中。 */
    HEATING("加热烧水", WORKING),
    /** 缺水保护：检测到无水，不打开电源。 */
    NO_WATER("缺水保护", WORKING),
    /** 休眠：23:00 断电，停止采样。 */
    SLEEP("休眠", null),
    /** 故障维修：水箱烧坏，等待维修完成。 */
    REPAIR("故障维修", null);

    private final String label;
    private final TankState parent;

    TankState(String label, TankState parent) {
        this.label = label;
        this.parent = parent;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public State parent() {
        return parent;
    }
}
