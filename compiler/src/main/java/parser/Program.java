package parser;

import java.util.*;

import parser.Parser.TokenKind;

public class Program {
    private List<Definition> definitions;

    public Program() {
        this.definitions = new ArrayList<>();
    }

    public Program(List<Definition> definitions) {
        this.definitions = definitions;
    }

    public List<Definition> getDefinitions() {
        return this.definitions;
    }

    public String toString() {
        return "";
    }

    public static class Definition {
        private Identifier ident;
        private List<Pattern> patterns;
        private Expression result;
    
        public Definition(Identifier ident, List<Pattern> patterns, Expression result) {
            this.ident = ident;
            this.patterns = patterns;
            this.result = result;
        }

        public Identifier getIdentifier() {
            return ident;
        }

        public List<Pattern> getPatterns() {
            return patterns;
        }

        public Expression getExpression() {
            return result;
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

    public static class Pattern {
        public TokenKind token;

        public String toString() {
            return token.name();
        }
    }

    public static class PatternUnderscore extends Pattern {
        public PatternUnderscore() {
            this.token = TokenKind.UNDERSCORE;
        }
    }
    
    public static class PatternIdentifier extends Pattern {
        public Identifier ident;
        public PatternIdentifier(Identifier ident) {
            this.token = TokenKind.IDENT;
            this.ident = ident;
        }
    }
    
    public static class PatternLiteral extends Pattern {
        public int literal;
        public PatternLiteral(int literal) {
            this.token = TokenKind.LITERAL;
            this.literal = literal;
        }
    
        public String toString() {
            return "PATTERN LITERAL " + literal;
        }
    }
    
    public static class Expression {
    }
    
    public static class ExpressionLiteral extends Expression {
        public int literal;
        public ExpressionLiteral(int literal) {
            this.literal = literal;
        }

        public String toString() {
            return "EXPRESSION LITERAL " + literal;
        }
    }
    
    public static class ExpressionLet extends Expression {
        public Definition definition;
        public Expression expression;
    
        public ExpressionLet(Definition definition, Expression expression) {
            this.definition = definition;
            this.expression = expression;
        }

        public String toString() {
            return Parser.KW_LET + " " +
                definition.toString() + 
                Parser.KW_IN + " " +
                expression.toString();
        }
    }
    
    public static class ExpressionFunction extends Expression {
        public Identifier ident;
        public List<Expression> params;
        public ExpressionFunction(Identifier ident, List<Expression> params) {
            this.ident = ident;
            this.params = params;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(ident);
            builder.append("( ");
            for (Expression param : params) {
                builder.append(param);
                builder.append(" ");
            }
            builder.append(")");
            return builder.toString();
        }
    }
    
    public static class ExpressionIdentifier extends Expression {
        public Identifier ident;
        public ExpressionIdentifier(Identifier ident) {
            this.ident = ident;
        }

        public String toString() {
            return ident.toString();
        }
    }
    
    public static class Identifier {
        public String name;
        public int id;

        public Identifier(String name) {
            this.name = name;
            this.id = new Random().nextInt();
        }

        public String toString() {
            return name;
        }
    }
}