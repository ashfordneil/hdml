package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import parser.Program.*;

public class Parser {
    public static final String KW_UNDERSCORE   = "_";
    public static final String KW_EQUALS       = "=";
    public static final String KW_LET          = "let";
    public static final String KW_IN           = "in";
    public static final String KW_OPEN_BRACE   = "(";
    public static final String KW_CLOSE_BRACE  = ")";

    public static Program parse(List<Token> tokens) {
        List<Definition> definitions = new ArrayList<>();
        ListIterator<Token> iter = tokens.listIterator();
        while (iter.hasNext()) {
            definitions.add(parseDefinition(iter));
        }
        return new Program(definitions);
    }

    private static Token peek(ListIterator<Token> iter) {
        Token token = iter.next();
        iter.previous();
        return token;
    }

    private static Definition parseDefinition(ListIterator<Token> iter) {
        Token token = iter.next();
        if (token.getKind() != TokenKind.IDENT) {
            throw new RuntimeException("Expected Identifier");
        }
        Identifier ident = new Identifier(((TokenIdentifier) token).getName());
        List<Pattern> patterns = new ArrayList<>();
        while (peek(iter).getKind() != TokenKind.EQUALS) {
            patterns.add(parsePattern(iter));
        }
        if (iter.next().getKind() != TokenKind.EQUALS) {
            throw new RuntimeException("Expected EQUALS");
        }
        Expression e = parseExpression(iter);
        return new Definition(ident, patterns, e);
    }

    private static Pattern parsePattern(ListIterator<Token> iter) {
        Token token = iter.next();
        if (token.getKind() == TokenKind.UNDERSCORE) {
            return new PatternUnderscore();
        } else if (token.getKind() == TokenKind.IDENT) {
            return new PatternIdentifier(new Identifier(((TokenIdentifier) token).getName()));
        } else if (token.getKind() == TokenKind.LITERAL) {
            return new PatternLiteral(((TokenLiteral) token).getLiteral());
        } else {
            throw new RuntimeException("Expected Pattern");
        }
    }

    private static Expression parseExpression(ListIterator<Token> iter) {
        Token token = iter.next();
        if (token.getKind() == TokenKind.LITERAL) {
            return new ExpressionLiteral(((TokenLiteral) token).getLiteral());
        } else if (token.getKind() == TokenKind.LET) {
            return parseExpressionLet(iter);
        } else if (token.getKind() == TokenKind.IDENT) {
            Identifier ident = new Identifier(((TokenIdentifier) token).getName());
            Token test = peek(iter);
            if (test.getKind() == TokenKind.OPEN_BRACE) {
                iter.next(); // skip open_brace
                test = peek(iter);
                List<Expression> expressions = new ArrayList<Expression>();
                while (test.getKind() != TokenKind.CLOSE_BRACE) {
                    expressions.add(parseExpression(iter));
                    test = peek(iter);
                }
                iter.next(); // skip close_brace
                return new ExpressionFunction(ident, expressions);
            } else {
                // Hopefully just an ident
                return new ExpressionIdentifier(ident);
            }
        } 
        throw new RuntimeException("You dun fucked up");
    }

    private static Assignment parseAssignment(ListIterator<Token> iter) {
        Token token = iter.next();
        if (token.kind != TokenKind.IDENT) {
            throw new RuntimeException("Expected IDENTIFER, got " + token.kind.name());
        }
        Identifier i = new Identifier(((TokenIdentifier )token).name);
        token = iter.next();
        if (token.kind != TokenKind.EQUALS) {
            throw new RuntimeException("Expected EQUALS, got " + token.kind.name());
        }
        Expression e = parseExpression(iter);
        return new Assignment(i, e);
    }

    private static ExpressionLet parseExpressionLet(ListIterator<Token> iter) {
        Assignment assignment = parseAssignment(iter);
        Token token = iter.next();
        if (token.getKind() != TokenKind.IN) {
            throw new RuntimeException("Expected IN, got " + token);
        }
        Expression e = parseExpression(iter);
        return new ExpressionLet(assignment, e);
    }

    public static List<Token> tokenize(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<Token> tokens = new ArrayList<Token>();
        String line = reader.readLine();
        while (line != null) {
            Scanner scanner = new Scanner(line);
            while (scanner.hasNext()) {
                String token = scanner.next();
                if (token.equals(KW_UNDERSCORE)) {
                    tokens.add(new TokenKeyword(TokenKind.UNDERSCORE));
                } else if (token.equals(KW_EQUALS)) {
                    tokens.add(new TokenKeyword(TokenKind.EQUALS));
                } else if (token.equals(KW_LET)) {
                    tokens.add(new TokenKeyword(TokenKind.LET));
                } else if (token.equals(KW_IN)) {
                    tokens.add(new TokenKeyword(TokenKind.IN));
                } else if (token.equals(KW_OPEN_BRACE)) {
                    tokens.add(new TokenKeyword(TokenKind.OPEN_BRACE));
                } else if (token.equals(KW_CLOSE_BRACE)) {
                    tokens.add(new TokenKeyword(TokenKind.CLOSE_BRACE));
                } else if (token.matches("^\\d+$")) { // Is number
                    tokens.add(new TokenLiteral(Integer.parseInt(token)));
                } else { // Identifier
                    tokens.add(new TokenIdentifier(token));
                }
            }
            line = reader.readLine();
        }
        return tokens;
    }

    public static class Token {
        protected TokenKind kind;

        public TokenKind getKind() {
            return kind;
        }
    }

    public static class TokenKeyword extends Token {
        public TokenKeyword(TokenKind kind) {
            this.kind = kind;
        }

        public String toString() {
            return "TOKEN KEYWORD " + kind;
        }
    }

    public static class TokenIdentifier extends Token {
        private String name;

        public TokenIdentifier(String name) {
            this.kind = TokenKind.IDENT;
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return "TOKEN IDENTIFIER " + name;
        }
    }

    public static class TokenLiteral extends Token {
        private int literal;

        public TokenLiteral(int literal) {
            this.kind = TokenKind.LITERAL;
            this.literal = literal;
        }

        public int getLiteral() {
            return this.literal;
        }

        public String toString() {
            return "TOKEN LITERAL " + literal;
        }
    }

    public enum TokenKind {
        IDENT,
        LITERAL,
        UNDERSCORE,
        EQUALS,
        LET,
        IN,
        OPEN_BRACE,
        CLOSE_BRACE,
    }
}