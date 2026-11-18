package com.hj212.experiment4.b2;

import com.hj212.experiment4.fsm.StateMachine;

import java.util.List;

/**
 * B2 SafeHome 控制面板 ControlPanel 的实现，
 * 对应状态图 {@code model/b2-control-panel.puml}。
 *
 * <p>行为要点：</p>
 * <ul>
 *   <li>密码长度 4 位，每敲入一个字符都与密码长度比较，满 4 位立即校验；</li>
 *   <li>校验错误累计 3 次锁定面板，锁定期间按键一律被忽略；</li>
 *   <li>锁定满 120 秒后回到「读取键入字符」，错误计数清零，可重新输入；</li>
 *   <li>校验正确进入「系统功能选择」。</li>
 * </ul>
 *
 * <p>状态图中的 "do / 每收到一个按键即与密码长度比较" 由 {@link #pressKey(char)}
 * 承担（读取按键、累积字符、判断长度并触发校验）；"entry / ..." 由
 * {@link StateMachine.Builder#entry} 注册的动作承担。</p>
 */
public final class ControlPanel {

    private final String password;
    private final StringBuilder buffer = new StringBuilder();
    private final StateMachine<PanelState, PanelEvent> machine;

    private int failCount;
    private int lockedSeconds;
    private boolean functionIndicator;

    public ControlPanel() {
        this("2580");
    }

    public ControlPanel(String password) {
        this.password = password;
        this.machine = buildMachine();
    }

    private StateMachine<PanelState, PanelEvent> buildMachine() {
        StateMachine.Builder<PanelState, PanelEvent> b = StateMachine.<PanelState, PanelEvent>builder(PanelState.IDLE);

        b.transition(PanelState.IDLE, KeyPress.class, PanelState.READING);
        b.transition(PanelState.READING, KeyPress.class,
                k -> buffer.length() + 1 < PanelState.PASSWORD_LENGTH, PanelState.READING);
        b.transition(PanelState.READING, KeyPress.class,
                k -> buffer.length() + 1 >= PanelState.PASSWORD_LENGTH, PanelState.VERIFYING);

        b.transition(PanelState.VERIFYING, VerifyResult.class,
                v -> v.isCorrect(), PanelState.SELECTING);
        b.transition(PanelState.VERIFYING, VerifyResult.class,
                v -> !v.isCorrect() && failCount < PanelState.MAX_FAILURES, PanelState.READING);
        b.transition(PanelState.VERIFYING, VerifyResult.class,
                v -> !v.isCorrect() && failCount >= PanelState.MAX_FAILURES, PanelState.LOCKED);

        b.transition(PanelState.LOCKED, TimerElapsed.class,
                t -> t.getSeconds() >= PanelState.LOCK_SECONDS, PanelState.READING);

        b.transition(PanelState.SELECTING, FunctionDone.class, PanelState.IDLE);

        b.entry(PanelState.IDLE, e -> functionIndicator = false);
        b.entry(PanelState.READING, e -> buffer.setLength(0));
        b.entry(PanelState.LOCKED, e -> {
            buffer.setLength(0);
            lockedSeconds = 0;
        });
        b.entry(PanelState.SELECTING, e -> {
            buffer.setLength(0);
            failCount = 0;
            functionIndicator = true;
        });

        return b.build();
    }

    // ---------------------------- 外部事件入口 ----------------------------

    /**
     * 户主敲击一个按键。
     *
     * @return 该按键是否被接受（锁定状态与功能选择状态下返回 false，按键被忽略）
     */
    public boolean pressKey(char key) {
        PanelState s = machine.current();
        if (s == PanelState.LOCKED || s == PanelState.SELECTING || s == PanelState.VERIFYING) {
            return false;
        }
        boolean completesPassword = buffer.length() + 1 >= PanelState.PASSWORD_LENGTH;
        machine.fire(new KeyPress(key));
        buffer.append(key);
        if (completesPassword) {
            verify();
        }
        return true;
    }

    /** 校验密码（对应状态图中「校验密码」状态的 entry 行为）。 */
    private void verify() {
        boolean correct = password.equals(buffer.toString());
        if (!correct) {
            failCount++;
        }
        machine.fire(new VerifyResult(correct));
    }

    /**
     * 推进锁定计时器。
     *
     * @return 本次推进是否解除了锁定
     */
    public boolean elapse(int seconds) {
        if (machine.current() != PanelState.LOCKED) {
            return false;
        }
        lockedSeconds += seconds;
        boolean unlocked = machine.fireIfPossible(new TimerElapsed(lockedSeconds));
        if (unlocked) {
            failCount = 0;
        }
        return unlocked;
    }

    /** 结束功能选择，返回空闲状态。 */
    public boolean finishFunction() {
        return machine.fireIfPossible(new FunctionDone());
    }

    // ------------------------------ 观测接口 ------------------------------

    public PanelState state() {
        return machine.current();
    }

    public boolean isLocked() {
        return machine.current() == PanelState.LOCKED;
    }

    public String inputBuffer() {
        return buffer.toString();
    }

    public int failCount() {
        return failCount;
    }

    public int lockedSeconds() {
        return lockedSeconds;
    }

    public boolean isFunctionIndicatorOn() {
        return functionIndicator;
    }

    public List<String> trace() {
        return machine.trace();
    }
}
