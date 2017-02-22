package com.atomist.rug.compiler.typescript;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atomist.source.ArtifactSource;

@SuppressWarnings("serial")
public class TypeScriptDetailedCompilationException extends TypeScriptCompilationException {

    private static String PATTERN_STRING = "(.*)\\(([0-9]*),([0-9]*)\\):";
    private static Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    public TypeScriptDetailedCompilationException(String msg, ArtifactSource source) {
        super(formatString(msg, source));
    }

    private static String formatString(String msg, ArtifactSource source) {
        StringBuilder sb = new StringBuilder();
        String[] lines = msg.split(System.lineSeparator());

        for (String line : lines) {
            sb.append(line).append(System.lineSeparator());
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.find()) {
                String fileName = matcher.group(1);
                int lineCount = Integer.valueOf(matcher.group(2)) - 1;
                int colCount = Integer.valueOf(matcher.group(3));
                String content = source.findFile(fileName).get().content();

                String[] contentLines = content.split(System.lineSeparator());

                if (contentLines.length > lineCount) {
                    line = contentLines[lineCount];
                    sb.append(line).append(System.lineSeparator());
                    for (int i = 1; i < Integer.valueOf(colCount); i++) {
                        sb.append(" ");
                    }
                    sb.append("^").append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }
}
