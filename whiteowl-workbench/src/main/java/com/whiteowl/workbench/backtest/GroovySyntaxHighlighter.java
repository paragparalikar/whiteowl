package com.whiteowl.workbench.backtest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GroovySyntaxHighlighter {

    private static final String KEYWORD_STYLE = "-fx-fill: #cc7832;";
    private static final String DSL_STYLE = "-fx-fill: #56a8f5;";
    private static final String STRING_STYLE = "-fx-fill: #6a8759;";
    private static final String NUMBER_STYLE = "-fx-fill: #6897bb;";
    private static final String COMMENT_STYLE = "-fx-fill: #5e6366; -fx-font-style: italic;";

    private static final String[] KEYWORDS = {
            "def", "var", "int", "float", "double", "long", "boolean", "char", "byte", "short",
            "void", "true", "false", "null", "new", "return", "if", "else", "for", "while",
            "do", "switch", "case", "break", "continue", "default", "class", "import", "package",
            "try", "catch", "finally", "throw", "throws", "this", "super", "in", "as", "instanceof"
    };

    private static final String[] DSL_FUNCTIONS = {
            "sma", "ema", "rsi", "atr", "macd",
            "crossover", "crossunder", "highest", "lowest",
            "longEntry", "longExit", "shortEntry", "shortExit",
            "open", "high", "low", "close", "volume", "timestamps", "barCount"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String DSL_PATTERN = "\\b(" + String.join("|", DSL_FUNCTIONS) + ")\\b";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?[fFdDlL]?\\b";
    private static final String LINE_COMMENT_PATTERN = "//[^\n]*";
    private static final String BLOCK_COMMENT_PATTERN = "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + LINE_COMMENT_PATTERN + "|" + BLOCK_COMMENT_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<DSL>" + DSL_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
    );

    public static StyleSpans<String> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;
        while (matcher.find()) {
            String style = resolveStyle(matcher);
            builder.add("", matcher.start() - lastEnd);
            builder.add(style, matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        builder.add("", text.length() - lastEnd);
        return builder.create();
    }

    private static String resolveStyle(Matcher matcher) {
        if (matcher.group("COMMENT") != null) return COMMENT_STYLE;
        if (matcher.group("STRING") != null) return STRING_STYLE;
        if (matcher.group("KEYWORD") != null) return KEYWORD_STYLE;
        if (matcher.group("DSL") != null) return DSL_STYLE;
        if (matcher.group("NUMBER") != null) return NUMBER_STYLE;
        return "";
    }

}
