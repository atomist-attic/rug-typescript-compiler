function compile(file, sourceFileLoader) {

  var output = "";
  var opts = ts.getDefaultCompilerOptions();

  // enable commonjs modules
  opts.module = 1; // 1 = CommonJS
  opts.experimentalDecorators = true;

  var host = {
    getDefaultLibFileName: function() {
      return "typescript/lib/" + (opts.target === 2 ? "lib.core.es6.d.ts" : "lib.core.d.ts");
    },
    getCurrentDirectory: function() {
      return '';
    },
    useCaseSensitiveFileNames: function() {
      return true;
    },
    getCanonicalFileName: function(name) {
      return name;
    },
    getNewLine: function() {
      return _newline;
    },
    getSourceFile: function(filename, languageVersion, onError) {
      var body;
      try {
        var input = sourceFileLoader.sourceFor(filename, file);
        body = input.toString();
      } catch (e) {
        if (onError) {
          onError((e.getMessage && e.getMessage()) || "Unknown error");
        }
        body = "";
      }
      return ts.createSourceFile(filename, body, opts.target, '0');
    },
    writeFile: function(filename, data, writeByteOrderMark, onError) {
      output += data;
    },
    fileExists: function(filename) {
      try {
        sourceFileLoader.sourceFor(filename, file);
      } catch (e) {
        return false;
      }
      return true;
    }
  };

  var program = ts.createProgram([file], opts, host);

  function reportDiagnostic(diagnostic, errors) {
    if (diagnostic.file) {
        var loc = ts.getLineAndCharacterOfPosition(diagnostic.file, diagnostic.start);
        errors += diagnostic.file.fileName + "(" + (loc.line + 1) + "," + (loc.character + 1) + "): ";
    }
    var category = ts.DiagnosticCategory[diagnostic.category].toLowerCase();
    errors += category + " TS" + diagnostic.code + ": " + ts.flattenDiagnosticMessageText(diagnostic.messageText, host.getNewLine()) + host.getNewLine();
    return errors;
  }

  function reportDiagnostics(diagnostics, errors) {
      for (var i = 0; i < diagnostics.length; i++) {
          errors = reportDiagnostic(diagnostics[i], errors);
      }
      return errors;
  }


  // Collect all errors into errors
  var errors = "";
  var diagnostics = program.getSyntacticDiagnostics();
  errors = reportDiagnostics(diagnostics, errors);
  if (diagnostics.length === 0) {
      var diagnostics = program.getGlobalDiagnostics();
      errors = reportDiagnostics(diagnostics, errors);
      if (diagnostics.length === 0) {
          var diagnostics = program.getSemanticDiagnostics();
          errors = reportDiagnostics(diagnostics, errors);
      }
  }

  var emitOutput = program.emit();
  errors = reportDiagnostics(emitOutput.diagnostics, errors);

  if (diagnostics.length > 0 || emitOutput.diagnostics.length > 0) {
    throw "<#>" + errors + "<#>";
  }

  return output;
}
