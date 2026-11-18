package com.hj212.experiment4.fsm;

import java.util.function.Predicate;

/**
 * 一条状态迁移（状态图中的一个箭头）。
 *
 * <p>对应状态图三要素中的后两个：</p>
 * <ul>
 *   <li><b>事件</b>：{@code eventType}，按事件类型匹配；</li>
 *   <li><b>监护条件</b>：{@code guard}，写在状态图方括号 [ ] 中的布尔表达式；</li>
 *   <li><b>源状态 / 目标状态</b>：{@code from} / {@code to}。</li>
 * </ul>
 *
 * <p>迁移上的动作（状态图中写在 "/" 之后的动作）由目标状态的 entry 动作承担，
 * 见 {@link StateMachine.Builder#entry(State, java.util.function.Consumer)}。</p>
 *
 * @param <S> 状态类型
 * @param <E> 事件类型
 */
public final class Transition<S extends State, E> {

    private final S from;
    private final Class<? extends E> eventType;
    private final Predicate<E> guard;
    private final S to;

    Transition(S from, Class<? extends E> eventType, Predicate<E> guard, S to) {
        this.from = from;
        this.eventType = eventType;
        this.guard = guard;
        this.to = to;
    }

    public S from() {
        return from;
    }

    public S to() {
        return to;
    }

    /** 事件类型是否匹配（子类事件同样匹配父类型）。 */
    boolean matches(Object event) {
        return event != null && eventType.isInstance(event);
    }

    /** 监护条件是否成立（无监护条件视为恒真）。 */
    @SuppressWarnings("unchecked")
    boolean guardHolds(Object event) {
        return guard == null || guard.test((E) event);
    }

    @Override
    public String toString() {
        return from.label() + " --> " + to.label();
    }
}
