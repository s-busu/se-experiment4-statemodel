package com.hj212.experiment4.b3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B3 字符串提取状态机的单元测试。 */
@DisplayName("B3 源文件字符串提取状态图")
class StringExtractorTest {

    @Test
    @DisplayName("题目示例：转义的双引号属于字符串内容")
    void extractsEscapedQuotes() {
        String source = "\"一般学习一门编程语言的时候，基本上第一个程序是输出\\\"Hello World!\\\"\"";
        List<String> result = StringExtractor.extract(source);
        assertEquals(1, result.size());
        assertEquals("一般学习一门编程语言的时候，基本上第一个程序是输出\"Hello World!\"", result.get(0));
    }

    @Test
    @DisplayName("转义的反斜杠被还原为一个反斜杠")
    void restoresEscapedBackslash() {
        List<String> result = StringExtractor.extract("\"a\\\\b\"");
        assertEquals(Arrays.asList("a\\b"), result);
    }

    @Test
    @DisplayName("一个源文件中的多个字符串全部提取")
    void extractsMultipleStrings() {
        List<String> result = StringExtractor.extract("String a = \"x\"; String b = \"y\";");
        assertEquals(Arrays.asList("x", "y"), result);
    }

    @Test
    @DisplayName("空字符串被正确提取")
    void extractsEmptyString() {
        assertEquals(Arrays.asList(""), StringExtractor.extract("String s = \"\";"));
    }

    @Test
    @DisplayName("字符串之外的字符（含单引号字符常量）不产生字符串")
    void ignoresNonStringText() {
        assertEquals(0, StringExtractor.extract("char c = 'x'; int n = 42;").size());
    }

    @Test
    @DisplayName("转义序列只保留被转义的字符，反斜杠被消化")
    void keepsOnlyEscapedCharacter() {
        assertEquals(Arrays.asList("anb"), StringExtractor.extract("\"a\\nb\""));
    }

    @Test
    @DisplayName("源文件在字符串中途结束：丢弃残片并报未闭合错误")
    void reportsUnterminatedString() {
        StringExtractor extractor = new StringExtractor();
        extractor.feed("\"abc");
        List<String> result = extractor.finish();
        assertTrue(extractor.isUnterminated());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("源文件结束时仍处于转义状态：同样视为未闭合")
    void reportsUnterminatedEscape() {
        StringExtractor extractor = new StringExtractor();
        extractor.feed("\"abc\\");
        assertTrue(extractor.finish().isEmpty());
        assertTrue(extractor.isUnterminated());
        assertEquals(ScanState.FINISHED, extractor.state());
    }

    @Test
    @DisplayName("状态随字符在 扫描/字符串内/转义 之间迁移")
    void walksThroughStates() {
        StringExtractor extractor = new StringExtractor();
        assertEquals(ScanState.SCANNING, extractor.state());

        extractor.feed('"');
        assertEquals(ScanState.IN_STRING, extractor.state());

        extractor.feed('\\');
        assertEquals(ScanState.ESCAPE, extractor.state());

        extractor.feed('n');
        assertEquals(ScanState.IN_STRING, extractor.state());

        extractor.feed('"');
        assertEquals(ScanState.SCANNING, extractor.state());
        assertEquals(Arrays.asList("n"), extractor.strings());
    }

    @Test
    @DisplayName("源文件正常结束时状态为终态且不报错")
    void finishesNormally() {
        StringExtractor extractor = new StringExtractor();
        extractor.feed("\"ok\"");
        assertEquals(Arrays.asList("ok"), extractor.finish());
        assertEquals(ScanState.FINISHED, extractor.state());
        assertFalse(extractor.isUnterminated());
    }

    @Test
    @DisplayName("结束后继续送入字符抛出异常")
    void rejectsInputAfterFinish() {
        StringExtractor extractor = new StringExtractor();
        extractor.finish();
        assertThrows(IllegalStateException.class, () -> extractor.feed('x'));
    }
}
