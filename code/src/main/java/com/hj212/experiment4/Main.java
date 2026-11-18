package com.hj212.experiment4;

import com.hj212.experiment4.b1.WaterTankController;
import com.hj212.experiment4.b2.ControlPanel;
import com.hj212.experiment4.b3.StringExtractor;

import java.util.Arrays;
import java.util.List;

/**
 * 演示程序：依次运行 B1、B2、B3 三个状态机模型，打印状态迁移轨迹。
 *
 * <p>运行方式：{@code gradle run}</p>
 */
public final class Main {

    public static void main(String[] args) {
        b1();
        b2();
        b3();
    }

    private static void b1() {
        System.out.println("========== B1 恒温水箱（20℃~100℃） ==========");
        WaterTankController tank = new WaterTankController();
        System.out.println("启动后：" + tank.state().path() + "，继电器=" + tank.isRelayOn());

        tank.sample(15, false);                       // 低于 20℃ 但无水
        System.out.println("15℃ 无水：" + tank.state().path() + "，继电器=" + tank.isRelayOn());

        tank.sample(15, true);                        // 注水后开始烧水
        System.out.println("15℃ 有水：" + tank.state().path() + "，继电器=" + tank.isRelayOn());

        tank.sample(60, true);                        // 升温途中，无迁移
        tank.sample(100, true);                       // 到达 100℃ 断电
        System.out.println("100℃：" + tank.state().path() + "，继电器=" + tank.isRelayOn());

        tank.sample(95, true);                        // 温度回落，继续待机
        tank.sample(18, true);                        // 再次低于 20℃，重新加热
        System.out.println("18℃ 有水：" + tank.state().path() + "，继电器=" + tank.isRelayOn());

        tank.clock(23, 0);                            // 23:00 进入休眠
        System.out.println("23:00：" + tank.state().path() + "，继电器=" + tank.isRelayOn()
                + "，采样=" + tank.isSampling());

        tank.clock(7, 0);                             // 07:00 恢复
        System.out.println("07:00：" + tank.state().path() + "，继电器=" + tank.isRelayOn());

        tank.sample(15, true);                        // 恢复到烧水状态
        tank.reportFault();                           // 水箱烧坏
        System.out.println("烧坏：" + tank.state().path() + "，继电器=" + tank.isRelayOn()
                + "，报警=" + tank.alarms());
        tank.repair();
        System.out.println("维修完成：" + tank.state().path());

        System.out.println("-- 迁移轨迹 --");
        for (String line : tank.trace()) {
            System.out.println("   " + line);
        }
        System.out.println();
    }

    private static void b2() {
        System.out.println("========== B2 SafeHome 控制面板 ControlPanel ==========");
        ControlPanel panel = new ControlPanel("2580");
        System.out.println("上电：" + panel.state().label());

        for (char c : "2580".toCharArray()) {
            panel.pressKey(c);
        }
        System.out.println("输入正确密码 2580：" + panel.state().label()
                + "，功能指示灯=" + panel.isFunctionIndicatorOn());
        panel.finishFunction();

        // 连续三次输错密码
        for (int attempt = 1; attempt <= 3; attempt++) {
            for (char c : "1111".toCharArray()) {
                panel.pressKey(c);
            }
            System.out.println("第 " + attempt + " 次输错：" + panel.state().label()
                    + "，累计错误=" + panel.failCount());
        }
        System.out.println("锁定期间按键被接受？" + panel.pressKey('2'));

        panel.elapse(100);
        System.out.println("锁定 100 秒：" + panel.state().label());
        panel.elapse(20);
        System.out.println("锁定 120 秒：" + panel.state().label()
                + "，累计错误=" + panel.failCount());

        System.out.println("-- 迁移轨迹 --");
        for (String line : panel.trace()) {
            System.out.println("   " + line);
        }
        System.out.println();
    }

    private static void b3() {
        System.out.println("========== B3 源文件字符串提取 ==========");
        String source = "\"一般学习一门编程语言的时候，基本上第一个程序是输出\\\"Hello World!\\\"\"";
        System.out.println("源文件片段：" + source);
        System.out.println("提取结果  ：" + StringExtractor.extract(source));

        String code = "String a = \"x\"; char q = '\\''; String b = \"反斜杠:\\\\\";";
        System.out.println("源文件片段：" + code);
        System.out.println("提取结果  ：" + StringExtractor.extract(code));

        StringExtractor extractor = new StringExtractor();
        extractor.feed("\"未闭合的字符串");
        List<String> result = extractor.finish();
        System.out.println("未闭合示例：" + Arrays.toString(result.toArray())
                + "，是否未闭合=" + extractor.isUnterminated());

        System.out.println();
    }

    private Main() {
    }
}
