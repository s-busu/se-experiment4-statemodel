package com.hj212.experiment4.b2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B2 ControlPanel 状态机的单元测试，覆盖密码输入、校验、锁定与解锁。 */
@DisplayName("B2 SafeHome 控制面板状态图")
class ControlPanelTest {

    private static final String PASSWORD = "2580";

    private ControlPanel newPanel() {
        return new ControlPanel(PASSWORD);
    }

    private void type(ControlPanel panel, String keys) {
        for (char c : keys.toCharArray()) {
            panel.pressKey(c);
        }
    }

    @Test
    @DisplayName("上电后处于空闲状态")
    void startsIdle() {
        ControlPanel panel = newPanel();
        assertEquals(PanelState.IDLE, panel.state());
    }

    @Test
    @DisplayName("敲击第一个按键后进入读取键入字符状态")
    void entersReadingOnFirstKey() {
        ControlPanel panel = newPanel();
        assertTrue(panel.pressKey('2'));
        assertEquals(PanelState.READING, panel.state());
        assertEquals("2", panel.inputBuffer());
    }

    @Test
    @DisplayName("不足 4 位时停留在读取键入字符状态")
    void staysReadingBelowPasswordLength() {
        ControlPanel panel = newPanel();
        type(panel, "258");
        assertEquals(PanelState.READING, panel.state());
        assertEquals(3, panel.inputBuffer().length());
    }

    @Test
    @DisplayName("输入满 4 位且密码正确：进入系统功能选择状态")
    void entersSelectingOnCorrectPassword() {
        ControlPanel panel = newPanel();
        type(panel, PASSWORD);
        assertEquals(PanelState.SELECTING, panel.state());
        assertTrue(panel.isFunctionIndicatorOn());
        assertEquals(0, panel.failCount());
    }

    @Test
    @DisplayName("密码错误：回到读取键入字符状态，错误计数加 1，缓冲清空")
    void returnsToReadingOnWrongPassword() {
        ControlPanel panel = newPanel();
        type(panel, "1111");
        assertEquals(PanelState.READING, panel.state());
        assertEquals(1, panel.failCount());
        assertEquals("", panel.inputBuffer());
    }

    @Test
    @DisplayName("连续输入错误密码 3 次：锁定控制面板")
    void locksAfterThreeFailures() {
        ControlPanel panel = newPanel();
        type(panel, "1111");
        type(panel, "2222");
        assertEquals(PanelState.READING, panel.state());
        assertEquals(2, panel.failCount());

        type(panel, "3333");
        assertEquals(PanelState.LOCKED, panel.state());
        assertEquals(3, panel.failCount());
        assertTrue(panel.isLocked());
    }

    @Test
    @DisplayName("锁定期间敲击按键被忽略")
    void ignoresKeysWhileLocked() {
        ControlPanel panel = newPanel();
        type(panel, "1111");
        type(panel, "2222");
        type(panel, "3333");
        assertFalse(panel.pressKey('2'));
        assertEquals(PanelState.LOCKED, panel.state());
        assertEquals("", panel.inputBuffer());
    }

    @Test
    @DisplayName("锁定不足 120 秒不解锁")
    void staysLockedBeforeTimeout() {
        ControlPanel panel = newPanel();
        type(panel, "1111");
        type(panel, "2222");
        type(panel, "3333");
        assertFalse(panel.elapse(119));
        assertEquals(PanelState.LOCKED, panel.state());
    }

    @Test
    @DisplayName("锁定满 120 秒后允许再次输入，错误计数清零")
    void unlocksAfterTwoMinutes() {
        ControlPanel panel = newPanel();
        type(panel, "1111");
        type(panel, "2222");
        type(panel, "3333");
        assertTrue(panel.elapse(120));
        assertEquals(PanelState.READING, panel.state());
        assertEquals(0, panel.failCount());

        type(panel, PASSWORD);
        assertEquals(PanelState.SELECTING, panel.state());
    }

    @Test
    @DisplayName("功能选择结束后回到空闲状态")
    void returnsToIdleAfterFunction() {
        ControlPanel panel = newPanel();
        type(panel, PASSWORD);
        assertTrue(panel.finishFunction());
        assertEquals(PanelState.IDLE, panel.state());
        assertFalse(panel.isFunctionIndicatorOn());
    }

    @Test
    @DisplayName("密码长度、错误次数、锁定时长符合需求约定")
    void constantsMatchRequirement() {
        assertEquals(4, PanelState.PASSWORD_LENGTH);
        assertEquals(3, PanelState.MAX_FAILURES);
        assertEquals(120, PanelState.LOCK_SECONDS);
    }
}
