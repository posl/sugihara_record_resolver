package rm4j.util.functions;

import rm4j.compiler.core.CompileException;

@FunctionalInterface
public interface CESupplier<R>{
    public R get() throws CompileException;
}
