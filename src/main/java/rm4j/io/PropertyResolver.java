package rm4j.io;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import rm4j.compiler.tree.BlockTree;
import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.ExpressionNameTree;
import rm4j.compiler.tree.MemberSelectTree;
import rm4j.compiler.tree.MethodTree;
import rm4j.compiler.tree.ReturnTree;
import rm4j.compiler.tree.StatementTree;
import rm4j.compiler.tree.ThisTree;
import rm4j.compiler.tree.Tree;
import rm4j.compiler.tree.VariableTree;
import rm4j.compiler.tree.ModifiersTree.ModifierKeyword;
import rm4j.compiler.tree.Tree.DeclarationType;

public class PropertyResolver {
    
    public boolean isGetPrefixedMethod(MethodTree method){
        return method.name().name().startsWith("get");
    }

    public boolean isEffectivelyGetter(MethodTree method, ClassTree c){
        return testMethod(getInstanceFields(c).stream().map(f -> f.name().name()).toList()::contains, method);
    }

    public boolean isRecordFormatGetter(MethodTree method, ClassTree c){
        String name = null;
        for(VariableTree field : getInstanceFields(c)){
            if(field.name().name().equals(method.name().name())){
                name = method.name().name();
                break;
            }
        }
        if(name == null || !method.parameters().isEmpty()){
            return false;
        }
        return testMethod(name::equals, method);
    }

    public boolean isTraditionalFormatGetter(MethodTree method, ClassTree c){
        String name = null;
        for(VariableTree field : getInstanceFields(c)){
            String fieldName = field.name().name();
            if(fieldName != null && fieldName.length() > 0
                && method.name().name().equals("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1))){
                name = field.name().name();
                break;
            }
        }
        if(name == null || !method.parameters().isEmpty()){
            return false;
        }
        return testMethod(name::equals, method);
    }

    public boolean isEqualsMethod(MethodTree method){
        return method.name().name().equals("equals")
                && method.parameters() != null
                && method.parameters().size() == 1
                && (method.parameters().get(0).actualType().toQualifiedTypeName().equals("java.lang.Object")
                    || method.parameters().get(0).actualType().toQualifiedTypeName().equals("Object"));
    }

    public boolean isToStringMethod(MethodTree method){
        return method.name().name().equals("toString") && method.parameters() != null && method.parameters().isEmpty();
    }

    public boolean isHashCodeMethod(MethodTree method){
        return method.name().name().equals("hashCode") && method.parameters() != null && method.parameters().isEmpty();
    }

    public boolean isConstructor(MethodTree method){
        return method.actualType() == null;
    }

    public boolean isCompactConstructor(MethodTree method){
        return method.actualType() == null && method.parameters() == null;
    }

    public boolean isCanonicalConstructor(MethodTree method, ClassTree c){
        if(isConstructor(method) && method.typeParameters().isEmpty() && method.exceptions().isEmpty()){
            return isWeaklyCanonicalConstructor(method, c);
        }
        return false;
    }

    public boolean isWeaklyCanonicalConstructor(MethodTree method, ClassTree c){
        List<VariableTree> fields = new ArrayList<>(getInstanceFields(c));
        if(isConstructor(method)){
            if(method.parameters() == null || method.parameters().size() != fields.size()){
                return false;
            }
            OUTER : for(var parameter : method.parameters()){
                String parameterType = parameter.actualType().toQualifiedTypeName();
                if(parameterType.endsWith("...")){
                    parameterType = parameterType.substring(0, parameterType.length()-3) + "[]";
                }
                for(int i = 0; i < fields.size(); i++){
                    if(fields.get(i).actualType().toQualifiedTypeName().equals(parameterType)){
                        fields.remove(i);
                        continue OUTER;
                    }
                }
                return false;
            }
            return fields.isEmpty();
        }
        return false;
    }

    public boolean isNonImplicitMethod(MethodTree method, ClassTree c){
        return !isConstructor(method)
                && !isEffectivelyGetter(method, c)
                && !isEqualsMethod(method)
                && !isToStringMethod(method)
                && !isHashCodeMethod(method);
    }

    private boolean testMethod(Predicate<? super String> nameQuery, MethodTree method){
        if(method.body() != null && method.body().statements().size() == 1){
            StatementTree stmt = method.body().statements().get(0);
            if(stmt instanceof ReturnTree r){
                if(r.expression() instanceof MemberSelectTree ms){
                    if(ms.expression() instanceof ThisTree th
                        && th.qualifier() == ExpressionNameTree.EMPTY
                        && nameQuery.test(ms.identifier().name())){
                        return true;
                    }
                }else if(r.expression() instanceof ExpressionNameTree n){
                    if(n.qualifier() == ExpressionNameTree.EMPTY
                        && nameQuery.test(n.identifier().name())){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<VariableTree> getInstanceFields(ClassTree c){
        if(c.declType() == DeclarationType.RECORD){
            if(c.recordComponents() != null){
                return c.recordComponents().stream().toList();
            }else{
                return new ArrayList<>();
            }
        }else{
            return c.members().stream().<VariableTree>mapMulti((m, consumer) -> {
                    if(m instanceof VariableTree v && !v.modifiers().getModifiers().contains(ModifierKeyword.STATIC)){
                        consumer.accept(v);
                    }
                }
            ).toList();
        }
    }
    
    public int getClassData(ClassTree c){
        int ret = 0;
        if(getInstanceFields(c).stream().anyMatch(v -> !v.modifiers().getModifiers().contains(ModifierKeyword.FINAL))){
            ret |= 0b1;
        }
        if(c.modifiers().getModifiers().stream().anyMatch(modifier -> 
            modifier == ModifierKeyword.ABSTRACT
            || modifier == ModifierKeyword.SEALED
            || modifier == ModifierKeyword.NON_SEALED)){
            ret |= 0b10;
        }
        if(c.extendsClause() != null){
            ret |= 0b100;
        }
        for(Tree member : c.members()){
            if(member instanceof BlockTree b && !b.isStatic()){
                ret |= 0b1000;
            }
            if(member instanceof MethodTree method){
                if(isWeaklyCanonicalConstructor(method, c) && !isCanonicalConstructor(method, c)){
                    ret |= 0b10000;
                }
                if(isEffectivelyGetter(method, c) && !isRecordFormatGetter(method, c)){
                    ret |= 0b100000;
                }
                if(isEffectivelyGetter(method, c)){
                    ret |= 0b1000000;
                }
            }
        }
        return ret;
    }

}
