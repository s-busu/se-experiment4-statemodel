package com.hj212.experiment4.b2;

import com.hj212.experiment4.fsm.State;

/**
 * B2 SafeHome 控制面板 ControlPanel 的状态集合，
 * 与状态图 {@code model/b2-control-panel.puml} 一一对应。
 */
public enum PanelState implements State {

    /** 空闲：等待户主操作。 */
    IDLE("空闲"),
    /** 读取键入字符：累积密码，长度达到 4 位后提交校验。 */
    READING("读取键入字符"),
    /** 校验密码：把输入串与系统密码比对。 */
    VERIFYING("校验密码"),
    /** 锁定：连续错误 3 次后禁止输入，120 秒后解除。 */
    LOCKED("锁定"),
    /** 系统功能选择：密码正确后进入。 */
    SELECTING("系统功能选择");

    /** 密码长度固定为 4 位。 */
    public static final int PASSWORD_LENGTH = 4;
    /** 允许的连续错误次数。 */
    public static final int MAX_FAILURES = 3;
    /** 锁定时长（秒）。 */
    public static final int LOCK_SECONDS = 120;

    private final String label;

    PanelState(String label) {
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
