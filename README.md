# 软件工程实验四：面向对象的系统建模 B（状态图）

> 课程：软件工程　　学号：202410120323　　姓名：柳淳

本仓库为「软件工程实验四：面向对象的系统建模 B」的完整实现，包含 B1、B2、B3 三个子任务的
**UML 状态图模型**、**可运行的状态机实现** 与 **单元测试**。

- B1：恒温水箱 20℃~100℃ 的状态图（含组合状态、时间事件、异常状态）
- B2：SafeHome 控制面板 `ControlPanel` 的状态图（密码输入、校验、锁定与解锁）
- B3：源文件字符串提取的状态图，并按图实现为可执行的词法扫描状态机

---

## 一、实验内容与产物对照

| 子任务 | 内容 | 状态图源文件 | 状态图 | 代码包 | 用例数 |
|---|---|---|---|---|---|
| B1 | 恒温水箱 20℃~100℃ 恒温控制 | `model/b1-water-tank.puml` | `model/diagrams/B1_WaterTank.png` | `b1` | 14 |
| B2 | SafeHome 控制面板 ControlPanel | `model/b2-control-panel.puml` | `model/diagrams/B2_ControlPanel.png` | `b2` | 11 |
| B3 | 源文件字符串提取 | `model/b3-string-extractor.puml` | `model/diagrams/B3_StringExtractor.png` | `b3` | 11 |

三张状态图统一采用「事件 [监护条件] / 动作」的转移标签；状态别名与代码中的枚举常量一一对应。

---

## 二、目录结构

```
.
├── model/                                  # 状态图模型
│   ├── b1-water-tank.puml                   # B1 恒温水箱状态图（PlantUML）
│   ├── b2-control-panel.puml                # B2 ControlPanel 状态图
│   ├── b3-string-extractor.puml             # B3 字符串提取状态图
│   └── diagrams/                            # 导出的 PNG（图 4-1 ~ 图 4-3）
│
├── code/                                    # Java 实现与单元测试
│   ├── build.gradle                         # Gradle 构建脚本（UTF-8，JUnit 5）
│   ├── settings.gradle
│   └── src/
│       ├── main/java/com/hj212/experiment4/
│       │   ├── Main.java                    # 演示程序：依次运行三台状态机并打印轨迹
│       │   ├── fsm/State.java               # 状态接口（含组合状态的双亲指针）
│       │   ├── fsm/Transition.java          # 一条迁移
│       │   ├── fsm/StateMachine.java        # 表驱动分层状态机引擎
│       │   ├── b1/TankState.java 等          # B1 状态与事件
│       │   ├── b1/WaterTankController.java  # B1 状态机
│       │   ├── b2/PanelState.java 等         # B2 状态与事件
│       │   ├── b2/ControlPanel.java         # B2 状态机
│       │   ├── b3/ScanState.java 等          # B3 状态与事件
│       │   └── b3/StringExtractor.java      # B3 状态机
│       └── test/java/com/hj212/experiment4/
│           ├── b1/WaterTankControllerTest.java
│           ├── b2/ControlPanelTest.java
│           └── b3/StringExtractorTest.java
│
├── tools/                                   # 辅助脚本
│   ├── plantuml.jar                         # 状态图渲染
│   ├── gen_report.py                        # 生成实验报告 docx
│   └── preview.py                           # 报告导出 PDF 并分页预览（自检排版用）
│
└── 202410120323-柳淳-软件工程实验四.docx      # 实验报告
```

---

## 三、三个状态机的建模要点

### B1 恒温水箱（20℃~100℃）

- **状态**：组合状态「恒温烧水过程」（内含 待机、加热烧水、缺水保护）+ 顶层状态「休眠」「故障维修」。
- **为什么用组合状态**：23:00 断电与 07:00 恢复对所有工作子状态同样生效，画在组合状态边界上
  只需两条迁移，避免从每个子状态各画一条到「休眠」的箭头。
- **缺水**建模为独立状态而非监护条件：无水需要持续提示加水，注水后要能自动恢复加热；
  温度阈值（< 20℃、= 100℃）只是数值判断，用监护条件表达。
- **异常**：「水箱烧坏」从「加热烧水」直接跨出组合状态指向「故障维修」；
  「维修完成」指向组合状态本身，由其初始伪状态下沉到「待机」，表达「重新开始一轮恒温过程」。

### B2 ControlPanel

- **状态**：空闲、读取键入字符、校验密码、锁定、系统功能选择，共 5 个状态 8 条迁移。
- 「每输入一个字符即与密码长度比较」表达为「读取键入字符」的自反迁移（字符数 < 4）与
  指向「校验密码」的迁移（字符数 = 4）两个互斥监护条件。
- 「锁定」的 `after(120 秒)` 是 UML 时间事件；锁定期间没有按键迁移，事件被丢弃，即按键被忽略。

### B3 源文件字符串提取

- **状态**：扫描普通字符、字符串内、转义，外加终态。
- 转义状态保证 `\"` 与 `\\` 不会被误认为字符串界定符；反斜杠被消化，只保留被转义的字符。
- 两种结束方式：扫描态遇到文件结束为正常结束；字符串内（或转义中）遇到文件结束视为
  「字符串未闭合」，丢弃残片并报错。

---

## 四、状态机引擎

三张图都是「状态 + 迁移表」，因此实现为表驱动（table-driven）的分层状态机，支持的 UML 语义：

| 语义 | 说明 |
|---|---|
| 事件派发 | 按「当前状态 → 双亲状态 → …」查找第一条事件类型匹配且监护条件成立的迁移（子状态优先） |
| 组合状态 | 迁移目标若为组合状态，沿其初始伪状态下沉到默认子状态 |
| 进入动作 | 迁移后按「由外到内」执行本次新进入的各级状态的 `entry` 动作 |
| 未定义事件 | `fireIfPossible` 返回 false（事件被丢弃），用于「锁定期间按键忽略」等场景 |

图中每条箭头都能在代码中找到一行 `transition(...)` 声明，模型与代码严格一一对应。

---

## 五、运行方式

### 环境

* JDK 1.8.0_131
* Gradle 7.6.4
* JUnit 5.8.2

### 构建与测试

```bash
cd code
gradle test          # 运行 36 个单元测试
gradle run           # 运行演示程序，打印三台状态机的迁移轨迹
```

### 重新生成状态图

先下载 [PlantUML](https://plantuml.com/download) 的 `plantuml.jar` 放到 `tools/`（仓库未提交该 jar），然后：

```bash
java -jar tools/plantuml.jar -charset UTF-8 -tpng -o diagrams model/*.puml
```

生成的 PNG 位于 `model/diagrams/`（注意 `-o` 相对于源文件所在目录）。

### 重新生成实验报告

```bash
python tools/gen_report.py     # 生成 202410120323-柳淳-软件工程实验四.docx
python tools/make_doc.py       # 用本机 Word 另存为真正的 .doc（需 pywin32）
python tools/preview.py        # 导出 PDF 并逐页出图，自检排版
```

报告第六节的仓库地址由 `tools/gen_report.py` 顶部的 `REPO_URL` 变量决定，
换了仓库只需改这一个常量后重新生成。

### 推送到 Gitee

Gitee 自 2022 年起已停用「账号 + 密码」方式的推送，需使用私人令牌：

```bash
python tools/push_to_gitee.py <你的gitee私人令牌>
```

令牌在 Gitee「设置 → 安全设置 → 私人令牌」生成，勾选 `projects` 权限。
脚本会自动创建 `skinpipi/se-exp4-statemodel` 仓库并推送 `master` 分支。

---

## 六、测试结果

```
BUILD SUCCESSFUL
36 tests completed, 0 failed
```

| 测试类 | 用例数 | 覆盖要点 |
|---|---|---|
| `WaterTankControllerTest` | 14 | 初始状态、有/无水加热、注水恢复、100℃ 断电、加热中断水、恒温循环、烧坏与维修、23:00 休眠与 07:00 恢复、非法时钟无迁移 |
| `ControlPanelTest` | 11 | 上电空闲、逐位输入、满 4 位校验、密码正确、错误累加、3 次锁定、锁定期间按键忽略、120 秒解锁、功能结束返回空闲 |
| `StringExtractorTest` | 11 | 题目示例、转义双引号、转义反斜杠、多字符串、空字符串、非字符串文本、未闭合报错、状态迁移轨迹、终态后拒绝输入 |
