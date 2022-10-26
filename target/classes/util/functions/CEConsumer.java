package rm4j.util.functions;

import rm4j.compiler.core.CompileException;

@FunctionalInterface
public interface CEConsumer<T>{
    public void accept(T t) throws CompileException;
}