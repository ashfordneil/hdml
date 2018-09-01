package compiler;

import java.io.IOException;
import java.util.*;

import checker.Checker;
import parser.Parser;
import parser.Program;
import parser.Parser.Token;
import parser.Program.Definition;

public class Compiler {
    public static void main(String[] args) {
        List<Token> tokens = new ArrayList<>();
		try {
			tokens = Parser.tokenize("tests/xor");
		} catch (IOException e) {
			e.printStackTrace();
		}
        Program program = Parser.parse(tokens);
        for (Definition d : program.getDefinitions()) {
            System.out.println(d.toString());
        }
        System.out.println("Checking");
        Checker.check(program);
        return;
    }
}