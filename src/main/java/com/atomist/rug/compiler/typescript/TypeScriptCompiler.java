package com.atomist.rug.compiler.typescript;

import static java.util.stream.Collectors.toList;
import static scala.collection.JavaConversions.asJavaCollection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomist.rug.compiler.Compiler;
import com.atomist.rug.compiler.CompilerListener;
import com.atomist.rug.compiler.CompilerListenerEnabled;
import com.atomist.rug.compiler.typescript.compilation.CompilerFactory;
import com.atomist.source.ArtifactSource;
import com.atomist.source.Deltas;
import com.atomist.source.FileArtifact;
import com.atomist.source.file.FileSystemArtifactSource;
import com.atomist.source.file.FileSystemArtifactSourceIdentifier$;

import scala.Option;

public class TypeScriptCompiler implements Compiler, CompilerListenerEnabled {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeScriptCompiler.class);
    private static String PATTERN_STRING = "(.*)\\(([0-9]*),([0-9]*)\\):";
    private static Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    private com.atomist.rug.compiler.typescript.compilation.Compiler compiler;

    private boolean externalLifeCycle = false;

    private List<CompilerListener> listeners = new ArrayList<>();

    public TypeScriptCompiler() {
    }

    public TypeScriptCompiler(com.atomist.rug.compiler.typescript.compilation.Compiler compiler) {
        this.externalLifeCycle = true;
        this.compiler = compiler;
    }

    @Override
    public ArtifactSource compile(ArtifactSource source) {
        try {
            ArtifactSourceScriptLoader scriptLoader = new ArtifactSourceScriptLoader(source);

            // Get source files to compile
            List<FileArtifact> files = filterSourceFiles(source);

            if (files.size() > 0) {
                // Init the compiler
                initCompiler();

                // Actually compile the files now
                compileFiles(source, scriptLoader, files);

                ArtifactSource result = scriptLoader.result();
                Deltas deltas = result.deltaFrom(source);
                if (LOGGER.isDebugEnabled()) {

                    asJavaCollection(deltas.deltas()).forEach(d -> {
                        LOGGER.debug("Successfully compiled TypeScript to file {}, content:\n{}",
                                d.path(), result.findFile(d.path()).get().content());
                    });

                }

                return result;
            }
            else {
                return source;
            }
        }
        finally {
            shutDownCompiler();
        }
    }

    @Override
    public Set<String> extensions() {
        return Collections.singleton("ts");
    }

    @Override
    public String name() {
        return "TypeScript Compiler";
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public boolean supports(ArtifactSource source) {
        return !filterSourceFiles(source).isEmpty();
    }

    @Override
    public void registerListener(CompilerListener listener) {
        listeners.add(listener);
    }

    private void compileFiles(ArtifactSource source, ArtifactSourceScriptLoader scriptLoader,
            List<FileArtifact> files) {
        List<String> errors = new ArrayList<>();
        files.stream().forEach(f -> {
            try {
                listeners.forEach(l -> l.compileStarted(f.path()));
                compiler.compile(f.path(), scriptLoader);
                Option<FileArtifact> file = scriptLoader.result()
                        .findFile(f.path().replaceAll(".ts", ".js"));
                if (file.isDefined()) {
                    listeners.forEach(l -> l.compileSucceeded(f.path(), file.get().content()));
                }
                else {
                    // Can this happen?
                    listeners.forEach(l -> l.compileSucceeded(f.path(), null));
                }
            }
            catch (Exception e) {
                listeners.forEach(l -> l.compileFailed(f.path()));
                errors.add(handleException(e, source));
            }
        });
        if (!errors.isEmpty()) {
            throw new TypeScriptDetailedCompilationException(
                    errors.stream().collect(Collectors.joining(System.lineSeparator())));
        }
    }

    private String handleException(Exception e, ArtifactSource source) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("<#>")) {
            int start = msg.indexOf("<#>") + 3;
            int end = msg.lastIndexOf("<#>");
            return formatString(msg.substring(start, end).trim(), source);
        }
        else {
            return msg;
        }
    }

    private synchronized void initCompiler() {
        if (compiler == null) {
            compiler = CompilerFactory.create();
        }
    }

    private synchronized void shutDownCompiler() {
        if (compiler != null && !externalLifeCycle) {
            compiler.shutdown();
            compiler = null;
        }
    }

    protected List<FileArtifact> filterSourceFiles(ArtifactSource source) {
        return asJavaCollection(source.allFiles()).stream()
                .filter(f -> f.path().startsWith(".atomist/") && f.name().endsWith(".ts"))
                .filter(f -> !f.path().startsWith(".atomist/node_modules/")).collect(toList());
    }

    private String formatString(String msg, ArtifactSource source) {
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

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            System.out.println("Usage: TypeScriptCompiler <input-path> <output-path>");
            System.exit(1);
        }
        File inputFile = new File(args[0]);
        if (!inputFile.canRead()) {
            throw new IllegalArgumentException("Cannot read input: " + args[0]);
        }

        File outputFile = new File(args[1]);
        outputFile.mkdirs();

        FileSystemArtifactSource input = new FileSystemArtifactSource(
                FileSystemArtifactSourceIdentifier$.MODULE$.apply(inputFile));

        ArtifactSource outputMem = new TypeScriptCompiler().compile(input);

        asJavaCollection(outputMem.allFiles()).forEach(f -> {
            PrintWriter pw = null;
            File output = new File(outputFile, f.path());
            try {
                output.getParentFile().mkdirs();
                pw = new PrintWriter(output);
                pw.write(f.content());
            }
            catch (FileNotFoundException e) {
                throw new RuntimeException(("Could not find file: " + output.getAbsolutePath()), e);
            }
            finally {
                IOUtils.closeQuietly(pw);
            }
        });

    }

}
