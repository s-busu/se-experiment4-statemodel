package com.hj212.experiment4.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 表驱动的分层（UML）状态机解释器。
 *
 * <p>把状态图直接翻译成「迁移表」，由本类解释执行。支持的 UML 语义：</p>
 * <ol>
 *   <li><b>事件派发</b>：{@link #fire(Object)} 按「当前状态 → 双亲状态 → …」的
 *       顺序查找第一条事件类型匹配且监护条件成立的迁移，即子状态优先（innermost-first）。</li>
 *   <li><b>组合状态</b>：迁移目标若是组合状态，则沿其初始伪状态下沉到默认子状态，
 *       与状态图中「指向组合状态外框的箭头」语义一致。</li>
 *   <li><b>进入动作</b>：迁移到目标后，按「由外到内」的顺序执行本次新进入的各级状态的
 *       entry 动作；已处于其中的外层组合状态不重复执行。</li>
 *   <li><b>未定义事件</b>：若没有任何迁移可响应，本实现抛
 *       {@link IllegalStateException}；调用方可用 {@link #accepts(Object)} 先行探测，
 *       从而实现「锁定期间按键被忽略」这类忽略语义。</li>
 * </ol>
 *
 * @param <S> 状态类型（枚举）
 * @param <E> 事件类型
 */
public final class StateMachine<S extends State, E> {

    private final S rootInitial;
    private final Map<S, S> initialOf;
    private final List<Transition<S, E>> transitions;
    private final Map<S, Consumer<E>> entryActions;
    private final List<String> trace = new ArrayList<String>();

    private S current;

    private StateMachine(Builder<S, E> b) {
        this.rootInitial = b.rootInitial;
        this.initialOf = new LinkedHashMap<S, S>(b.initialOf);
        this.transitions = new ArrayList<Transition<S, E>>(b.transitions);
        this.entryActions = new LinkedHashMap<S, Consumer<E>>(b.entryActions);
        this.current = resolveTarget(b.rootInitial);
        runEntry(current, Collections.<State>emptyList(), null);
        trace.add("初始状态：" + current.path());
    }

    /** 当前状态（永远是最内层子状态）。 */
    public S current() {
        return current;
    }

    /** 当前是否处于状态 {@code s} 或其内部（组合状态包含其全部子状态）。 */
    public boolean isIn(State s) {
        for (State p = current; p != null; p = p.parent()) {
            if (p == s) {
                return true;
            }
        }
        return false;
    }

    /** 当前状态能否响应事件 {@code event}（不改变状态）。 */
    public boolean accepts(E event) {
        return find(event) != null;
    }

    /**
     * 派发一个事件。
     *
     * @return 迁移之后的状态
     * @throws IllegalStateException 当前状态无法响应该事件
     */
    public S fire(E event) {
        Transition<S, E> t = find(event);
        if (t == null) {
            throw new IllegalStateException(
                    "状态 " + current.path() + " 无法响应事件 " + event);
        }
        List<State> before = chainOf(current);
        S target = resolveTarget(t.to());
        current = target;
        runEntry(target, before, event);
        trace.add(t.from().path() + " --[" + event + "]--> " + target.path());
        return target;
    }

    /**
     * 若当前状态能响应该事件则派发，否则忽略（对应 UML 中「未定义该事件的迁移，
     * 事件被丢弃」的语义，例如锁定期间敲击按键）。
     *
     * @return 是否发生了迁移
     */
    public boolean fireIfPossible(E event) {
        if (find(event) == null) {
            return false;
        }
        fire(event);
        return true;
    }

    /** 运行轨迹（按发生顺序），用于实验报告与调试。 */
    public List<String> trace() {
        return Collections.unmodifiableList(trace);
    }

    /** 清空轨迹。 */
    public void clearTrace() {
        trace.clear();
    }

    // ------------------------------------------------------------------

    private Transition<S, E> find(E event) {
        for (State s = current; s != null; s = s.parent()) {
            for (Transition<S, E> t : transitions) {
                if (t.from() == s && t.matches(event) && t.guardHolds(event)) {
                    return t;
                }
            }
        }
        return null;
    }

    /** 若目标状态是组合状态，则下沉到它的初始子状态。 */
    private S resolveTarget(S to) {
        S s = to;
        while (initialOf.containsKey(s)) {
            s = initialOf.get(s);
        }
        return s;
    }

    /** 由外到内的状态链，例如 [恒温烧水过程, 加热烧水]。 */
    private List<State> chainOf(State s) {
        List<State> chain = new ArrayList<State>();
        for (State p = s; p != null; p = p.parent()) {
            chain.add(0, p);
        }
        return chain;
    }

    private void runEntry(S target, List<State> before, E event) {
        for (State s : chainOf(target)) {
            if (before.contains(s)) {
                continue;
            }
            Consumer<E> action = entryActions.get(s);
            if (action != null) {
                action.accept(event);
            }
        }
    }

    // ------------------------------------------------------------------

    public static <S extends State, E> Builder<S, E> builder(S rootInitial) {
        return new Builder<S, E>(rootInitial);
    }

    /** 状态机构造器：以声明式的方式描述状态图的箭头。 */
    public static final class Builder<S extends State, E> {

        private final S rootInitial;
        private final Map<S, S> initialOf = new LinkedHashMap<S, S>();
        private final List<Transition<S, E>> transitions = new ArrayList<Transition<S, E>>();
        private final Map<S, Consumer<E>> entryActions = new LinkedHashMap<S, Consumer<E>>();

        private Builder(S rootInitial) {
            this.rootInitial = rootInitial;
        }

        /** 指定组合状态的初始子状态（状态图中组合状态内部的初始伪状态）。 */
        public Builder<S, E> initial(S composite, S subState) {
            initialOf.put(composite, subState);
            return this;
        }

        /**
         * 增设一条迁移：{@code from --[eventType(guard)]--> to}。
         *
         * <p>监护条件按具体事件类型书写，例如 {@code CharRead::isQuote}。</p>
         */
        public <T extends E> Builder<S, E> transition(S from, Class<T> eventType,
                                                     Predicate<T> guard, S to) {
            Predicate<E> wrapped = guard == null ? null : e -> guard.test(eventType.cast(e));
            transitions.add(new Transition<S, E>(from, eventType, wrapped, to));
            return this;
        }

        /** 增设一条无监护条件的迁移。 */
        public Builder<S, E> transition(S from, Class<? extends E> eventType, S to) {
            return transition(from, eventType, null, to);
        }

        /** 状态的 entry 动作（状态图中写在状态框内的 entry / ...）。 */
        public Builder<S, E> entry(S state, Consumer<E> action) {
            entryActions.put(state, action);
            return this;
        }

        public StateMachine<S, E> build() {
            return new StateMachine<S, E>(this);
        }
    }
}
