package com.hj212.experiment4.b1;

/**
 * 事件：传感器周期采样（温度 + 水量）。
 *
 * <p>状态图中「温度 &lt; 20℃[有水]」这类标签，其事件部分即本事件，
 * 温度与水量作为监护条件的输入。</p>
 */
public final class SensorReading implements TankEvent {

    private final double temperature;
    private final boolean waterAvailable;

    public SensorReading(double temperature, boolean waterAvailable) {
        this.temperature = temperature;
        this.waterAvailable = waterAvailable;
    }

    public double getTemperature() {
        return temperature;
    }

    public boolean isWaterAvailable() {
        return waterAvailable;
    }

    @Override
    public String toString() {
        return String.format("采样(温度=%.0f℃, 有水=%s)", temperature, waterAvailable);
    }
}
