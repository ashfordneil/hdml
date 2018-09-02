package codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import checker.Checker.*;
import parser.Program;
import parser.Program.*;

public class Codegen {
    public static void gen(CheckedProgram p, BufferedWriter writer) throws IOException {
        writer.write("{\n");

        int k = 0;
        for (CheckedDefinition d : p.definitions) {
            writeLineIndented("\"" + d.ident.name + "\": {", writer, 1);
            // inputs
            indent(writer, 2);
            writer.write("\"inputs\": [");
            for (int i = 0; i < d.patterns.size(); i++) {
                Pattern pattern = d.patterns.get(i);
                PatternIdentifier pIdent = (PatternIdentifier) pattern;
                writer.write("\"" + pIdent.ident.name + "\"");
                if (i != d.patterns.size() - 1) {
                    writer.write(", ");
                }
            }
            writer.write("],");
            writer.newLine();

            // nodes
            writeLineIndented("\"nodes\": {", writer, 2);
            int i = 0, size = d.symbols.keySet().size();
            for (String key : d.symbols.keySet()) {
                Symbol s = d.symbols.get(key);
                // Don't include ourselves in the output
                if (s.type == SymbolType.DEFINITION) {
                    i++;
                    continue;
                }
                writeLineIndented("\"" + s.ident.name + "\"" + ": {", writer, 3);
                if (s.type == SymbolType.INPUT) {
                    writeLineIndented("\"type_\": \"Input\",", writer, 4);
                } else if (s.type == SymbolType.OUTPUT) {
                    writeLineIndented("\"type_\": \"Output\",", writer, 4);
                } else if (s.type == SymbolType.CALL) {
                    SymbolCall call = (SymbolCall) s;
                    writeLineIndented("\"type_\": {", writer, 4);
                    writeLineIndented("\"Internal\": \"" + call.functionType + "\"", writer, 5);
                    writeLineIndented("},", writer, 4);
                } else if (s.type == SymbolType.VARIABLE) {
                    SymbolVariable var = (SymbolVariable) s;
                    writeLineIndented("\"type_\": {", writer, 4);
                    writeLineIndented("\"Internal\": \"" + var.value.type + "\"", writer, 5);
                    writeLineIndented("},", writer, 4);
                }
                writeLineIndented("\"name\": \"" + s.ident.name + "\"", writer, 4);
                if (i < size - 1) {
                    writeLineIndented("},", writer, 3);
                } else {
                    writeLineIndented("}", writer, 3);
                }
                i++;
            }
            writeLineIndented("},\n", writer, 2);

            // edges
            writeLineIndented("\"edges\": {", writer, 2);
            i = 0;
            for (String key : d.symbols.keySet()) {
                Symbol s = d.symbols.get(key);
                // Don't include ourselves in the output
                if (s.type == SymbolType.DEFINITION) {
                    i++;
                    continue;
                }
                writeLineIndented("\"" + s.ident.name + "\"" + ": [", writer, 3);
                int j = 0;
                while (j < s.references.size()) {
                    writeLineIndented("{", writer, 4);
                    writeLineIndented("\"source\": \"" + s.ident.name + "\",", writer, 5);
                    writeLineIndented("\"sink\": [\"" + s.references.get(j) + "\", \"" + s.referencesInputs.get(j) + "\"]", writer, 5);
                    if (j < s.references.size() - 1) {
                        writeLineIndented("},", writer, 4);
                    } else {
                        writeLineIndented("}", writer, 4);
                    }
                    j++;
                }
                if (i < size - 1) {
                    writeLineIndented("],", writer, 3);
                } else {
                    writeLineIndented("]", writer, 3);
                }
                i++;
            }
            writeLineIndented("}\n", writer, 2);

            if (k < p.definitions.size() - 1) {
                writeLineIndented("},\n", writer, 1);
            } else {
                writeLineIndented("}\n", writer, 1);
            }
            k++;
        }
        writer.write("}");
        writer.newLine();
        writer.flush();
    }

    private static void writeLineIndented(String content, BufferedWriter writer, int depth) throws IOException {
        indent(writer, depth);
        writer.write(content);
        writer.newLine();
    }

    private static void indent(BufferedWriter writer, int depth) throws IOException {
        String whitespace = "";
        for (int i = 0; i < depth * 4; i++) {
            whitespace += " ";
        }
        writer.write(whitespace);
    }
}