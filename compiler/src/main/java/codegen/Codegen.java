package codegen;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import checker.Checker.*;
import parser.Program;
import parser.Program.*;

public class Codegen {
    public static void gen(CheckedProgram p, String out) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.write("{\n");
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
            for (String key : d.symbols.keySet()) {
                Symbol s = d.symbols.get(key);
                writeLineIndented("\"" + s.ident.name + "\"" + ": {", writer, 3);
                if (s.type == SymbolType.INPUT) {
                    writeLineIndented("\"type_\": \"input\",", writer, 4);
                } else if (s.type == SymbolType.OUTPUT) {
                    writeLineIndented("\"type_\": \"output\",", writer, 4);
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
                writeLineIndented("},", writer, 3);
            }
            writeLineIndented("},\n", writer, 2);

            // edges
            writeLineIndented("\"edges\": {", writer, 2);
            for (String key : d.symbols.keySet()) {
                Symbol s = d.symbols.get(key);
                writeLineIndented("\"" + s.ident.name + "\"" + ": [", writer, 3);
                for (String r : s.references) {
                    writeLineIndented("{", writer, 4);
                    writeLineIndented("\"source\": \"" + s.ident.name + "\",", writer, 5);
                    writeLineIndented("\"sink\": [\"" + r + "\"],", writer, 5);
                    writeLineIndented("},", writer, 4);
                }
                writeLineIndented("],", writer, 3);
            }
            writeLineIndented("}\n", writer, 2);

            writeLineIndented("}\n", writer, 1);
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