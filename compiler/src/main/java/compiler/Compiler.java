package compiler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

import checker.Checker;
import checker.Checker.CheckedProgram;
import codegen.Codegen;
import parser.Parser;
import parser.Program;
import parser.Parser.Token;

public class Compiler {
    public static void main(String[] args) {
        List<Token> tokens = new ArrayList<>();
        if (args.length != 1) {
            throw new RuntimeException("Please pass a filename as the only parameter");
        }
        String filename = args[0];
		try {
			tokens = Parser.tokenize(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
        Program program = Parser.parse(tokens);

        CheckedProgram checked = Checker.check(program);
        try {
            Codegen.gen(checked, new BufferedWriter(new OutputStreamWriter(System.out)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
}