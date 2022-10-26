package rm4j.util.functions;

import rm4j.compiler.core.CompileException;

@FunctionalInterface
public interface CEFunction<T, R>{
    public R apply(T t) throws CompileException;
}
