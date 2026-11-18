package com.hj212.experiment4.b2;

/** 事件：密码校验结果（校验完成后立即产生，属「完成事件」）。 */
public final class VerifyResult implements PanelEvent {

    private final boolean correct;

    public VerifyResult(boolean correct) {
        this.correct = correct;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public String toString() {
        return correct ? "校验结果(正确)" : "校验结果(错误)";
    }
}
