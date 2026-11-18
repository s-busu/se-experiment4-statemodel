# -*- coding: utf-8 -*-
"""生成报告 PDF 预览并逐页导出 PNG（自检排版用）。

用法： <managed-python> tools/preview.py
依赖： pywin32（调用 Word 导出 PDF）、pymupdf（PDF 转图片）
"""
import os
import sys

import pymupdf
import win32com.client

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCX = os.path.join(ROOT, "202410120323-柳淳-软件工程实验四.docx")
PDF = os.path.join(ROOT, "tools", "preview.pdf")


def export_pdf():
    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0
    doc = word.Documents.Open(DOCX, False, True)
    doc.ExportAsFixedFormat(PDF, 17)  # 17 = wdExportFormatPDF
    doc.Close(0)
    word.Quit()
    print("pdf:", PDF)


def main():
    export_pdf()
    for f in os.listdir(os.path.join(ROOT, "tools")):
        if f.startswith("p") and f.endswith(".png") and len(f) == 6:
            os.remove(os.path.join(ROOT, "tools", f))
    d = pymupdf.open(PDF)
    print("pages:", d.page_count)
    for i, page in enumerate(d):
        pix = page.get_pixmap(dpi=100)
        pix.save(os.path.join(ROOT, "tools", "p%02d.png" % (i + 1)))
        text = page.get_text().strip().replace("\n", " ")
        print("p%02d %4d 字符 | %s" % (i + 1, len(text), text[:50]))


if __name__ == "__main__":
    sys.exit(main())
