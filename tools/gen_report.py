# -*- coding: utf-8 -*-
"""
生成《软件工程》实验报告四：面向对象的系统建模 B（状态图）。

沿用课程官方模板 template/实验报告模板.docx 的封面版式，正文按模板的六段式结构
（一、实验目的 … 六、仓库地址）填写，嵌入三张状态图、状态与迁移明细表、
关键代码与测试结果。
"""
import os
import sys

from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = r"D:\软件工程\软件工程实验4"
TPL = os.path.join(ROOT, "template", "实验报告模板.docx")
OUT_DOCX = os.path.join(ROOT, "202410120323-柳淳-软件工程实验四.docx")
OUT_DOC = os.path.join(ROOT, "202410120323-柳淳-软件工程实验四.doc")
DIAG_DIR = os.path.join(ROOT, "model", "diagrams")

STUDENT_NAME = "柳淳"
STUDENT_NO = "202410120323"
COLLEGE = "计算机与信息学院"
MAJOR = "软件工程"
LAB = "软件工程实验室"
EXPERIMENT_DATE = "2026 年 11 月 18 日"
REPO_URL = "https://gitee.com/skinpipi/se-exp4-statemodel"
REPO_BACKUP_URL = "https://github.com/s-busu/se-experiment4-statemodel"

# 模板正文可用宽度：21.0 - 3.175*2 = 14.65 cm
CONTENT_W = 14.6


def fit_widths(widths):
    """把给定的列宽按比例压缩到正文可用宽度，避免表格溢出页边距。"""
    total = float(sum(widths))
    return [w * CONTENT_W / total for w in widths]


# ------------------------- 基础排版工具 -------------------------

def set_run(run, name="宋体", size=12, bold=False, ascii_font="Times New Roman"):
    run.font.name = ascii_font
    run.font.size = Pt(size)
    run.bold = bold
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    rfonts.set(qn("w:ascii"), ascii_font)
    rfonts.set(qn("w:hAnsi"), ascii_font)
    rfonts.set(qn("w:eastAsia"), name)


def strip_numbering(paragraph):
    """清除段落继承的自动编号（模板 Normal 样式带有 numPr，会串出列表序号）。"""
    ppr = paragraph._p.get_or_add_pPr()
    for numpr in ppr.findall(qn("w:numPr")):
        ppr.remove(numpr)
    numpr = OxmlElement("w:numPr")
    ilvl = OxmlElement("w:ilvl")
    ilvl.set(qn("w:val"), "0")
    numid = OxmlElement("w:numId")
    numid.set(qn("w:val"), "0")
    numpr.append(ilvl)
    numpr.append(numid)
    ppr.insert(0, numpr)


def add_para(doc, text="", size=12, bold=False, align=None, font="宋体",
             space_before=0, space_after=0, indent_first=True, line=None):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    if line:
        pf.line_spacing = line
    if align is not None:
        p.alignment = align
    else:
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    if indent_first and text:
        pf.first_line_indent = Pt(size * 2)
    if bold:
        pf.keep_together = True
        pf.keep_with_next = True
    strip_numbering(p)
    if text:
        set_run(p.add_run(text), name=font, size=size, bold=bold)
    return p


def add_heading(doc, text, size=14):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.keep_with_next = True
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    strip_numbering(p)
    set_run(p.add_run(text), name="黑体", size=size, bold=True)
    return p


def add_code(doc, lines, size=8.5):
    for line in lines:
        p = doc.add_paragraph()
        pf = p.paragraph_format
        pf.space_before = Pt(0)
        pf.space_after = Pt(0)
        pf.left_indent = Cm(0.9)
        pf.line_spacing = 1.0
        strip_numbering(p)
        set_run(p.add_run(line if line else " "), name="Consolas", size=size,
                ascii_font="Consolas")


def add_image(doc, path, caption, width_cm=15.5):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.add_run().add_picture(path, width=Cm(width_cm))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_after = Pt(10)
    strip_numbering(cap)
    set_run(cap.add_run(caption), name="宋体", size=10.5, bold=True)


def set_cell(cell, text, bold=False, size=10.5, align=None):
    cell.text = ""
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(1)
    p.paragraph_format.space_after = Pt(1)
    p.paragraph_format.first_line_indent = Pt(0)
    if align is not None:
        p.alignment = align
    else:
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    strip_numbering(p)
    parts = str(text).split("\n")
    for i, part in enumerate(parts):
        if i:
            p.add_run().add_break()
        set_run(p.add_run(part), name="宋体", size=size, bold=bold)


def shade_cell(cell, hex_color="D9D9D9"):
    tcpr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tcpr.append(shd)


def repeat_header(row):
    trpr = row._tr.get_or_add_trPr()
    th = OxmlElement("w:tblHeader")
    th.set(qn("w:val"), "true")
    trpr.append(th)


def add_table(doc, headers, rows, widths=None, size=9.5, caption=None,
              center_from=0):
    if caption:
        cap = doc.add_paragraph()
        cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
        cap.paragraph_format.space_before = Pt(6)
        cap.paragraph_format.space_after = Pt(3)
        cap.paragraph_format.keep_with_next = True
        strip_numbering(cap)
        set_run(cap.add_run(caption), name="宋体", size=10.5, bold=True)

    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    for i, h in enumerate(headers):
        set_cell(t.rows[0].cells[i], h, bold=True, size=size)
        shade_cell(t.rows[0].cells[i])
    repeat_header(t.rows[0])
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            al = WD_ALIGN_PARAGRAPH.CENTER if i < center_from else WD_ALIGN_PARAGRAPH.LEFT
            set_cell(cells[i], str(val), size=size, align=al)
    if widths:
        widths = fit_widths(widths)
        for r in t.rows:
            for i, w in enumerate(widths):
                r.cells[i].width = Cm(w)
    for idx_r, r in enumerate(t.rows):
        trpr = r._tr.get_or_add_trPr()
        trpr.append(OxmlElement("w:cantSplit"))
        if idx_r < len(t.rows) - 1:
            for c in r.cells:
                for cp in c.paragraphs:
                    cp.paragraph_format.keep_with_next = True
    sp = doc.add_paragraph()
    sp.paragraph_format.space_after = Pt(4)
    return t


def page_break(doc):
    from docx.enum.text import WD_BREAK
    p = doc.add_paragraph()
    p.add_run().add_break(WD_BREAK.PAGE)


# ------------------------- 模板封面 -------------------------

def build_cover(doc):
    for p in doc.paragraphs:
        text = "".join(p._p.itertext())
        stripped = text.strip()

        if stripped.startswith("实验报告一"):
            for r in list(p.runs):
                r._element.getparent().remove(r._element)
            strip_numbering(p)
            set_run(p.add_run("实验报告四 ：面向对象的系统建模B"),
                    name="楷体_GB2312", size=24, bold=True)

        elif stripped.startswith("姓    名："):
            for r in list(p.runs):
                r._element.getparent().remove(r._element)
            set_run(p.add_run("姓    名：%s          学    号：%s          "
                              % (STUDENT_NAME, STUDENT_NO)),
                    name="宋体", size=14, bold=True)

        elif stripped.startswith("院    系："):
            for r in list(p.runs):
                r._element.getparent().remove(r._element)
            set_run(p.add_run("院    系：%s          专  业：%s          "
                              % (COLLEGE, MAJOR)),
                    name="宋体", size=14, bold=True)

        elif stripped.startswith("实 验 室："):
            for r in list(p.runs):
                r._element.getparent().remove(r._element)
            set_run(p.add_run("实 验 室：%s        实验日期：%s    "
                              % (LAB, EXPERIMENT_DATE)),
                    name="宋体", size=14, bold=True)

        elif stripped.startswith("总评成绩："):
            for r in list(p.runs):
                r._element.getparent().remove(r._element)
            set_run(p.add_run("总评成绩：                        审阅教师：                  "),
                    name="宋体", size=14, bold=True)


def clear_body_after_cover(doc, keep_cover=True):
    body = doc.element.body
    children = list(body)
    keep = 0
    found = False
    for i, child in enumerate(children):
        tag = child.tag.split("}")[-1]
        if tag == "sectPr":
            continue
        if tag == "p" and "一、实验目的" in "".join(child.itertext()):
            keep = i
            found = True
            break
        keep = i + 1
    if not found or not keep_cover:
        keep = 0
    for child in children[keep:]:
        if child.tag.split("}")[-1] != "sectPr":
            body.remove(child)


# ------------------------- 表格数据 -------------------------

B1_STATES = [
    ["恒温烧水过程\n(Working)", "组合状态", "封装三个工作子状态；时钟事件作用在其边界上",
     "启动温度与水量采样"],
    ["待机\n(Idle)", "子状态", "水温不低于 20℃，等待温度回落", "断电"],
    ["加热烧水\n(Heating)", "子状态", "继电器闭合，水温由 20℃ 升至 100℃", "通电加热"],
    ["缺水保护\n(NoWater)", "子状态", "无水，不允许通电加热", "保持断电并提示加水"],
    ["休眠\n(Sleep)", "顶层状态", "23:00 至次日 07:00 整机断电", "断开电源，停止采样"],
    ["故障维修\n(Repair)", "顶层状态", "水箱烧坏，等待人工维修", "停机报警，等待维修"],
]

B1_TRANS = [
    ["T1", "待机", "采样", "温度 < 20℃ 且 有水", "加热烧水", "通电加热"],
    ["T2", "待机", "采样", "温度 < 20℃ 且 无水", "缺水保护", "保持断电"],
    ["T3", "缺水保护", "采样", "温度 < 20℃ 且 已注水", "加热烧水", "通电加热"],
    ["T4", "缺水保护", "采样", "温度 ≥ 20℃", "待机", "—"],
    ["T5", "加热烧水", "采样", "温度达到 100℃", "待机", "断电"],
    ["T6", "加热烧水", "采样", "加热过程中无水", "缺水保护", "断电"],
    ["T7", "加热烧水", "水箱烧坏", "—", "故障维修", "停机报警"],
    ["T8", "故障维修", "维修完成", "—", "恒温烧水过程\n（进入待机）", "复位故障标志"],
    ["T9", "恒温烧水过程", "时钟", "23:00", "休眠", "断开电源并休眠"],
    ["T10", "休眠", "时钟", "07:00", "恒温烧水过程\n（进入待机）", "恢复恒温烧水"],
]

B2_STATES = [
    ["空闲\n(Idle)", "控制面板上电后的初始状态，等待户主操作", "显示操作提示，关闭键盘指示灯"],
    ["读取键入字符\n(Reading)", "累积户主键入的密码字符，长度上限 4 位",
     "清空输入缓冲，已输入字符数置 0；do：每个按键与密码长度比较"],
    ["校验密码\n(Verifying)", "把 4 位输入串与系统保存的密码比对", "将输入串与系统密码比对"],
    ["锁定\n(Locked)", "连续输错 3 次后的惩罚状态，屏蔽一切按键输入",
     "启动 120 秒定时器，屏蔽全部按键"],
    ["系统功能选择\n(Selecting)", "密码正确后进入，等待户主选择安防功能",
     "点亮功能指示灯，等待功能选择"],
]

B2_TRANS = [
    ["T1", "空闲", "按键", "—", "读取键入字符", "开始接收密码，缓冲清空"],
    ["T2", "读取键入字符", "按键", "已输入字符数 < 4", "读取键入字符", "字符入缓冲，计数 +1"],
    ["T3", "读取键入字符", "按键", "已输入字符数 = 4", "校验密码", "提交密码校验"],
    ["T4", "校验密码", "校验结果", "密码正确", "系统功能选择", "清零错误计数"],
    ["T5", "校验密码", "校验结果", "密码错误且累计错误 < 3", "读取键入字符", "错误次数 +1，清空缓冲"],
    ["T6", "校验密码", "校验结果", "密码错误且累计错误 = 3", "锁定", "错误次数 +1，启动定时器"],
    ["T7", "锁定", "计时器 after(120 秒)", "锁定满 120 秒", "读取键入字符", "清零错误计数，允许重新输入"],
    ["T8", "系统功能选择", "功能结束或超时", "—", "空闲", "退出功能选择"],
]

B3_STATES = [
    ["扫描普通字符\n(Scanning)", "字符串之外，逐个读取源文件字符", "do：逐字符读取源文件"],
    ["字符串内\n(InString)", "已读到左引号，正在累积字符串内容", "do：普通字符追加到缓冲区"],
    ["转义\n(Escape)", "刚读到反斜杠，等待被转义的字符", "只保留被转义的字符"],
    ["结束\n(Finished)", "终态，对应状态图中的终态 [*]", "输出结果或报告未闭合错误"],
]

B3_TRANS = [
    ["T1", "扫描普通字符", "读到双引号", "—", "字符串内", "清空缓冲区，开始新字符串"],
    ["T2", "扫描普通字符", "读到其他字符", "—", "扫描普通字符", "—（单引号、注释符、空白均不处理）"],
    ["T3", "字符串内", "读到其他字符", "—", "字符串内", "追加到缓冲区"],
    ["T4", "字符串内", "读到反斜杠", "—", "转义", "—"],
    ["T5", "转义", "读到任意字符 c", "—", "字符串内", "追加 c，丢弃反斜杠"],
    ["T6", "字符串内", "读到双引号", "—", "扫描普通字符", "输出缓冲区内容，得到一个字符串"],
    ["T7", "扫描普通字符", "源文件结束", "—", "结束", "输出全部提取结果"],
    ["T8", "字符串内 / 转义", "源文件结束", "字符串未闭合", "结束", "丢弃残片并报告错误"],
]

TRACE_ROWS = [
    ["双引号", "Scanning", "InString", "（清空缓冲，开始新字符串）"],
    ["一", "InString", "InString", "一"],
    ["……普通字符持续追加……", "InString", "InString", "……"],
    ["反斜杠", "InString", "Escape", "一般学习…是输出"],
    ["双引号", "Escape", "InString", "一般学习…是输出\"  ← 转义的双引号被收进内容"],
    ["Hello World!", "InString", "InString", "一般学习…是输出\"Hello World!"],
    ["反斜杠", "InString", "Escape", "一般学习…是输出\"Hello World!"],
    ["双引号", "Escape", "InString", "一般学习…是输出\"Hello World!\""],
    ["双引号", "InString", "Scanning", "输出：一般学习…是输出\"Hello World!\""],
]

# B3 逐字符推演示意（等宽排版）
TRACE_TABLE = r"""
输入字符       迁移前状态     迁移后状态    缓冲区内容
"              Scanning      InString      （清空缓冲，开始新字符串）
一              InString      InString      一
…（普通字符持续追加）
\              InString      Escape        一般学习…是输出
"              Escape        InString      一般学习…是输出"   ← 转义的双引号被收进内容
Hello World!   InString      InString      一般学习…是输出"Hello World!
\              InString      Escape        一般学习…是输出"Hello World!
"              Escape        InString      一般学习…是输出"Hello World!"
"              InString      Scanning      输出：一般学习…是输出"Hello World!"
"""

TEST_ROWS = [
    ["b1", "WaterTankControllerTest", "14",
     "初始状态、低温和有/无水加热、注水恢复、100℃ 断电、加热中断水、"
     "20℃~100℃ 恒温循环、烧坏与维修、23:00 休眠与 07:00 恢复、非法时钟无迁移"],
    ["b2", "ControlPanelTest", "11",
     "上电空闲、逐位输入、满 4 位校验、密码正确进入功能选择、"
     "错误计数累加、3 次锁定、锁定期间按键被忽略、120 秒解锁、功能结束返回空闲"],
    ["b3", "StringExtractorTest", "11",
     "题目示例、转义双引号、转义反斜杠、多字符串、空字符串、非字符串文本、"
     "转义序列、未闭合报错、状态迁移轨迹、终态后拒绝输入"],
]


# ------------------------- 报告正文 -------------------------

def build_report():
    doc = Document(TPL)
    build_cover(doc)
    clear_body_after_cover(doc, keep_cover=True)
    page_break(doc)

    # ---------- 一、实验目的 ----------
    add_heading(doc, "一、实验目的")
    for t in [
        "掌握 UML 状态图的绘制方法，充分理解状态图的三要素——状态（State）、"
        "转移（Transition）以及事件（Event）和与之相伴的监护条件与动作；",
        "理解状态图模型的意义：状态图描述一个对象在生命周期中响应事件所经历的状态序列，"
        "它把分散在条件分支中的行为集中表达为「对象当前处于什么状态、"
        "在什么事件下迁移到什么状态」，是实时控制类、协议类、界面交互类系统的重要建模手段；",
        "能够从实际问题（自然语言需求）中识别状态、事件、监护条件与动作，"
        "构建正确的状态图模型，并用程序实现该状态机，使模型可执行、可验证。",
    ]:
        add_para(doc, t)

    # ---------- 二、实验环境 ----------
    add_heading(doc, "二、实验环境")
    add_para(doc, "建模工具：Visual Paradigm（状态图绘制工具）、PlantUML 1.2024.7"
                  "（按 UML 2.5 语义绘制状态图并导出 PNG）；")
    add_para(doc, "实现语言：Java（JDK 1.8.0_131），采用表驱动的分层状态机实现；")
    add_para(doc, "构建工具：Gradle 7.6.4；测试框架：JUnit 5.8.2；")
    add_para(doc, "版本管理：Git 2.55.0 + Gitee（码云）；")
    add_para(doc, "操作系统：Windows 11（64 位）。")

    # ---------- 三、实验要求 ----------
    add_heading(doc, "三、实验要求")
    for t in [
        "按照实验报告模板编写实验报告，模型图贴到文档的适当位置；",
        "使用自己熟悉的语言实现状态机模型，源代码的 gitee 地址写到实验报告中；",
        "文件以「学号-姓名-软件工程实验四.doc」的方式命名，提交到长江雨课堂「软件工程实验四」；",
        "独立完成三个子任务：B1 绘制恒温水箱 20℃~100℃ 的状态图；"
        "B2 针对 SafeHome 控制面板类 ControlPanel 的状态建模；"
        "B3 采用状态图/状态机建模并实现源文件字符串提取。",
    ]:
        add_para(doc, t)

    # ---------- 四、实验内容 ----------
    add_heading(doc, "四、实验内容")
    add_para(doc, "本实验包含 B1、B2、B3 三个子任务，前两个任务重在「由需求建状态图」，"
                  "第三个任务要求「建图 + 实现」，因此三张状态图都进一步被实现为可运行的状态机，"
                  "并用单元测试验证图中的每一条迁移。三张图统一采用如下记号："
                  "圆角矩形表示状态（状态框内书写 entry 与 do 动作），"
                  "实心圆表示初始伪状态，同心圆表示终态，"
                  "带箭头的实线表示转移，转移标签采用「事件 [监护条件] / 动作」的格式。")

    # ===== 4.1 B1 =====
    add_heading(doc, "4.1 B1 恒温水箱 20℃~100℃ 的状态图", size=12.5)
    add_para(doc, "需求描述与状态识别：", bold=True, indent_first=False)
    for t in [
        "水箱启动后进入「恒温烧水」过程。温度传感器检测到水温低于 20℃ 时："
        "若水量传感器检测到水箱中有水，则打开继电器电源开始烧水；"
        "若检测到无水，则不打开电源烧水。",
        "温度达到 100℃ 时断开电源；温度回落到 20℃ 以下又重新加热，"
        "二者构成 20℃~100℃ 的恒温循环。若水箱烧坏，则进行维修。",
        "晚上 11 点整自动断开电源进入休眠，早上 7 点整自动恢复恒温烧水过程。",
    ]:
        add_para(doc, "• " + t, size=11)

    add_para(doc, "由上述描述可以识别出 6 个状态（1 个组合状态 + 5 个简单状态）"
                  "和 10 条迁移。三个工作子状态（待机、加热烧水、缺水保护）由组合状态"
                  "「恒温烧水过程」封装，理由是：23:00 与 07:00 的时钟事件"
                  "「无论当前处于哪个工作子状态都要执行」，把它画在组合状态的边界上，"
                  "只需两条迁移即可表达，避免从每个子状态各画一条到休眠的箭头，"
                  "这正是组合状态（composite state）的价值所在。", size=11)

    add_image(doc, os.path.join(DIAG_DIR, "B1_WaterTank.png"),
              "图 4-1  恒温水箱 20℃~100℃ 状态图", width_cm=12.2)
    add_para(doc, "图中转移标签采用「事件 [监护条件] / 动作」的格式；"
                  "为保持图形紧凑，标签中的动作写得较短（如「通电」「断电」），"
                  "其完整含义由目标状态的 entry 动作给出，见表 4-1、表 4-2。",
             size=10.5)

    add_table(doc, ["状态（别名）", "类型", "含义", "entry 动作"],
              B1_STATES, widths=[3.3, 1.9, 6.2, 4.6], size=9,
              caption="表 4-1  B1 状态集合", center_from=0)

    add_table(doc, ["编号", "源状态", "事件", "监护条件", "目标状态", "动作"],
              B1_TRANS, widths=[1.4, 2.5, 1.9, 3.5, 3.1, 3.2], size=9,
              caption="表 4-2  B1 状态迁移明细", center_from=0)

    add_para(doc, "建模要点：", bold=True, indent_first=False)
    for t in [
        "「无水不加热」既可写成监护条件，也可建模为独立状态。本实验选择后者：无水需要持续"
        "提示加水，注水后还要能自动恢复加热，做成「缺水保护」状态才可见、可观测；"
        "温度阈值（< 20℃、= 100℃）只是数值判断，用监护条件表达即可。",
        "「烧坏 → 维修」从「加热烧水」直接跨出组合状态指向「故障维修」，表示一旦烧坏即退出"
        "整个恒温过程；「维修完成」指向组合状态本身，由其初始伪状态下沉到「待机」，"
        "准确表达「重新开始一轮恒温过程」。",
    ]:
        add_para(doc, "• " + t, size=10.5)

    # ===== 4.2 B2 =====
    add_heading(doc, "4.2 B2 SafeHome 控制面板 ControlPanel 的状态图", size=12.5)
    add_para(doc, "需求描述与状态识别：", bold=True, indent_first=False)
    for t in [
        "户主敲击键盘按键输入密码时，控制面板处于「读取键入字符」状态；",
        "每输入一个字符就与设定的密码长度（4 位）比较，达到 4 位即校验密码是否正确；",
        "输入密码错误 3 次则锁定控制面板，锁定时间超过 120 秒后再次允许输入；",
        "密码正确则进入「系统功能选择」状态。",
    ]:
        add_para(doc, "• " + t, size=11)

    add_para(doc, "识别结果：空闲、读取键入字符、校验密码、锁定、系统功能选择共 5 个状态，"
                  "8 条迁移。其中「读取键入字符 → 读取键入字符」的自反迁移与"
                  "「读取键入字符 → 校验密码」的分支，正是「每输入一个字符即与密码长度比较」"
                  "的图形化表达；「锁定」状态的 after(120 秒) 是 UML 的时间事件，"
                  "表示在进入该状态 120 秒后自动触发迁移。", size=11)

    add_image(doc, os.path.join(DIAG_DIR, "B2_ControlPanel.png"),
              "图 4-2  SafeHome 控制面板 ControlPanel 状态图", width_cm=10.6)

    add_table(doc, ["状态（别名）", "含义", "entry / do 动作"],
              B2_STATES, widths=[3.6, 7.2, 5.2], size=9,
              caption="表 4-3  B2 状态集合", center_from=0)

    add_table(doc, ["编号", "源状态", "事件", "监护条件", "目标状态", "动作"],
              B2_TRANS, widths=[1.1, 2.8, 3.0, 3.4, 2.5, 3.2], size=9,
              caption="表 4-4  B2 状态迁移明细", center_from=0)

    add_para(doc, "建模要点：", bold=True, indent_first=False)
    for t in [
        "把「校验密码」单独建模为一个状态，而不是在「读取键入字符」内部用条件分支处理，"
        "原因是校验是一次独立的行为（比对、计错误次数、决定后续路径），"
        "用状态表达可以让「输入 → 校验 → 锁定 / 放行」的责任边界清晰。",
        "错误次数通过校验状态的两个互斥监护条件（累计错误 < 3 与 = 3）分流："
        "前者回到读取键入字符并清空缓冲，后者进入锁定。"
        "这样「锁定」只需要一个入口，不会出现多条路径重复计数的歧义。",
        "锁定期间不响应按键，在 UML 中即「当前状态没有该事件的迁移，事件被丢弃」，"
        "因此图中不必为锁定状态画按键自反箭头。",
    ]:
        add_para(doc, "• " + t, size=10.5)

    # ===== 4.3 B3 =====
    add_heading(doc, "4.3 B3 源文件字符串提取的状态图与实现", size=12.5)
    add_para(doc, "问题描述：从源代码文件中提取所有字符串。字符串是双引号之间的文本，"
                  "但字符串中的反斜杠会转义下一个字符。"
                  "例如对源码片段：", size=11)
    add_code(doc, [
        "\"一般学习一门编程语言的时候，基本上第一个程序是输出\\\"Hello World!\\\"\"",
    ])
    add_para(doc, "应提取出：", size=11)
    add_code(doc, [
        "一般学习一门编程语言的时候，基本上第一个程序是输出\"Hello World!\"",
    ])
    add_para(doc, "也就是说，\\\" 中的双引号属于字符串内容，不能作为字符串的结束标志；"
                  "反斜杠本身不进入结果，它只负责「消化」下一个字符。"
                  "这是一个典型的词法扫描问题，其本质是对输入字符流的顺序处理，"
                  "状态图恰好适合描述这种「每读一个字符做一次状态判断」的过程。", size=11)

    add_para(doc, "状态识别：只需三个状态即可覆盖全部情形——", size=11, indent_first=False)
    for t in [
        "扫描普通字符：在字符串之外，遇到双引号就开始一个字符串；",
        "字符串内：累积字符，遇到反斜杠进入转义，遇到双引号结束当前字符串；",
        "转义：把紧随反斜杠的字符原样收进结果，然后回到字符串内。"
        "该状态保证「被转义的双引号、反斜杠不会被误认为界定符」。",
    ]:
        add_para(doc, "• " + t, size=11)

    add_image(doc, os.path.join(DIAG_DIR, "B3_StringExtractor.png"),
              "图 4-3  源文件字符串提取状态图", width_cm=12.8)

    add_table(doc, ["状态（别名）", "含义", "动作"],
              B3_STATES, widths=[3.8, 7.2, 5.0], size=9,
              caption="表 4-5  B3 状态集合", center_from=0)

    add_table(doc, ["编号", "源状态", "事件", "监护条件", "目标状态", "动作"],
              B3_TRANS, widths=[1.1, 3.0, 2.8, 2.4, 2.6, 3.5], size=9,
              caption="表 4-6  B3 状态迁移明细", center_from=0)

    add_para(doc, "按状态图手工推演题目示例的处理过程（节选）：", bold=True,
             indent_first=False)
    add_table(doc, ["输入字符", "迁移前状态", "迁移后状态", "缓冲区内容"], TRACE_ROWS,
              widths=[3.4, 2.6, 2.6, 6.0], size=9,
              caption="表 4-7  题目示例的逐字符推演（节选）", center_from=0)
    add_para(doc, "图中还给出了两种结束方式：源文件正常结束（扫描普通字符 → 终态，输出全部结果）"
                  "与异常结束（字符串内或转义时遇到文件结束 → 丢弃残片并报告「字符串未闭合」）。"
                  "后者是实际工程中必须处理的边界情况，若忽略会导致最后一段残片被当作合法字符串输出。",
             size=11)

    # ===== 4.4 代码实现 =====
    add_heading(doc, "4.4 状态机模型的代码实现", size=12.5)
    add_para(doc, "三张状态图都是「状态 + 迁移表」的形式，因此实现上采用表驱动（table-driven）"
                  "的分层状态机引擎：把图中的每条箭头登记为一条迁移记录"
                  "（源状态、事件类型、监护条件、目标状态），运行时由引擎按 UML 语义派发事件。"
                  "这样做的直接好处是——状态图改了，只需要改迁移表，控制逻辑不用动；"
                  "而且图中每一条箭头都能在代码里找到对应的一行声明，模型与代码严格一一对应。"
                  "代码目录结构如下：", size=11)
    add_code(doc, [
        "code/",
        "├── build.gradle / settings.gradle     // Gradle 构建脚本（UTF-8，JUnit 5）",
        "└── src/",
        "    ├── main/java/com/hj212/experiment4/",
        "    │   ├── Main.java                   // 演示程序：依次运行三台状态机并打印轨迹",
        "    │   ├── fsm/State.java              // 状态接口（支持组合状态的双亲指针）",
        "    │   ├── fsm/Transition.java         // 一条迁移：源状态/事件类型/监护条件/目标状态",
        "    │   ├── fsm/StateMachine.java       // 表驱动状态机引擎（事件派发 + entry 动作）",
        "    │   ├── b1/TankState.java           // B1 状态：恒温烧水过程/待机/加热烧水/缺水保护…",
        "    │   ├── b1/TankEvent.java 等 5 个   // B1 事件：采样、时钟、水箱烧坏、维修完成",
        "    │   ├── b1/WaterTankController.java // B1 状态机的迁移表与继电器/采样控制",
        "    │   ├── b2/PanelState.java          // B2 状态：空闲/读取键入字符/校验密码/锁定/功能选择",
        "    │   ├── b2/PanelEvent.java 等 4 个  // B2 事件：按键、校验结果、计时、功能结束",
        "    │   ├── b2/ControlPanel.java        // B2 状态机与密码输入逻辑",
        "    │   ├── b3/ScanState.java           // B3 状态：扫描普通字符/字符串内/转义/结束",
        "    │   ├── b3/ScanEvent.java 等 2 个   // B3 事件：读到字符、源文件结束",
        "    │   └── b3/StringExtractor.java     // B3 状态机与提取动作",
        "    └── test/java/com/hj212/experiment4/",
        "        ├── b1/WaterTankControllerTest.java   // 14 个用例",
        "        ├── b2/ControlPanelTest.java          // 11 个用例",
        "        └── b3/StringExtractorTest.java       // 11 个用例",
    ])

    add_para(doc, "关键代码片段 1：状态机引擎的事件派发（含组合状态的子状态优先与进入动作）",
             bold=True, indent_first=False)
    add_code(doc, [
        "public S fire(E event) {",
        "    Transition<S, E> t = find(event);          // 由内向外查找第一条可用迁移",
        "    if (t == null) {",
        "        throw new IllegalStateException(\"状态 \" + current.path()",
        "                + \" 无法响应事件 \" + event);",
        "    }",
        "    List<State> before = chainOf(current);",
        "    S target = resolveTarget(t.to());          // 目标为组合状态时下沉到初始子状态",
        "    current = target;",
        "    runEntry(target, before, event);           // 按由外到内执行新进入状态的 entry 动作",
        "    trace.add(t.from().path() + \" --[\" + event + \"]--> \" + target.path());",
        "    return target;",
        "}",
        "",
        "private Transition<S, E> find(E event) {       // 子状态优先：当前状态 → 双亲状态 → …",
        "    for (State s = current; s != null; s = s.parent()) {",
        "        for (Transition<S, E> t : transitions) {",
        "            if (t.from() == s && t.matches(event) && t.guardHolds(event)) {",
        "                return t;",
        "            }",
        "        }",
        "    }",
        "    return null;",
        "}",
    ])

    add_para(doc, "关键代码片段 2：B1 迁移表的声明（与图 4-1、表 4-2 逐条对应）",
             bold=True, indent_first=False)
    add_code(doc, [
        "StateMachine.Builder<TankState, TankEvent> b =",
        "        StateMachine.<TankState, TankEvent>builder(TankState.WORKING)",
        "                .initial(TankState.WORKING, TankState.IDLE);  // 初始子状态",
        "",
        "// T1 待机 --[温度<20℃ 且 有水]--> 加热烧水",
        "b.transition(TankState.IDLE, SensorReading.class,",
        "        r -> r.getTemperature() < 20 && r.isWaterAvailable(), TankState.HEATING);",
        "// T2 待机 --[温度<20℃ 且 无水]--> 缺水保护",
        "b.transition(TankState.IDLE, SensorReading.class,",
        "        r -> r.getTemperature() < 20 && !r.isWaterAvailable(), TankState.NO_WATER);",
        "// T3 / T4 缺水保护 --[已注水]--> 加热烧水；--[温度≥20℃]--> 待机",
        "b.transition(TankState.NO_WATER, SensorReading.class,",
        "        r -> r.getTemperature() < 20 && r.isWaterAvailable(), TankState.HEATING);",
        "b.transition(TankState.NO_WATER, SensorReading.class,",
        "        r -> r.getTemperature() >= 20, TankState.IDLE);",
        "// T5 / T6 加热烧水 --[100℃]--> 待机；--[加热中无水]--> 缺水保护",
        "b.transition(TankState.HEATING, SensorReading.class,",
        "        r -> r.getTemperature() >= 100, TankState.IDLE);",
        "b.transition(TankState.HEATING, SensorReading.class,",
        "        r -> !r.isWaterAvailable(), TankState.NO_WATER);",
        "// T7 加热烧水 --[水箱烧坏]--> 故障维修（跨出组合状态）",
        "b.transition(TankState.HEATING, BoilerFault.class, TankState.REPAIR);",
        "// T8 故障维修 --[维修完成]--> 恒温烧水过程（进入初始子状态：待机）",
        "b.transition(TankState.REPAIR, RepairDone.class, TankState.WORKING);",
        "// T9 / T10 时钟事件发生在组合状态边界上",
        "b.transition(TankState.WORKING, ClockEvent.class, e -> e.at(23, 0), TankState.SLEEP);",
        "b.transition(TankState.SLEEP, ClockEvent.class, e -> e.at(7, 0), TankState.WORKING);",
        "",
        "// 状态的 entry 动作：加热烧水 entry 时闭合继电器，其余状态断开",
        "b.entry(TankState.HEATING, e -> relayOn = true);",
        "b.entry(TankState.IDLE,    e -> relayOn = false);",
    ])

    add_para(doc, "关键代码片段 3：B2 按键处理——每输入一个字符即与密码长度比较",
             bold=True, indent_first=False)
    add_code(doc, [
        "public boolean pressKey(char key) {",
        "    PanelState s = machine.current();",
        "    if (s == PanelState.LOCKED || s == PanelState.SELECTING) {",
        "        return false;                            // 锁定/功能选择期间按键被忽略",
        "    }",
        "    boolean completes = buffer.length() + 1 >= PanelState.PASSWORD_LENGTH;",
        "    machine.fire(new KeyPress(key));             // 空闲→读取；读取→读取或→校验密码",
        "    buffer.append(key);",
        "    if (completes) {",
        "        verify();                                // 达到 4 位立即校验",
        "    }",
        "    return true;",
        "}",
        "",
        "private void verify() {",
        "    boolean correct = password.equals(buffer.toString());",
        "    if (!correct) {",
        "        failCount++;                             // 错误次数累加",
        "    }",
        "    machine.fire(new VerifyResult(correct));     // 由监护条件分流到功能选择/重输/锁定",
        "}",
    ])

    add_para(doc, "关键代码片段 4：B3 字符处理——按「迁移前状态 → 迁移后状态」执行图中动作",
             bold=True, indent_first=False)
    add_code(doc, [
        "public void feed(char ch) {",
        "    ScanState before = machine.current();",
        "    machine.fire(new CharRead(ch));",
        "    ScanState after = machine.current();",
        "",
        "    if (before == ScanState.SCANNING && after == ScanState.IN_STRING) {",
        "        buffer.setLength(0);                     // 读到左引号：清空缓冲，开始新字符串",
        "    } else if (before == ScanState.IN_STRING) {",
        "        if (after == ScanState.SCANNING) {",
        "            strings.add(buffer.toString());      // 读到右引号：输出一个字符串",
        "        } else if (after == ScanState.IN_STRING) {",
        "            buffer.append(ch);                   // 普通字符：追加",
        "        }",
        "    } else if (before == ScanState.ESCAPE && after == ScanState.IN_STRING) {",
        "        buffer.append(ch);                       // 被转义的字符：只保留字符本身",
        "    }",
        "}",
    ])

    # ===== 4.5 测试 =====
    add_heading(doc, "4.5 测试与运行结果", size=12.5)
    add_para(doc, "状态图不仅要画对，还要能跑起来。为此为三台状态机编写了 36 个 JUnit 5 单元测试，"
                  "按「一条迁移至少一个用例」的原则覆盖图中的全部箭头，"
                  "并额外覆盖若干边界情况（休眠期间采样被忽略、锁定期间按键被忽略、"
                  "未闭合字符串、源文件结束后继续送字符等）。", size=11)

    add_table(doc, ["包", "测试类", "用例数", "覆盖要点"], TEST_ROWS,
              widths=[2.0, 4.4, 1.7, 7.9], size=8.5,
              caption="表 4-8  单元测试覆盖情况", center_from=0)

    add_para(doc, "执行 gradle test，全部用例通过：", size=11, indent_first=False)
    add_code(doc, [
        "$ gradle test",
        "> Task :compileJava",
        "> Task :compileTestJava",
        "> Task :test",
        "",
        "B1 恒温水箱状态图 > 温度<20℃且有水：打开继电器电源开始烧水 PASSED",
        "B1 恒温水箱状态图 > 温度<20℃但无水：进入缺水保护，不打开电源 PASSED",
        "B1 恒温水箱状态图 > 温度达到100℃：断开电源回到待机 PASSED",
        "B1 恒温水箱状态图 > 23:00 自动断开电源进入休眠，期间采样停止 PASSED",
        "B1 恒温水箱状态图 > 07:00 自动恢复恒温烧水过程 PASSED",
        "B2 SafeHome 控制面板状态图 > 输入满 4 位且密码正确：进入系统功能选择状态 PASSED",
        "B2 SafeHome 控制面板状态图 > 连续输入错误密码 3 次：锁定控制面板 PASSED",
        "B2 SafeHome 控制面板状态图 > 锁定期间敲击按键被忽略 PASSED",
        "B2 SafeHome 控制面板状态图 > 锁定满 120 秒后允许再次输入，错误计数清零 PASSED",
        "B3 源文件字符串提取状态图 > 题目示例：转义的双引号属于字符串内容 PASSED",
        "B3 源文件字符串提取状态图 > 源文件在字符串中途结束：丢弃残片并报未闭合错误 PASSED",
        "",
        "BUILD SUCCESSFUL",
        "36 tests completed, 0 failed",
    ])

    sp = doc.add_paragraph()
    sp.paragraph_format.space_after = Pt(2)
    add_para(doc, "运行时演示程序（gradle run）的输出片段，可直观看到状态机的迁移轨迹：",
             size=11, indent_first=False)
    add_code(doc, [
        "========== B1 恒温水箱（20℃~100℃） ==========",
        "启动后：恒温烧水过程/待机，继电器=false",
        "15℃ 无水：恒温烧水过程/缺水保护，继电器=false",
        "15℃ 有水：恒温烧水过程/加热烧水，继电器=true",
        "100℃：恒温烧水过程/待机，继电器=false",
        "23:00：休眠，继电器=false，采样=false",
        "07:00：恒温烧水过程/待机，继电器=false",
        "烧坏：故障维修，继电器=false",
        "维修完成：恒温烧水过程/待机",
        "",
        "========== B2 SafeHome 控制面板 ControlPanel ==========",
        "输入正确密码 2580：系统功能选择，功能指示灯=true",
        "第 1 次输错：读取键入字符，累计错误=1",
        "第 2 次输错：读取键入字符，累计错误=2",
        "第 3 次输错：锁定，累计错误=3",
        "锁定期间按键被接受？false",
        "锁定 120 秒：读取键入字符，累计错误=0",
        "",
        "========== B3 源文件字符串提取 ==========",
        "提取结果：[一般学习一门编程语言的时候，基本上第一个程序是输出\"Hello World!\"]",
    ])

    # ---------- 五、实验结论 ----------
    add_heading(doc, "五、实验结论")
    add_para(doc, "本次实验完成了 B1、B2、B3 三个状态图任务，并把它们实现为可运行、"
                  "可测试的状态机。主要收获如下：")
    for t in [
        "第一，真正理解了状态图的三要素以及三者的分工。状态是对对象生命周期片段的抽象；"
        "事件是引起状态变化的触发（按键、传感器读数、时钟、硬件故障）；"
        "监护条件是同一事件下的分岔判据，写在 [ ] 中；动作是迁移发生时要执行的计算，"
        "写在 / 之后，或作为状态的 entry / do / exit。画图时最容易犯的错是把"
        "「条件」和「事件」混为一谈——B1 中「温度低于 20℃」是监护条件而非事件，"
        "事件是「传感器上报了一次采样」，把二者分开写，图才不会被画成流程图。",

        "第二，理解了组合状态（composite state）的意义。它不是「把几个状态画在一起」的美术处理，"
        "而是消除重复迁移的手段：B1 中 23:00 的断电事件对三个工作子状态同样生效，"
        "画在组合状态边界上只需一条迁移；B2 中如果密码输入还有「修改密码」等更多子状态，"
        "同样可以封装。组合状态还带来了「进入组合状态时由初始伪状态决定入口」的语义，"
        "使「维修完成后重新开始恒温过程」可以直接指向组合状态而无需关心具体子状态。",

        "第三，体会到「状态图 vs 活动图」的区别。状态图描述的是一个对象的状态随事件演化，"
        "迁移由事件驱动，且同一时刻只处于一个状态；活动图描述的是业务流程的步骤与控制流，"
        "可以有并发与分叉。B3 的字符串提取看似是一个流程，但它的每一步都取决于"
        "「当前处于字符串内还是字符串外」，本质是状态记忆问题，用状态图（进而用状态机实现）"
        "比用一串 if-else 更清晰、更不易漏掉转义与未闭合等边界情况。",

        "第四，掌握了从自然语言需求到状态图的方法：先找出描述中所有「持续一段时间的情形」"
        "作为候选状态，再找出「引起变化的事情」作为事件，然后确定每个事件下是否还有分岔（监护条件），"
        "最后补上初始状态、终态与异常状态。B2 的需求只有四句话，却能完整地推出 5 个状态 8 条迁移，"
        "关键是把「输入错误 3 次锁定」「锁定 120 秒」「密码正确」三个判断分别落到"
        "校验状态的监护条件与锁定状态的时间事件上。",

        "第五，认识到模型与实现的一致性可以检验模型本身。把状态图翻译成迁移表后，"
        "每条箭头对应代码中一行 transition 声明，图与码的偏差会立即暴露；"
        "本次 36 个单元测试就是按「一条迁移一个用例」组织的，"
        "测试通过意味着图中的每条迁移都被实现且行为符合预期，"
        "这也反过来促使我修正了建模时的疏漏（例如最初漏掉了「加热过程中无水」这条迁移）。",
    ]:
        add_para(doc, t)

    add_para(doc, "不足与改进：", bold=True, indent_first=False)
    for t in [
        "状态机引擎目前只实现了 entry 动作，未实现 exit 动作与 do 活动（图中的 do / 部分"
        "由事件处理方法承担）。完整的 UML 状态机还应支持「内部迁移」「历史伪状态（H）」"
        "与「fork/join」，这些在更复杂的控制器建模中会用到，后续可以继续完善。",
        "B2 的锁定计时用「外部推进秒数」的方式模拟，真实系统中应使用定时器或事件循环；"
        "B1 的传感器采样也是手工调用，实际应接入定时采样线程。二者都属于「时间事件」的实现细节，"
        "不影响状态图的正确性，但工程化时需要补齐。",
        "B3 只处理了双引号字符串与反斜杠转义，尚未处理字符常量、注释（// 与 /* */）、"
        "原始字符串（如 Java 的 text block）等词法细节；若要做成通用的源码分析工具，"
        "还需要扩展状态与迁移，例如增加「行注释」「块注释」两个状态。",
    ]:
        add_para(doc, t)

    # ---------- 六、仓库地址 ----------
    add_heading(doc, "六、仓库地址")
    add_para(doc, "源代码仓库（Gitee 码云）：", bold=True, indent_first=False)
    add_para(doc, REPO_URL, indent_first=False)
    add_para(doc, "同一份代码的备份镜像（GitHub）：", bold=True, indent_first=False)
    add_para(doc, REPO_BACKUP_URL, indent_first=False)
    add_para(doc, "仓库内容：三张状态图的 PlantUML 源文件与导出的 PNG、"
                  "状态机引擎与三个子任务的 Java 实现、36 个 JUnit 5 单元测试、"
                  "状态图渲染脚本与实验报告生成脚本。", size=10.5)
    add_para(doc, "仓库目录结构：", bold=True, indent_first=False)
    add_code(doc, [
        "model/                                   # 状态图模型",
        "├── b1-water-tank.puml                   # B1 恒温水箱状态图（PlantUML 源文件）",
        "├── b2-control-panel.puml                # B2 ControlPanel 状态图",
        "├── b3-string-extractor.puml             # B3 字符串提取状态图",
        "└── diagrams/                            # 导出的 PNG（B1/B2/B3 各一张）",
        "",
        "code/                                    # Java 实现与单元测试",
        "├── build.gradle / settings.gradle       # Gradle 构建脚本",
        "└── src/main/java, src/test/java         # fsm 引擎 + b1/b2/b3 三台状态机",
        "",
        "tools/                                   # 辅助脚本",
        "└── plantuml.jar                          # 状态图渲染",
        "",
        "202410120323-柳淳-软件工程实验四.docx        # 本实验报告",
    ])

    doc.save(OUT_DOCX)
    print("saved:", OUT_DOCX)

    # 按课程要求，另存一份 .doc 扩展名的副本（内容与 .docx 完全一致）
    with open(OUT_DOCX, "rb") as src, open(OUT_DOC, "wb") as dst:
        dst.write(src.read())
    print("saved:", OUT_DOC)


if __name__ == "__main__":
    build_report()
