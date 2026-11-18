package com.hj212.experiment4.b1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B1 恒温水箱状态机的单元测试，逐条覆盖状态图中的迁移。 */
@DisplayName("B1 恒温水箱状态图")
class WaterTankControllerTest {

    @Test
    @DisplayName("系统启动后进入恒温烧水过程的初始子状态：待机，继电器断开")
    void startsInIdle() {
        WaterTankController tank = new WaterTankController();
        assertEquals(TankState.IDLE, tank.state());
        assertFalse(tank.isRelayOn());
        assertTrue(tank.isSampling());
    }

    @Test
    @DisplayName("温度<20℃且有水：打开继电器电源开始烧水")
    void heatsWhenColdAndWaterAvailable() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        assertEquals(TankState.HEATING, tank.state());
        assertTrue(tank.isRelayOn());
    }

    @Test
    @DisplayName("温度<20℃但无水：进入缺水保护，不打开电源")
    void blocksHeatingWhenNoWater() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, false);
        assertEquals(TankState.NO_WATER, tank.state());
        assertFalse(tank.isRelayOn());
        assertEquals(1, tank.alarms().size());
    }

    @Test
    @DisplayName("缺水保护状态下注水后重新开始烧水")
    void resumesHeatingAfterRefill() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, false);
        tank.sample(16, true);
        assertEquals(TankState.HEATING, tank.state());
        assertTrue(tank.isRelayOn());
    }

    @Test
    @DisplayName("温度回升到20℃以上时，缺水保护回到待机")
    void returnsToIdleWhenWarm() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, false);
        tank.sample(25, true);
        assertEquals(TankState.IDLE, tank.state());
        assertFalse(tank.isRelayOn());
    }

    @Test
    @DisplayName("温度达到100℃：断开电源回到待机")
    void stopsAtOneHundredDegrees() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        tank.sample(100, true);
        assertEquals(TankState.IDLE, tank.state());
        assertFalse(tank.isRelayOn());
    }

    @Test
    @DisplayName("加热过程中检测到无水：立即断电并进入缺水保护")
    void stopsHeatingWhenWaterRunsOut() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        tank.sample(60, false);
        assertEquals(TankState.NO_WATER, tank.state());
        assertFalse(tank.isRelayOn());
    }

    @Test
    @DisplayName("20℃~100℃ 之间、水量正常时的读数不改变状态（恒温循环）")
    void keepsHeatingBetweenLimits() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        tank.sample(55, true);
        assertEquals(TankState.HEATING, tank.state());
        assertTrue(tank.isRelayOn());
    }

    @Test
    @DisplayName("多次 20℃→100℃ 循环，形成恒温过程")
    void cyclesRepeatedly() {
        WaterTankController tank = new WaterTankController();
        for (int i = 0; i < 3; i++) {
            tank.sample(19, true);
            assertEquals(TankState.HEATING, tank.state());
            tank.sample(100, true);
            assertEquals(TankState.IDLE, tank.state());
            assertFalse(tank.isRelayOn());
        }
    }

    @Test
    @DisplayName("水箱烧坏：进入故障维修并报警，维修完成后回到待机")
    void goesToRepairOnFault() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        tank.reportFault();
        assertEquals(TankState.REPAIR, tank.state());
        assertFalse(tank.isRelayOn());

        tank.repair();
        assertEquals(TankState.IDLE, tank.state());
        assertTrue(tank.isSampling());
    }

    @Test
    @DisplayName("23:00 自动断开电源进入休眠，期间采样停止")
    void sleepsAtElevenPm() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        tank.clock(23, 0);
        assertEquals(TankState.SLEEP, tank.state());
        assertFalse(tank.isRelayOn());
        assertFalse(tank.isSampling());
    }

    @Test
    @DisplayName("休眠期间传感器读数被忽略")
    void ignoresSensorWhileSleeping() {
        WaterTankController tank = new WaterTankController();
        tank.clock(23, 0);
        tank.sample(10, true);
        assertEquals(TankState.SLEEP, tank.state());
        assertFalse(tank.isRelayOn());
    }

    @Test
    @DisplayName("07:00 自动恢复恒温烧水过程")
    void wakesAtSevenAm() {
        WaterTankController tank = new WaterTankController();
        tank.sample(15, true);
        tank.clock(23, 0);
        tank.clock(7, 0);
        assertEquals(TankState.IDLE, tank.state());
        assertTrue(tank.isSampling());

        tank.sample(15, true);
        assertEquals(TankState.HEATING, tank.state());
    }

    @Test
    @DisplayName("非 23:00 / 07:00 的时钟事件不引起迁移")
    void ignoresOtherClockTicks() {
        WaterTankController tank = new WaterTankController();
        tank.clock(12, 30);
        assertEquals(TankState.IDLE, tank.state());
    }
}
