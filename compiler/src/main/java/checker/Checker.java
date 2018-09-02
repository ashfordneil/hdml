package checker;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import parser.Program;
import parser.Program.*;
import parser.Parser.*;

public class Checker {

    private static int identifier = 0;
    
    private static NumberFormat f = new DecimalFormat("000000");
    
    public static CheckedProgram check(Program p) {
        HashMap<String, Symbol> symbols = new HashMap<>();
        List<String> nandParams = new ArrayList<>();
        nandParams.add("x");
        nandParams.add("y");
        symbols.put("nand", new SymbolFunction(new Identifier("nand"), nandParams));
        List<CheckedDefinition> definitions = new ArrayList<>();
        for (Definition d : p.getDefinitions()) {
            // copy the symbol table down
            definitions.add(checkDefinition(d, symbols));
        }
        return new CheckedProgram(definitions, symbols);
    }

    public static CheckedDefinition checkDefinition(Definition d, HashMap<String, Symbol> symbols) {
        // System.out.println("Definition " + d);

        Identifier i = d.getIdentifier();

        if (symbols.containsKey(i.name)) {
            throw new RuntimeException("Duplicate function name " + i.name);
        }
        List<String> params = new ArrayList<>();
        for (Pattern p : d.getPatterns()) {
            params.add(((PatternIdentifier) p).ident.name);
        }
        symbols.put(i.name, new SymbolFunction(i, params));

        HashMap<String, Symbol> s = new HashMap<>(symbols);

        // Add the output node, 1, and 0 to the enclosed symbol table
        String outputName = i.name + "_OUTPUT";
        s.put(outputName, new Symbol(SymbolType.OUTPUT, new Identifier(outputName)));
        s.put("1", new Symbol(SymbolType.INPUT, new Identifier("1")));
        s.put("0", new Symbol(SymbolType.INPUT, new Identifier("0")));
        for (Pattern p : d.getPatterns()) {
            checkPattern(p, s);
        }
        checkExpression(d.getExpression(), s, outputName, "");

        return new CheckedDefinition(i, d.getPatterns(), d.getExpression(), s);
    }

    public static void checkPattern(Pattern p, HashMap<String, Symbol> symbols) {
        // System.out.println("Pattern " + p.toString());
        if (p.token == TokenKind.IDENT) {
            Symbol s = new Symbol(SymbolType.INPUT, ((PatternIdentifier) p).ident);
            if (symbols.containsKey(s.ident.name)) {
                Symbol existing = symbols.get(s.ident.name);
                if (existing.type == SymbolType.UNDEFINED) {
                    if (!((SymbolUndefined) existing).expected.contains(SymbolType.INPUT)) {
                        throw new RuntimeException("Expected symbol to be INPUT");
                    }
                    for (int i = 0; i < existing.references.size(); i++) {
                        s.addReference(existing.references.get(i), existing.referencesInputs.get(i));
                    }
                } else {
                    throw new RuntimeException("Duplicate local identifier " + s.ident.name);
                }
            }
            symbols.put(s.ident.name, s);
        }
    }

    public static void checkAssignment(Assignment assignment, HashMap<String, Symbol> symbols, String sink, String sinkInput) {
        Symbol variable = new SymbolVariable(assignment.ident, assignment.expression);
        if (symbols.containsKey(assignment.ident.name)) {
            Symbol s = symbols.get(assignment.ident.name);
            if (s.type != SymbolType.UNDEFINED) {
                throw new RuntimeException("Duplicate local identifier " + assignment.ident.name);
            }
            SymbolUndefined undefined = (SymbolUndefined) s;
            if (!undefined.expected.contains(SymbolType.VARIABLE)) {
                throw new RuntimeException("Expected VARIABLE");
            }
            for (int i = 0; i < undefined.references.size(); i++) {
                variable.addReference(undefined.references.get(i), undefined.referencesInputs.get(i));
            }
        }
        symbols.put(assignment.ident.name, variable);
        String name = checkExpression(assignment.expression, symbols, assignment.ident.name, sinkInput);
        for (Symbol ss : symbols.values()) {
            int index = ss.references.indexOf(name);
            if (index != -1) {
                ss.references.set(index, assignment.ident.name);
            }
        }
        symbols.remove(name);
    }

    public static String checkExpression(Expression expression, HashMap<String, Symbol> symbols, String sink, String sinkInput) {
        // System.out.println("Expression " + expression);
        if (expression instanceof ExpressionLet) {

            ExpressionLet e = (ExpressionLet) expression;
            // Allow check definition to modify this symbol table
            checkAssignment(e.assignment, symbols, sink, sinkInput);
            // Use that symbol table to validate the expression
            return checkExpression(e.expression, symbols, sink, sinkInput);

        } else if (expression instanceof ExpressionIdentifier) {

            ExpressionIdentifier e = (ExpressionIdentifier) expression;
            if (!symbols.containsKey(e.ident.name)) {
                symbols.put(e.ident.name, new SymbolUndefined(new Identifier(e.ident.name), new SymbolType[]{SymbolType.INPUT, SymbolType.VARIABLE}));
            }
            symbols.get(e.ident.name).addReference(sink, sinkInput);
            return e.ident.name;

        } else if (expression instanceof ExpressionFunction) {

            ExpressionFunction e = (ExpressionFunction) expression;
            String name = e.ident.name + f.format(identifier++);
            Symbol s = symbols.get(e.ident.name);
            if (s != null) {
                if (s.type != SymbolType.DEFINITION && s.type != SymbolType.UNDEFINED) {
                    throw new RuntimeException("Can't call something that's not a function");
                }
            } else {
                // Be optimistic
                Symbol maybeFunction = new SymbolUndefined(new Identifier(e.ident.name), new SymbolType[]{SymbolType.DEFINITION});
                symbols.put(maybeFunction.ident.name, maybeFunction);
            }
            // Add symbol for function call
            symbols.put(name, new SymbolCall(new Identifier(name), e.ident.name));
            symbols.get(name).addReference(sink, sinkInput);

            SymbolFunction f = (SymbolFunction) s;
            if (e.params.size() > f.params.size()) {
                throw new RuntimeException("Too many parameters for function " + f.ident.name);
            }
            int i = 0;
            for (Expression child : e.params) {
                // Give these a new scope
                checkExpression(child, symbols, name, f.params.get(i));
                i++;
            }
            return name;
            
        } else if (expression instanceof ExpressionLiteral) {
            ExpressionLiteral e = (ExpressionLiteral) expression;
            if (e.literal != 0 && e.literal != 1) {
                throw new RuntimeException("2 isn't real");
            }
            symbols.get(e.literal == 1 ? "1" : "0").addReference(sink, sinkInput);
            return e.literal == 1 ? "1" : "0";
        }
        return null;
    }

    public static class Symbol {
        public SymbolType type;
        public Identifier ident;
        public List<String> references;
        public List<String> referencesInputs;
        

        public Symbol(SymbolType type, Identifier ident) {
            this.type = type;
            this.ident = ident;
            this.references = new ArrayList<>();
            this.referencesInputs = new ArrayList<>();
        }

        public String toString() {
            return type.name() +  " - " + ident.toString();
        }

        public boolean equals(Object o) {
            if (!(o instanceof Symbol)) {
                return false;
            }
            Symbol other = (Symbol) o;
            return other.ident == this.ident && other.type == this.type;
        }

        public void addReference(String sink, String sinkInput) {
            this.references.add(sink);
            this.referencesInputs.add(sinkInput);
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

    public static class SymbolUndefined extends Symbol {
        public List<SymbolType> expected;
        public SymbolUndefined(Identifier ident, SymbolType[] expected) {
            super(SymbolType.UNDEFINED, ident);
            this.expected = Arrays.asList(expected);
        }
    }

    public enum SymbolType {
        DEFINITION,
        INPUT,
        OUTPUT,
        VARIABLE,
        CALL,
        UNDEFINED,
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