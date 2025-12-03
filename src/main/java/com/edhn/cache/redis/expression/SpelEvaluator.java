package com.edhn.cache.redis.expression;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * SpelEvaluator
 *
 * @author edhn
 * @version 1.0
 * @date 2022/5/17
 */
public class SpelEvaluator implements Function<Object, Object> {

    private static ExpressionParser parser;
    private static ParameterNameDiscoverer parameterNameDiscoverer;

    static {
        parser = new SpelExpressionParser();
        parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    private final Expression expression;
    private String[] parameterNames;

    public SpelEvaluator(String script, Method defineMethod) {
        expression = parser.parseExpression(script);
        if (defineMethod.getParameterCount() > 0) {
            parameterNames = parameterNameDiscoverer.getParameterNames(defineMethod);
        }
    }

    @Override
    public Object apply(Object rootObject) {
        EvaluationContext context = new StandardEvaluationContext(rootObject);
        MethodContext cic = (MethodContext) rootObject;
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], cic.getArgs()[i]);
            }
        }
        context.setVariable("result", cic.getResult());
        return expression.getValue(context);
    }

    @Data
    @AllArgsConstructor
    public static class MethodContext {
        private java.lang.reflect.Method method;
        private Object[] args;
        private Object result;
        private Object targetObject;

    }
}
