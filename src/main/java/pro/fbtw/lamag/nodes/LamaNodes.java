package pro.fbtw.lamag.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import pro.fbtw.lamag.LamaException;
import pro.fbtw.lamag.LamaLanguage;
import pro.fbtw.lamag.runtime.LamaArray;
import pro.fbtw.lamag.runtime.LamaCallable;
import pro.fbtw.lamag.runtime.LamaFrame;
import pro.fbtw.lamag.runtime.LamaFunction;
import pro.fbtw.lamag.runtime.LamaPattern;
import pro.fbtw.lamag.runtime.LamaSexp;
import pro.fbtw.lamag.runtime.LamaValues;

import java.util.ArrayList;
import java.util.List;

public final class LamaNodes {
    private LamaNodes() {
    }

    public interface AssignableNode {
        void write(LamaFrame env, Object value);
    }

    static void writeTo(LamaExpressionNode target, LamaFrame env, Object value) {
        if (!(target instanceof AssignableNode)) {
            throw LamaException.error("assignment target is not assignable");
        }
        ((AssignableNode) target).write(env, value);
    }

    public static final class LiteralNode extends LamaExpressionNode {
        private final Object value;

        public LiteralNode(Object value) {
            this.value = value;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            return value;
        }
    }

    public static final class VariableNode extends LamaExpressionNode implements AssignableNode {
        private final String name;

        public VariableNode(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            return env.read(name);
        }

        @Override
        public void write(LamaFrame env, Object value) {
            env.write(name, value);
        }
    }

    public static final class SequenceNode extends LamaExpressionNode implements AssignableNode {
        @Children private final LamaExpressionNode[] expressions;

        public SequenceNode(LamaExpressionNode[] expressions) {
            this.expressions = expressions;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            Object result = 0L;
            for (LamaExpressionNode expression : expressions) {
                result = expression.executeGeneric(env);
            }
            return result;
        }

        @Override
        public void write(LamaFrame env, Object value) {
            for (int i = 0; i < expressions.length - 1; i++) {
                expressions[i].executeGeneric(env);
            }
            writeTo(expressions[expressions.length - 1], env, value);
        }
    }

    public static final class ScopeNode extends LamaExpressionNode implements AssignableNode {
        @Children private final LamaDeclarationNode[] declarations;
        @Child private LamaExpressionNode body;

        public ScopeNode(LamaDeclarationNode[] declarations, LamaExpressionNode body) {
            this.declarations = declarations;
            this.body = body;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            LamaFrame local = new LamaFrame(env);
            for (LamaDeclarationNode declaration : declarations) {
                declaration.executeDeclaration(local);
            }
            return body.executeGeneric(local);
        }

        @Override
        public void write(LamaFrame env, Object value) {
            LamaFrame local = new LamaFrame(env);
            for (LamaDeclarationNode declaration : declarations) {
                declaration.executeDeclaration(local);
            }
            writeTo(body, local, value);
        }
    }

    public static final class DeclarationSequenceNode extends LamaExpressionNode {
        @Children private final LamaDeclarationNode[] declarations;
        @Child private LamaExpressionNode body;

        public DeclarationSequenceNode(LamaDeclarationNode[] declarations, LamaExpressionNode body) {
            this.declarations = declarations;
            this.body = body;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            for (LamaDeclarationNode declaration : declarations) {
                declaration.executeDeclaration(env);
            }
            return body.executeGeneric(env);
        }
    }

    public static final class VarDeclarationNode extends LamaDeclarationNode {
        private final String[] names;
        @Children private final LamaExpressionNode[] initializers;

        public VarDeclarationNode(String[] names, LamaExpressionNode[] initializers) {
            this.names = names;
            this.initializers = initializers;
        }

        @Override
        public void executeDeclaration(LamaFrame env) {
            for (int i = 0; i < names.length; i++) {
                Object value = initializers[i] == null ? 0L : initializers[i].executeGeneric(env);
                env.define(names[i], value);
            }
        }
    }

    public static final class FunctionDeclarationNode extends LamaDeclarationNode {
        private final String name;
        private final int arity;
        private final CallTarget callTarget;

        public FunctionDeclarationNode(LamaLanguage language, String name, LamaPattern[] parameters, LamaExpressionNode body) {
            this.name = name;
            this.arity = parameters.length;
            this.callTarget = new LamaFunctionRootNode(language, name, parameters, body).getCallTarget();
        }

        @Override
        public void executeDeclaration(LamaFrame env) {
            env.define(name, new LamaFunction(name, arity, env, callTarget));
        }
    }

    public static final class ExternalFunctionDeclarationNode extends LamaDeclarationNode {
        private final String name;

        public ExternalFunctionDeclarationNode(String name) {
            this.name = name;
        }

        @Override
        public void executeDeclaration(LamaFrame env) {
            try {
                env.resolve(name);
            } catch (LamaException ex) {
                env.define(name, (LamaCallable) args -> {
                    throw LamaException.error("external function is not implemented: " + name);
                });
            }
        }
    }

    public static final class UnsupportedDeclarationNode extends LamaDeclarationNode {
        private final String feature;

        public UnsupportedDeclarationNode(String feature) {
            this.feature = feature;
        }

        @Override
        public void executeDeclaration(LamaFrame env) {
            throw LamaException.error(feature + " is parsed but not implemented in the Truffle runtime");
        }
    }

    public static final class AssignmentNode extends LamaExpressionNode {
        @Child private LamaExpressionNode target;
        @Child private LamaExpressionNode valueNode;

        public AssignmentNode(LamaExpressionNode target, LamaExpressionNode valueNode) {
            this.target = target;
            this.valueNode = valueNode;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            Object value = valueNode.executeGeneric(env);
            writeTo(target, env, value);
            return value;
        }
    }

    public static final class BinaryNode extends LamaExpressionNode {
        private final String operator;
        @Child private LamaExpressionNode left;
        @Child private LamaExpressionNode right;

        public BinaryNode(String operator, LamaExpressionNode left, LamaExpressionNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            Object l = left.executeGeneric(env);
            Object r = right.executeGeneric(env);
            switch (operator) {
                case ":":
                    return LamaValues.cons(l, r);
                case "!!":
                    return LamaValues.truth(l) != 0 || LamaValues.truth(r) != 0 ? 1L : 0L;
                case "&&":
                    return LamaValues.truth(l) != 0 && LamaValues.truth(r) != 0 ? 1L : 0L;
                case "=":
                case "==":
                    return LamaValues.equal(l, r) ? 1L : 0L;
                case "!=":
                    return LamaValues.equal(l, r) ? 0L : 1L;
                case "<":
                    return LamaValues.compare(l, r) < 0 ? 1L : 0L;
                case "<=":
                    return LamaValues.compare(l, r) <= 0 ? 1L : 0L;
                case ">":
                    return LamaValues.compare(l, r) > 0 ? 1L : 0L;
                case ">=":
                    return LamaValues.compare(l, r) >= 0 ? 1L : 0L;
                case "+":
                    return LamaValues.asLong(l) + LamaValues.asLong(r);
                case "-":
                    return LamaValues.asLong(l) - LamaValues.asLong(r);
                case "*":
                    return LamaValues.asLong(l) * LamaValues.asLong(r);
                case "/":
                    return LamaValues.asLong(l) / LamaValues.asLong(r);
                case "%":
                    return LamaValues.asLong(l) % LamaValues.asLong(r);
                default:
                    throw LamaException.error("unknown binary operator: " + operator);
            }
        }
    }

    public static final class UnaryMinusNode extends LamaExpressionNode {
        @Child private LamaExpressionNode valueNode;

        public UnaryMinusNode(LamaExpressionNode valueNode) {
            this.valueNode = valueNode;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            return -LamaValues.asLong(valueNode.executeGeneric(env));
        }
    }

    public static final class IfNode extends LamaExpressionNode implements AssignableNode {
        @Child private LamaExpressionNode condition;
        @Child private LamaExpressionNode thenNode;
        @Child private LamaExpressionNode elseNode;

        public IfNode(LamaExpressionNode condition, LamaExpressionNode thenNode, LamaExpressionNode elseNode) {
            this.condition = condition;
            this.thenNode = thenNode;
            this.elseNode = elseNode;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            if (LamaValues.truth(condition.executeGeneric(env)) != 0) {
                return thenNode.executeGeneric(env);
            }
            return elseNode.executeGeneric(env);
        }

        @Override
        public void write(LamaFrame env, Object value) {
            LamaExpressionNode branch = LamaValues.truth(condition.executeGeneric(env)) != 0 ? thenNode : elseNode;
            writeTo(branch, env, value);
        }
    }

    public static final class WhileNode extends LamaExpressionNode {
        @Child private LamaExpressionNode condition;
        @Child private LamaExpressionNode body;

        public WhileNode(LamaExpressionNode condition, LamaExpressionNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            while (LamaValues.truth(condition.executeGeneric(env)) != 0) {
                body.executeGeneric(env);
            }
            return 0L;
        }
    }

    public static final class DoWhileNode extends LamaExpressionNode {
        @Children private final LamaDeclarationNode[] declarations;
        @Child private LamaExpressionNode body;
        @Child private LamaExpressionNode condition;

        public DoWhileNode(LamaDeclarationNode[] declarations, LamaExpressionNode body, LamaExpressionNode condition) {
            this.declarations = declarations;
            this.body = body;
            this.condition = condition;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            while (true) {
                LamaFrame local = new LamaFrame(env);
                for (LamaDeclarationNode declaration : declarations) {
                    declaration.executeDeclaration(local);
                }
                body.executeGeneric(local);
                if (LamaValues.truth(condition.executeGeneric(local)) == 0) {
                    break;
                }
            }
            return 0L;
        }
    }

    public static final class ForNode extends LamaExpressionNode {
        @Child private LamaExpressionNode init;
        @Child private LamaExpressionNode condition;
        @Child private LamaExpressionNode step;
        @Child private LamaExpressionNode body;

        public ForNode(LamaExpressionNode init, LamaExpressionNode condition, LamaExpressionNode step, LamaExpressionNode body) {
            this.init = init;
            this.condition = condition;
            this.step = step;
            this.body = body;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            LamaFrame loopEnv = new LamaFrame(env);
            init.executeGeneric(loopEnv);
            while (LamaValues.truth(condition.executeGeneric(loopEnv)) != 0) {
                body.executeGeneric(loopEnv);
                step.executeGeneric(loopEnv);
            }
            return 0L;
        }
    }

    public static final class CaseNode extends LamaExpressionNode {
        @Child private LamaExpressionNode valueNode;
        private final LamaPattern[] patterns;
        @Children private final LamaExpressionNode[] bodies;

        public CaseNode(LamaExpressionNode valueNode, LamaPattern[] patterns, LamaExpressionNode[] bodies) {
            this.valueNode = valueNode;
            this.patterns = patterns;
            this.bodies = bodies;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            Object value = valueNode.executeGeneric(env);
            for (int i = 0; i < patterns.length; i++) {
                LamaFrame branchEnv = new LamaFrame(env);
                if (patterns[i].bind(value, branchEnv)) {
                    return bodies[i].executeGeneric(branchEnv);
                }
            }
            throw LamaException.error("pattern matching failed: " + LamaValues.print(value));
        }
    }

    public static final class ArrayLiteralNode extends LamaExpressionNode {
        @Children private final LamaExpressionNode[] elements;

        public ArrayLiteralNode(LamaExpressionNode[] elements) {
            this.elements = elements;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            List<Object> values = new ArrayList<>(elements.length);
            for (LamaExpressionNode element : elements) {
                values.add(element.executeGeneric(env));
            }
            return new LamaArray(values);
        }
    }

    public static final class ListLiteralNode extends LamaExpressionNode {
        @Children private final LamaExpressionNode[] elements;

        public ListLiteralNode(LamaExpressionNode[] elements) {
            this.elements = elements;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            List<Object> values = new ArrayList<>(elements.length);
            for (LamaExpressionNode element : elements) {
                values.add(element.executeGeneric(env));
            }
            return LamaValues.list(values);
        }
    }

    public static final class SexpLiteralNode extends LamaExpressionNode {
        private final String tag;
        @Children private final LamaExpressionNode[] fields;

        public SexpLiteralNode(String tag, LamaExpressionNode[] fields) {
            this.tag = tag;
            this.fields = fields;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                values[i] = fields[i].executeGeneric(env);
            }
            return new LamaSexp(tag, values);
        }
    }

    public static final class CallNode extends LamaExpressionNode {
        @Child private LamaExpressionNode callee;
        @Children private final LamaExpressionNode[] arguments;
        @Child private DirectCallNode directCallNode;
        @Child private IndirectCallNode indirectCallNode;
        @CompilationFinal private CallTarget cachedTarget;

        public CallNode(LamaExpressionNode callee, LamaExpressionNode[] arguments) {
            this.callee = callee;
            this.arguments = arguments;
        }

        @Override
        @ExplodeLoop
        public Object executeGeneric(LamaFrame env) {
            Object callable = callee.executeGeneric(env);
            Object[] values = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                values[i] = arguments[i].executeGeneric(env);
            }
            if (callable instanceof LamaFunction) {
                return dispatchFunction((LamaFunction) callable, values);
            }
            if (callable instanceof LamaCallable) {
                return ((LamaCallable) callable).call(values);
            }
            throw LamaException.error("callee did not evaluate to a function: " + LamaValues.print(callable));
        }

        private Object dispatchFunction(LamaFunction function, Object[] values) {
            if (function.arity() != values.length) {
                throw LamaException.error("function " + function.name() + " expects " + function.arity()
                        + " arguments, got " + values.length);
            }
            Object[] frameArguments = new Object[values.length + 1];
            frameArguments[0] = function;
            System.arraycopy(values, 0, frameArguments, 1, values.length);

            CallTarget target = function.callTarget();
            if (cachedTarget == target) {
                return directCallNode.call(frameArguments);
            }
            if (cachedTarget == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedTarget = target;
                directCallNode = insert(DirectCallNode.create(target));
                return directCallNode.call(frameArguments);
            }
            if (indirectCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                indirectCallNode = insert(IndirectCallNode.create());
            }
            return indirectCallNode.call(target, frameArguments);
        }
    }

    public static final class ElementNode extends LamaExpressionNode implements AssignableNode {
        @Child private LamaExpressionNode receiver;
        @Child private LamaExpressionNode index;

        public ElementNode(LamaExpressionNode receiver, LamaExpressionNode index) {
            this.receiver = receiver;
            this.index = index;
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            return LamaValues.element(receiver.executeGeneric(env), index.executeGeneric(env));
        }

        @Override
        public void write(LamaFrame env, Object value) {
            LamaValues.setElement(receiver.executeGeneric(env), index.executeGeneric(env), value);
        }
    }

    public static final class FunctionLiteralNode extends LamaExpressionNode {
        private final String name;
        private final int arity;
        private final CallTarget callTarget;

        public FunctionLiteralNode(LamaLanguage language, String name, LamaPattern[] parameters, LamaExpressionNode body) {
            this.name = name;
            this.arity = parameters.length;
            this.callTarget = new LamaFunctionRootNode(language, name, parameters, body).getCallTarget();
        }

        @Override
        public Object executeGeneric(LamaFrame env) {
            return new LamaFunction(name, arity, env, callTarget);
        }
    }
}
