package com.hj212.experiment4.b1;

import com.hj212.experiment4.fsm.StateMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B1 恒温水箱控制器：状态图 {@code model/b1-water-tank.puml} 的可执行实现。
 *
 * <p>状态机对继电器（电源）与采样线程的开关效果，通过状态的 entry 动作落实：
 * 加热烧水 entry 时闭合继电器，待机/缺水保护/休眠/维修 entry 时断开继电器。</p>
 *
 * <p>20℃~100℃ 的恒温过程由「待机 ←→ 加热烧水」的循环表达：
 * 温度低于 20℃ 且有水即通电加热，升到 100℃ 断电；温度回落到 20℃ 以下再次加热。</p>
 */
public final class WaterTankController {

    static final double LOW_TEMPERATURE = 20.0;
    static final double HIGH_TEMPERATURE = 100.0;

    private final StateMachine<TankState, TankEvent> machine;
    private final List<String> alarms = new ArrayList<String>();

    private boolean relayOn;
    private boolean sampling;
    private double lastTemperature = Double.NaN;

    public WaterTankController() {
        this.machine = buildMachine();
    }

    /** 按状态图逐条声明迁移与 entry 动作。 */
    private StateMachine<TankState, TankEvent> buildMachine() {
        StateMachine.Builder<TankState, TankEvent> b =
                StateMachine.<TankState, TankEvent>builder(TankState.WORKING).initial(TankState.WORKING, TankState.IDLE);

        // 系统启动 → 恒温烧水过程（内部初始伪状态 → 待机）
        b.transition(TankState.IDLE, SensorReading.class,
                r -> r.getTemperature() < LOW_TEMPERATURE && r.isWaterAvailable(),
                TankState.HEATING);
        b.transition(TankState.IDLE, SensorReading.class,
                r -> r.getTemperature() < LOW_TEMPERATURE && !r.isWaterAvailable(),
                TankState.NO_WATER);

        b.transition(TankState.NO_WATER, SensorReading.class,
                r -> r.getTemperature() < LOW_TEMPERATURE && r.isWaterAvailable(),
                TankState.HEATING);
        b.transition(TankState.NO_WATER, SensorReading.class,
                r -> r.getTemperature() >= LOW_TEMPERATURE,
                TankState.IDLE);

        b.transition(TankState.HEATING, SensorReading.class,
                r -> r.getTemperature() >= HIGH_TEMPERATURE,
                TankState.IDLE);
        b.transition(TankState.HEATING, SensorReading.class,
                r -> !r.isWaterAvailable(),
                TankState.NO_WATER);
        b.transition(TankState.HEATING, BoilerFault.class, TankState.REPAIR);

        b.transition(TankState.REPAIR, RepairDone.class, TankState.WORKING);

        // 23:00 / 07:00 的时钟事件作用在组合状态边界上
        b.transition(TankState.WORKING, ClockEvent.class,
                e -> e.at(23, 0), TankState.SLEEP);
        b.transition(TankState.SLEEP, ClockEvent.class,
                e -> e.at(7, 0), TankState.WORKING);

        // 状态的 entry 动作（状态框中 entry / ... 的部分）
        b.entry(TankState.WORKING, e -> {
            sampling = true;
        });
        b.entry(TankState.IDLE, e -> relayOn = false);
        b.entry(TankState.HEATING, e -> relayOn = true);
        b.entry(TankState.NO_WATER, e -> {
            relayOn = false;
            alarms.add("水量传感器检测到无水，不打开电源烧水");
        });
        b.entry(TankState.SLEEP, e -> {
            relayOn = false;
            sampling = false;
        });
        b.entry(TankState.REPAIR, e -> {
            relayOn = false;
            alarms.add("水箱烧坏，停机等待维修");
        });

        return b.build();
    }

    // ---------------------------- 外部事件入口 ----------------------------

    /** 温度/水量传感器上报一次采样。读数不引起迁移时被忽略。 */
    public void sample(double temperature, boolean waterAvailable) {
        this.lastTemperature = temperature;
        machine.fireIfPossible(new SensorReading(temperature, waterAvailable));
    }

    /** 时钟上报一个时刻，仅在 23:00 与 07:00 引起迁移。 */
    public void clock(int hour, int minute) {
        machine.fireIfPossible(new ClockEvent(hour, minute));
    }

    /** 上报水箱烧坏。 */
    public void reportFault() {
        machine.fireIfPossible(new BoilerFault());
    }

    /** 上报维修完成。 */
    public void repair() {
        machine.fireIfPossible(new RepairDone());
    }

    // ------------------------------ 观测接口 ------------------------------

    public TankState state() {
        return machine.current();
    }

    public boolean isRelayOn() {
        return relayOn;
    }

    public boolean isSampling() {
        return sampling;
    }

    public double getLastTemperature() {
        return lastTemperature;
    }

    public List<String> alarms() {
        return Collections.unmodifiableList(alarms);
    }

    public List<String> trace() {
        return machine.trace();
    }
}
