package checker;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import parser.Program;
import parser.Program.*;
import parser.Parser.*;

public class Checker {

    private static Random r = new Random();
    
    private static NumberFormat f = new DecimalFormat("000000");
    
    public static CheckedProgram check(Program p) {
        HashMap<String, Symbol> symbols = new HashMap<>();
        symbols.put("nand", new Symbol(SymbolType.DEFINITION, new Identifier("nand")));
        List<CheckedDefinition> definitions = new ArrayList<>();
        for (Definition d : p.getDefinitions()) {
            // copy the symbol table down
            definitions.add(checkDefinition(d, symbols));
        }
        return new CheckedProgram(definitions, symbols);
    }

    public static CheckedDefinition checkDefinition(Definition d, HashMap<String, Symbol> symbols) {
        System.out.println("Definition " + d);

        Identifier i = d.getIdentifier();

        if (symbols.containsKey(i.name)) {
            throw new RuntimeException("Duplicate function name " + i.name);
        }
        List<String> params = new ArrayList<>();
        for (Pattern p : d.getPatterns()) {
            params.add(((PatternIdentifier) p).ident.name);
        }
        symbols.put(i.name, new SymbolFunction(i, params));
        String outputName = i.name + "_OUTPUT";
        symbols.put(outputName, new Symbol(SymbolType.OUTPUT, new Identifier(outputName)));
        HashMap<String, Symbol> s = new HashMap<>(symbols);
        for (Pattern p : d.getPatterns()) {
            checkPattern(p, s);
        }
        checkExpression(d.getExpression(), s, outputName);
        // symbols.get(d.getIdentifier().name).addReference(outputName);
        return new CheckedDefinition(i, d.getPatterns(), d.getExpression(), s);
    }

    public static void checkPattern(Pattern p, HashMap<String, Symbol> symbols) {
        System.out.println("Pattern " + p.toString());
        if (p.token == TokenKind.IDENT) {
            Symbol s = new Symbol(SymbolType.INPUT, ((PatternIdentifier) p).ident);
            if (symbols.containsKey(s.ident.name)) {
                throw new RuntimeException("Duplicate local identifier " + s.ident.name);
            }
            symbols.put(s.ident.name, s);
        }
    }

    public static void checkAssignment(Assignment assignment, HashMap<String, Symbol> symbols, String enclosing) {
        symbols.put(assignment.ident.name, new SymbolVariable(assignment.ident, assignment.expression));
        checkExpression(assignment.expression, symbols, assignment.ident.name);
    }

    public static void checkExpression(Expression expression, HashMap<String, Symbol> symbols, String enclosing) {
        System.out.println("Expression " + expression);
        if (expression instanceof ExpressionLet) {

            ExpressionLet e = (ExpressionLet) expression;
            // Allow check definition to modify this symbol table
            checkAssignment(e.assignment, symbols, enclosing);
            // Use that symbol table to validate the expression
            checkExpression(e.expression, symbols, enclosing);

        } else if (expression instanceof ExpressionIdentifier) {

            ExpressionIdentifier e = (ExpressionIdentifier) expression;
            if (symbols.containsKey(e.ident.name)) {
                symbols.get(e.ident.name).addReference(enclosing);
            } else {
                throw new RuntimeException("Symbol " + e.ident.name + " not found");
            }

        } else if (expression instanceof ExpressionFunction) {

            ExpressionFunction e = (ExpressionFunction) expression;
            String name = e.ident.name + f.format(r.nextInt(1000000));
            if (symbols.containsKey(e.ident.name)) {
                Symbol s = symbols.get(e.ident.name);
                if (s.type != SymbolType.DEFINITION) {
                    throw new RuntimeException("Can't call something that's not a function");
                }
                // Add symbol for function 
                symbols.put(name, new SymbolCall(new Identifier(name), e.ident.name));
                symbols.get(name).addReference(enclosing);
            } else {
                throw new RuntimeException("Symbol " + e.ident.name + " not found");
            }
            for (Expression child : e.params) {
                // Give these a new scope
                checkExpression(child, symbols, name);
            }
            
        }
    }

    public static class Symbol {
        public SymbolType type;
        public Identifier ident;
        public List<String> references;
        

        public Symbol(SymbolType type, Identifier ident) {
            this.type = type;
            this.ident = ident;
            this.references = new ArrayList<>();
        }

        public boolean equals(Object o) {
            if (!(o instanceof Symbol)) {
                return false;
            }
            Symbol other = (Symbol) o;
            return other.ident == this.ident && other.type == this.type;
        }

        public void addReference(String s) {
            this.references.add(s);
        }
    }

    public static class SymbolFunction extends Symbol {
        public List<String> params;
        public SymbolFunction(Identifier ident, List<String> params) {
            super(SymbolType.DEFINITION, ident);
            this.params = params;
        }
    }

    public static class SymbolCall extends Symbol {
        public String functionType;
        public SymbolCall(Identifier ident, String type) {
            super(SymbolType.CALL, ident);
            this.functionType = type;
        }
    }

    public static class SymbolVariable extends Symbol {
        public Expression value;
        public SymbolVariable(Identifier ident, Expression value) {
            super(SymbolType.VARIABLE, ident);
            this.value = value;
        }
    }

    public enum SymbolType {
        DEFINITION,
        INPUT,
        OUTPUT,
        VARIABLE,
        CALL,
    }

    public static class CheckedProgram {
        public List<CheckedDefinition> definitions;
        public HashMap<String, Symbol> symbols;

        public CheckedProgram(List<CheckedDefinition> definitions, HashMap<String, Symbol> symbols) {
            this.definitions = definitions;
            this.symbols = symbols;
        }
    }

    public static class CheckedDefinition {
        public Identifier ident;
        public List<Pattern> patterns;
        public Expression result;
        public HashMap<String, Symbol> symbols;
    
        public CheckedDefinition(Identifier ident, List<Pattern> patterns, Expression result, HashMap<String, Symbol> symbols) {
            this.ident = ident;
            this.patterns = patterns;
            this.result = result;
            this.symbols = symbols;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(ident);
            builder.append(": ");
            for (Pattern p : patterns) {
                builder.append(p);
                builder.append(" ");
            }
            builder.append("= ");
            builder.append(result);
            return builder.toString();
        }
    }
}