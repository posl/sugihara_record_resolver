package rm4j.util.functions;

import rm4j.compiler.core.CompileException;

@FunctionalInterface
public interface CEPredicate<T>{
    public boolean test(T t) throws CompileException;
}