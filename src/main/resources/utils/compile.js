// Copyright 2015 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Compile a TypeScript file to JavaScript
 * @param file the name of the file to compile
 * @param sourceFactory a factory that loads source files
 * @returns {String} the generated JavaScript code
 */
function compileTypescript(file, sourceFactory) {
  var output = "";
  var opts = ts.getDefaultCompilerOptions();

  // enable commonjs modules
  opts.module = 1; // 1 = CommonJS
  opts.experimentalDecorators = true;

  // prepare a host object that we can pass to the TypeScript compiler
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
      return __lineSeparator;
    },

    getSourceFile: function(filename, languageVersion, onError) {
      // use TypeScriptClassLoader to load the given file
      var body;
      try {
        var input = sourceFactory.getSource(filename, file);
        body = input.toString();
      } catch (e) {
        if (__isFileNotFoundException(e)) {
          // the original version of this method just returns 'undefined'
          // if it could not find a file
          return undefined;
        }
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
      // use TypeScriptClassLoader and try to load the given file
      try {
        sourceFactory.getSource(filename, file);
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

  var errors = "";
  // report errors
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

  // generate code now
  var emitOutput = program.emit();
  errors = reportDiagnostics(emitOutput.diagnostics, errors);

  if (diagnostics.length > 0 || emitOutput.diagnostics.length > 0) {
    throw "Could not compile source file " + file + host.getNewLine() + errors;
  }

  return output;
}
