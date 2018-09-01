package checker;

import java.util.*;

import parser.Program;
import parser.Program.*;
import parser.Parser.*;

public class Checker {
    
    public static Program check(Program p) {
        List<Identifier> undefined = new ArrayList<>();
        HashMap<String, Symbol> symbols = new HashMap<>();
        symbols.put("nand", new Symbol(SymbolType.DEFINITION, new Identifier("nand")));
        for (Definition d : p.getDefinitions()) {
            // copy the symbol table down
            checkDefinition(d, symbols);
        }
        return p;
    }

    public static void checkDefinition(Definition d, HashMap<String, Symbol> symbols) {
        System.out.println("Definition " + d);

        Identifier i = d.getIdentifier();

        if (symbols.containsKey(i.name)) {
            throw new RuntimeException("Duplicate function name " + i.name);
        }
        symbols.put(i.name, new Symbol(SymbolType.DEFINITION, i));
        HashMap<String, Symbol> s = new HashMap<>(symbols);
        for (Pattern p : d.getPatterns()) {
            checkPattern(p, s);
        }
        checkExpression(d.getExpression(), s);
    }

    public static void checkPattern(Pattern p, HashMap<String, Symbol> symbols) {
        System.out.println("Pattern " + p.toString());
        if (p.token == TokenKind.IDENT) {
            Symbol s = new Symbol(SymbolType.VARIABLE, ((PatternIdentifier) p).ident);
            if (symbols.containsKey(s.ident.name)) {
                throw new RuntimeException("Duplicate local identifier " + s.ident.name);
            }
            symbols.put(s.ident.name, s);
        }
    }

    public static void checkExpression(Expression expression, HashMap<String, Symbol> symbols) {
        System.out.println("Expression " + expression);
        if (expression instanceof ExpressionLet) {
            ExpressionLet e = (ExpressionLet) expression;
            // Allow check definition to modify this symbol table
            checkDefinition(e.definition, symbols);
            // Use that symbol table to validate the expression
            checkExpression(e.expression, new HashMap<>(symbols));
        } else if (expression instanceof ExpressionIdentifier) {
            ExpressionIdentifier e = (ExpressionIdentifier) expression;
            if (!symbols.containsKey(e.ident.name)) {
                throw new RuntimeException("Symbol " + e.ident.name + " not found");
            }
        } else if (expression instanceof ExpressionFunction) {
            ExpressionFunction e = (ExpressionFunction) expression;
            if (!symbols.containsKey(e.ident.name)) {
                throw new RuntimeException("Symbol " + e.ident.name + " not found");
            }
            for (Expression child : e.params) {
                // Give these a new scope
                checkExpression(child, new HashMap<>(symbols));
            }
        }
    }

    public static class Symbol {
        SymbolType type;
        Identifier ident;

        public Symbol(SymbolType type, Identifier ident) {
            this.type = type;
            this.ident = ident;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Symbol)) {
                return false;
            }
            Symbol other = (Symbol) o;
            return other.ident == this.ident && other.type == this.type;
        }
    }

    public enum SymbolType {
        DEFINITION,
        VARIABLE,
    }
}