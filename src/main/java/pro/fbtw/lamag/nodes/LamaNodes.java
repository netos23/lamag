package pro.fbtw.lamag.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
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
        void write(VirtualFrame frame, LamaFrame env, Object value);
    }

    public static final class LiteralNode extends LamaExpressionNode {
        private final Object value;

        public LiteralNode(Object value) {
            this.value = value;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            return env.read(name);
        }

        @Override
        public void write(VirtualFrame frame, LamaFrame env, Object value) {
            env.write(name, value);
        }
    }

    public static final class SequenceNode extends LamaExpressionNode {
        @Children private final LamaExpressionNode[] expressions;

        public SequenceNode(LamaExpressionNode[] expressions) {
            this.expressions = expressions;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            Object result = 0L;
            for (LamaExpressionNode expression : expressions) {
                result = expression.executeGeneric(frame, env);
            }
            return result;
        }
    }

    public static final class ScopeNode extends LamaExpressionNode {
        @Children private final LamaDeclarationNode[] declarations;
        @Child private LamaExpressionNode body;

        public ScopeNode(LamaDeclarationNode[] declarations, LamaExpressionNode body) {
            this.declarations = declarations;
            this.body = body;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            LamaFrame local = new LamaFrame(env);
            for (LamaDeclarationNode declaration : declarations) {
                declaration.executeDeclaration(local);
            }
            return body.executeGeneric(frame, local);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            for (LamaDeclarationNode declaration : declarations) {
                declaration.executeDeclaration(env);
            }
            return body.executeGeneric(frame, env);
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
                Object value = initializers[i] == null ? 0L : initializers[i].executeGeneric(null, env);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            if (!(target instanceof AssignableNode)) {
                throw LamaException.error("assignment target is not assignable");
            }
            Object value = valueNode.executeGeneric(frame, env);
            ((AssignableNode) target).write(frame, env, value);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            Object l = left.executeGeneric(frame, env);
            Object r = right.executeGeneric(frame, env);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            return -LamaValues.asLong(valueNode.executeGeneric(frame, env));
        }
    }

    public static final class IfNode extends LamaExpressionNode {
        @Child private LamaExpressionNode condition;
        @Child private LamaExpressionNode thenNode;
        @Child private LamaExpressionNode elseNode;

        public IfNode(LamaExpressionNode condition, LamaExpressionNode thenNode, LamaExpressionNode elseNode) {
            this.condition = condition;
            this.thenNode = thenNode;
            this.elseNode = elseNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            if (LamaValues.truth(condition.executeGeneric(frame, env)) != 0) {
                return thenNode.executeGeneric(frame, env);
            }
            return elseNode.executeGeneric(frame, env);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            while (LamaValues.truth(condition.executeGeneric(frame, env)) != 0) {
                body.executeGeneric(frame, env);
            }
            return 0L;
        }
    }

    public static final class DoWhileNode extends LamaExpressionNode {
        @Child private LamaExpressionNode body;
        @Child private LamaExpressionNode condition;

        public DoWhileNode(LamaExpressionNode body, LamaExpressionNode condition) {
            this.body = body;
            this.condition = condition;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            do {
                body.executeGeneric(frame, env);
            } while (LamaValues.truth(condition.executeGeneric(frame, env)) != 0);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            LamaFrame loopEnv = new LamaFrame(env);
            init.executeGeneric(frame, loopEnv);
            while (LamaValues.truth(condition.executeGeneric(frame, loopEnv)) != 0) {
                body.executeGeneric(frame, loopEnv);
                step.executeGeneric(frame, loopEnv);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            Object value = valueNode.executeGeneric(frame, env);
            for (int i = 0; i < patterns.length; i++) {
                LamaFrame branchEnv = new LamaFrame(env);
                if (patterns[i].bind(value, branchEnv)) {
                    return bodies[i].executeGeneric(frame, branchEnv);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            List<Object> values = new ArrayList<>(elements.length);
            for (LamaExpressionNode element : elements) {
                values.add(element.executeGeneric(frame, env));
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            List<Object> values = new ArrayList<>(elements.length);
            for (LamaExpressionNode element : elements) {
                values.add(element.executeGeneric(frame, env));
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            Object[] values = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                values[i] = fields[i].executeGeneric(frame, env);
            }
            return new LamaSexp(tag, values);
        }
    }

    public static final class CallNode extends LamaExpressionNode {
        @Child private LamaExpressionNode callee;
        @Children private final LamaExpressionNode[] arguments;

        public CallNode(LamaExpressionNode callee, LamaExpressionNode[] arguments) {
            this.callee = callee;
            this.arguments = arguments;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            Object callable = callee.executeGeneric(frame, env);
            if (!(callable instanceof LamaCallable)) {
                throw LamaException.error("callee did not evaluate to a function: " + LamaValues.print(callable));
            }
            Object[] values = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                values[i] = arguments[i].executeGeneric(frame, env);
            }
            return ((LamaCallable) callable).call(values);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            return LamaValues.element(receiver.executeGeneric(frame, env), index.executeGeneric(frame, env));
        }

        @Override
        public void write(VirtualFrame frame, LamaFrame env, Object value) {
            LamaValues.setElement(receiver.executeGeneric(frame, env), index.executeGeneric(frame, env), value);
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
        public Object executeGeneric(VirtualFrame frame, LamaFrame env) {
            return new LamaFunction(name, arity, env, callTarget);
        }
    }
}
