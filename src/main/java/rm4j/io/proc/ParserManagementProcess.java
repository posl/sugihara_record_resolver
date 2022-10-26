package rm4j.io.proc;

import java.io.IOException;

import rm4j.compiler.core.CompileException;

@FunctionalInterface
public interface ParserManagementProcess{
    public void run() throws InterruptedException, IOException, CompileException;
}
