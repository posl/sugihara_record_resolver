package rm4j.test;

import java.util.List;

import rm4j.compiler.tree.*;

public class ClassTotalizer{

    private static boolean isCanonicalConstructor(MethodTree m, List<VariableTree> finalFields){
        if(RecordComparator.isConstructor(m) && m.typeParameters().isEmpty() && m.exceptions().isEmpty()){
            if(m.parameters().size() != finalFields.size()){
                return false;
            }
            OUTER : for(var parameter : m.parameters()){
                String parameterType = parameter.actualType().toQualifiedTypeName();
                if(parameterType.endsWith("...")){
                    parameterType = parameterType.substring(0, parameterType.length()-3) + "[]";
                }
                for(int i = 0; i < finalFields.size(); i++){
                    if(finalFields.get(i).actualType().toQualifiedTypeName().equals(parameterType)){
                        finalFields.remove(i);
                        continue OUTER;
                    }
                }
                return false;
            }
            return finalFields.isEmpty();
        }
        return false;
    }

}
