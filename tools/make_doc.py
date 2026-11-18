# -*- coding: utf-8 -*-
"""把报告 docx 另存为真正的 Word 97-2003 格式（.doc），并导出 PDF 供自检。

课程要求提交「学号-姓名-软件工程实验四.doc」，而 python-docx 只能写 docx，
因此用本机 Word 做一次格式转换，避免「扩展名与实际格式不一致」的提示。

用法： <managed-python> tools/make_doc.py
"""
import os

import win32com.client

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCX = os.path.join(ROOT, "202410120323-柳淳-软件工程实验四.docx")
DOC = os.path.join(ROOT, "202410120323-柳淳-软件工程实验四.doc")
PDF = os.path.join(ROOT, "tools", "preview_doc.pdf")

WD_FORMAT_DOCUMENT_97 = 0
WD_EXPORT_FORMAT_PDF = 17


def main():
    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0
    doc = word.Documents.Open(DOCX, False, True)
    doc.SaveAs(DOC, WD_FORMAT_DOCUMENT_97)
    doc.ExportAsFixedFormat(PDF, WD_EXPORT_FORMAT_PDF)
    doc.Close(0)
    word.Quit()
    print("doc:", DOC, os.path.getsize(DOC), "bytes")
    print("pdf:", PDF)


if __name__ == "__main__":
    main()
