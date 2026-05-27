package pro.fbtw.lamag.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import pro.fbtw.lamag.LamaLanguage;
import pro.fbtw.lamag.nodes.LamaDeclarationNode;
import pro.fbtw.lamag.nodes.LamaExpressionNode;
import pro.fbtw.lamag.nodes.LamaNodes;
import pro.fbtw.lamag.runtime.LamaPattern;
import pro.fbtw.lamag.runtime.LamaProgram;

import java.util.ArrayList;
import java.util.List;

final class LamaAstBuilder {
    private final LamaLanguage language;
    private int lambdaCounter;

    LamaAstBuilder(LamaLanguage language) {
        this.language = language;
    }

    LamaProgram build(LamaParser.ProgramContext context) {
        ScopeAst scope = scope(context.scopeBody());
        return new LamaProgram(scope.declarations, scope.body);
    }

    private ScopeAst scope(LamaParser.ScopeBodyContext context) {
        List<LamaDeclarationNode> declarations = new ArrayList<>();
        for (LamaParser.DeclarationContext declaration : context.declaration()) {
            declarations.add(declaration(declaration));
        }
        LamaExpressionNode body = context.sequence() == null ? literal(0L) : sequence(context.sequence());
        return new ScopeAst(declarations.toArray(new LamaDeclarationNode[0]), body);
    }

    private LamaExpressionNode scopeExpression(LamaParser.ScopeBodyContext context) {
        ScopeAst scope = scope(context);
        return new LamaNodes.ScopeNode(scope.declarations, scope.body);
    }

    private LamaExpressionNode declarationSequence(LamaParser.ScopeBodyContext context) {
        ScopeAst scope = scope(context);
        return new LamaNodes.DeclarationSequenceNode(scope.declarations, scope.body);
    }

    private LamaDeclarationNode declaration(LamaParser.DeclarationContext context) {
        if (context.varDecl() != null) {
            return varDeclaration(context.varDecl());
        }
        if (context.funDecl() != null) {
            return functionDeclaration(context.funDecl());
        }
        return new LamaNodes.UnsupportedDeclarationNode("user-defined infix operator");
    }

    private LamaDeclarationNode varDeclaration(LamaParser.VarDeclContext context) {
        List<String> names = new ArrayList<>();
        List<LamaExpressionNode> initializers = new ArrayList<>();
        for (LamaParser.VarInitContext init : context.varInit()) {
            names.add(init.LIDENT().getText());
            initializers.add(init.expression() == null ? null : expression(init.expression()));
        }
        return new LamaNodes.VarDeclarationNode(names.toArray(new String[0]), initializers.toArray(new LamaExpressionNode[0]));
    }

    private LamaDeclarationNode functionDeclaration(LamaParser.FunDeclContext context) {
        String name = context.LIDENT().getText();
        if (context.block() == null) {
            return new LamaNodes.ExternalFunctionDeclarationNode(name);
        }
        LamaPattern[] parameters = patterns(context.patternList());
        LamaExpressionNode body = scopeExpression(context.block().scopeBody());
        return new LamaNodes.FunctionDeclarationNode(language, name, parameters, body);
    }

    private LamaExpressionNode sequence(LamaParser.SequenceContext context) {
        List<LamaExpressionNode> expressions = new ArrayList<>();
        for (LamaParser.ExpressionContext expression : context.expression()) {
            expressions.add(expression(expression));
        }
        if (expressions.isEmpty()) {
            return literal(0L);
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        return new LamaNodes.SequenceNode(expressions.toArray(new LamaExpressionNode[0]));
    }

    private LamaExpressionNode expression(LamaParser.ExpressionContext context) {
        if (context.letExpr() != null) {
            return letExpression(context.letExpr());
        }
        return assignment(context.assignment());
    }

    private LamaExpressionNode letExpression(LamaParser.LetExprContext context) {
        return new LamaNodes.CaseNode(
                expression(context.expression(0)),
                new LamaPattern[]{pattern(context.pattern())},
                new LamaExpressionNode[]{expression(context.expression(1))});
    }

    private LamaExpressionNode assignment(LamaParser.AssignmentContext context) {
        LamaExpressionNode left = cons(context.cons());
        if (context.assignment() == null) {
            return left;
        }
        return new LamaNodes.AssignmentNode(left, assignment(context.assignment()));
    }

    private LamaExpressionNode cons(LamaParser.ConsContext context) {
        LamaExpressionNode left = logicOr(context.logicOr());
        if (context.cons() == null) {
            return left;
        }
        return new LamaNodes.BinaryNode(":", left, cons(context.cons()));
    }

    private LamaExpressionNode logicOr(LamaParser.LogicOrContext context) {
        LamaExpressionNode result = logicAnd(context.logicAnd(0));
        for (int i = 1; i < context.logicAnd().size(); i++) {
            result = new LamaNodes.BinaryNode("!!", result, logicAnd(context.logicAnd(i)));
        }
        return result;
    }

    private LamaExpressionNode logicAnd(LamaParser.LogicAndContext context) {
        LamaExpressionNode result = equality(context.equality(0));
        for (int i = 1; i < context.equality().size(); i++) {
            result = new LamaNodes.BinaryNode("&&", result, equality(context.equality(i)));
        }
        return result;
    }

    private LamaExpressionNode equality(LamaParser.EqualityContext context) {
        LamaExpressionNode result = comparison(context.comparison(0));
        for (int i = 1; i < context.comparison().size(); i++) {
            String operator = context.getChild(2 * i - 1).getText();
            result = new LamaNodes.BinaryNode(operator, result, comparison(context.comparison(i)));
        }
        return result;
    }

    private LamaExpressionNode comparison(LamaParser.ComparisonContext context) {
        LamaExpressionNode result = additive(context.additive(0));
        for (int i = 1; i < context.additive().size(); i++) {
            String operator = context.getChild(2 * i - 1).getText();
            result = new LamaNodes.BinaryNode(operator, result, additive(context.additive(i)));
        }
        return result;
    }

    private LamaExpressionNode additive(LamaParser.AdditiveContext context) {
        LamaExpressionNode result = multiplicative(context.multiplicative(0));
        for (int i = 1; i < context.multiplicative().size(); i++) {
            String operator = context.getChild(2 * i - 1).getText();
            result = new LamaNodes.BinaryNode(operator, result, multiplicative(context.multiplicative(i)));
        }
        return result;
    }

    private LamaExpressionNode multiplicative(LamaParser.MultiplicativeContext context) {
        LamaExpressionNode result = unary(context.unary(0));
        for (int i = 1; i < context.unary().size(); i++) {
            String operator = context.getChild(2 * i - 1).getText();
            result = new LamaNodes.BinaryNode(operator, result, unary(context.unary(i)));
        }
        return result;
    }

    private LamaExpressionNode unary(LamaParser.UnaryContext context) {
        if (context.MINUS() != null) {
            return new LamaNodes.UnaryMinusNode(unary(context.unary()));
        }
        return postfix(context.postfix());
    }

    private LamaExpressionNode postfix(LamaParser.PostfixContext context) {
        LamaExpressionNode result = primary(context.primary());
        for (LamaParser.PostfixPartContext part : context.postfixPart()) {
            if (part.DOT() != null) {
                String method = part.LIDENT().getText();
                List<LamaExpressionNode> args = new ArrayList<>();
                args.add(result);
                args.addAll(arguments(part.argList()));
                result = new LamaNodes.CallNode(new LamaNodes.VariableNode(method), args.toArray(new LamaExpressionNode[0]));
            } else if (part.LBRACK() != null) {
                result = new LamaNodes.ElementNode(result, expression(part.expression()));
            } else {
                result = new LamaNodes.CallNode(result, arguments(part.argList()).toArray(new LamaExpressionNode[0]));
            }
        }
        return result;
    }

    private LamaExpressionNode primary(LamaParser.PrimaryContext context) {
        if (context.DECIMAL() != null) {
            return literal(Long.parseLong(context.DECIMAL().getText()));
        }
        if (context.STRING() != null) {
            return literal(unquoteString(context.STRING().getText()));
        }
        if (context.CHAR() != null) {
            return literal((long) unquoteChar(context.CHAR().getText()));
        }
        if (context.TRUE() != null) {
            return literal(1L);
        }
        if (context.FALSE() != null) {
            return literal(0L);
        }
        if (context.LIDENT() != null) {
            return new LamaNodes.VariableNode(context.LIDENT().getText());
        }
        if (context.UIDENT() != null) {
            return new LamaNodes.SexpLiteralNode(context.UIDENT().getText(), arguments(context.argList()).toArray(new LamaExpressionNode[0]));
        }
        if (context.lambdaExpr() != null) {
            return lambda(context.lambdaExpr());
        }
        if (context.arrayLiteral() != null) {
            return new LamaNodes.ArrayLiteralNode(arguments(context.arrayLiteral().argList()).toArray(new LamaExpressionNode[0]));
        }
        if (context.listLiteral() != null) {
            return new LamaNodes.ListLiteralNode(arguments(context.listLiteral().argList()).toArray(new LamaExpressionNode[0]));
        }
        if (context.ifExpr() != null) {
            return ifExpression(context.ifExpr());
        }
        if (context.whileExpr() != null) {
            return new LamaNodes.WhileNode(expression(context.whileExpr().expression()), scopeExpression(context.whileExpr().scopeBody()));
        }
        if (context.forExpr() != null) {
            return new LamaNodes.ForNode(
                    declarationSequence(context.forExpr().init),
                    expression(context.forExpr().condition),
                    expression(context.forExpr().step),
                    scopeExpression(context.forExpr().body));
        }
        if (context.doWhileExpr() != null) {
            return new LamaNodes.DoWhileNode(scopeExpression(context.doWhileExpr().scopeBody()), expression(context.doWhileExpr().expression()));
        }
        if (context.caseExpr() != null) {
            return caseExpression(context.caseExpr());
        }
        if (context.SKIP_KW() != null) {
            return literal(0L);
        }
        return scopeExpression(context.scopeBody());
    }

    private LamaExpressionNode lambda(LamaParser.LambdaExprContext context) {
        String name = "<lambda" + lambdaCounter++ + ">";
        return new LamaNodes.FunctionLiteralNode(language, name, patterns(context.patternList()), scopeExpression(context.block().scopeBody()));
    }

    private LamaExpressionNode ifExpression(LamaParser.IfExprContext context) {
        LamaExpressionNode elseNode = context.elseBranch == null ? literal(0L) : scopeExpression(context.elseBranch);
        for (int i = context.elifBranch().size() - 1; i >= 0; i--) {
            LamaParser.ElifBranchContext branch = context.elifBranch(i);
            elseNode = new LamaNodes.IfNode(expression(branch.expression()), scopeExpression(branch.scopeBody()), elseNode);
        }
        return new LamaNodes.IfNode(expression(context.expression()), scopeExpression(context.thenBranch), elseNode);
    }

    private LamaExpressionNode caseExpression(LamaParser.CaseExprContext context) {
        LamaPattern[] patterns = new LamaPattern[context.caseBranch().size()];
        LamaExpressionNode[] bodies = new LamaExpressionNode[context.caseBranch().size()];
        for (int i = 0; i < context.caseBranch().size(); i++) {
            LamaParser.CaseBranchContext branch = context.caseBranch(i);
            patterns[i] = pattern(branch.pattern());
            bodies[i] = scopeExpression(branch.scopeBody());
        }
        return new LamaNodes.CaseNode(expression(context.expression()), patterns, bodies);
    }

    private List<LamaExpressionNode> arguments(LamaParser.ArgListContext context) {
        List<LamaExpressionNode> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        for (LamaParser.ExpressionContext expression : context.expression()) {
            result.add(expression(expression));
        }
        return result;
    }

    private LamaPattern[] patterns(LamaParser.PatternListContext context) {
        if (context == null) {
            return new LamaPattern[0];
        }
        List<LamaPattern> result = new ArrayList<>();
        for (LamaParser.PatternContext pattern : context.pattern()) {
            result.add(pattern(pattern));
        }
        return result.toArray(new LamaPattern[0]);
    }

    private LamaPattern pattern(LamaParser.PatternContext context) {
        LamaPattern left = patternPrimary(context.patternPrimary());
        if (context.pattern() == null) {
            return left;
        }
        return LamaPattern.sexp("cons", List.of(left, pattern(context.pattern())));
    }

    private LamaPattern patternPrimary(LamaParser.PatternPrimaryContext context) {
        if (context.UNDERSCORE() != null) {
            return LamaPattern.wildcard();
        }
        if (context.UIDENT() != null) {
            return LamaPattern.sexp(context.UIDENT().getText(), patternList(context.patternList()));
        }
        if (context.LBRACK() != null) {
            return LamaPattern.array(patternList(context.patternList()));
        }
        if (context.LBRACE() != null) {
            return LamaPattern.list(patternList(context.patternList()));
        }
        if (context.LIDENT() != null) {
            LamaPattern nested = context.pattern() == null ? LamaPattern.wildcard() : pattern(context.pattern());
            return LamaPattern.named(context.LIDENT().getText(), nested);
        }
        if (context.DECIMAL() != null) {
            long value = Long.parseLong(context.DECIMAL().getText());
            return LamaPattern.constant(context.MINUS() == null ? value : -value);
        }
        if (context.STRING() != null) {
            return LamaPattern.string(unquoteString(context.STRING().getText()));
        }
        if (context.CHAR() != null) {
            return LamaPattern.constant(unquoteChar(context.CHAR().getText()));
        }
        if (context.TRUE() != null) {
            return LamaPattern.constant(1L);
        }
        if (context.FALSE() != null) {
            return LamaPattern.constant(0L);
        }
        if (context.HASH() != null) {
            String tag = context.getChild(1).getText();
            switch (tag) {
                case "box":
                    return LamaPattern.boxed();
                case "val":
                    return LamaPattern.unboxed();
                case "str":
                    return LamaPattern.stringTag();
                case "sexp":
                    return LamaPattern.sexpTag();
                case "array":
                    return LamaPattern.arrayTag();
                case "fun":
                    return LamaPattern.closureTag();
                default:
                    throw new IllegalArgumentException(tag);
            }
        }
        return pattern(context.pattern());
    }

    private List<LamaPattern> patternList(LamaParser.PatternListContext context) {
        List<LamaPattern> result = new ArrayList<>();
        if (context == null) {
            return result;
        }
        for (LamaParser.PatternContext pattern : context.pattern()) {
            result.add(pattern(pattern));
        }
        return result;
    }

    private LamaExpressionNode literal(Object value) {
        return new LamaNodes.LiteralNode(value);
    }

    private static String unquoteString(String text) {
        return unescape(text.substring(1, text.length() - 1));
    }

    private static char unquoteChar(String text) {
        String unescaped = unescape(text.substring(1, text.length() - 1));
        return unescaped.isEmpty() ? 0 : unescaped.charAt(0);
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 == text.length()) {
                result.append(c);
                continue;
            }
            char escaped = text.charAt(++i);
            switch (escaped) {
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'b':
                    result.append('\b');
                    break;
                case 'f':
                    result.append('\f');
                    break;
                case 'u':
                    String hex = text.substring(i + 1, i + 5);
                    result.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                    break;
                default:
                    result.append(escaped);
                    break;
            }
        }
        return result.toString();
    }

    private static final class ScopeAst {
        private final LamaDeclarationNode[] declarations;
        private final LamaExpressionNode body;

        private ScopeAst(LamaDeclarationNode[] declarations, LamaExpressionNode body) {
            this.declarations = declarations;
            this.body = body;
        }
    }
}
