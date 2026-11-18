package com.hj212.experiment4.fsm;

/**
 * 状态机中的状态。
 *
 * <p>为支持 B1 中「恒温烧水过程」这样的组合状态（composite state），
 * 状态本身带有一个可选的双亲状态：组合状态作为双亲，其子状态通过
 * {@link #parent()} 指回组合状态；顶层状态的 {@code parent()} 返回 {@code null}。</p>
 *
 * <p>状态以枚举实现，枚举常量名即状态图中的状态别名，保证「图—码」一一对应。</p>
 */
public interface State {

    /** 状态在状态图中的显示名（中文名，用于日志与追踪）。 */
    String label();

    /** 双亲状态；顶层状态返回 {@code null}。 */
    State parent();

    /**
     * 当前状态在状态图中的路径，例如 {@code 恒温烧水过程/加热烧水}。
     */
    default String path() {
        StringBuilder sb = new StringBuilder(label());
        for (State p = parent(); p != null; p = p.parent()) {
            sb.insert(0, p.label() + "/");
        }
        return sb.toString();
    }
}
