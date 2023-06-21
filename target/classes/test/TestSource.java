package rm4j.test;

import java.lang.annotation.Repeatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.Class;
import java.util.Arrays;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;

/**
 * This class is prepared fot testing.
 */

public class TestSource{

    public static void main(String[] args){
        int a = 0;
        long b = 05_567l;
        float c = 226.77e-12f;
        double d = 54.;

        //end in line comment
        var l1 = new ArrayList<>(Arrays.asList(a, b, c, d));

        /* 
        traditional comment
        */

        int e = 0b111_000_1;
        byte f = 3_3;
        float g = 0f;
        double h = .3d;
        @SuppressWarnings(value = "unused")
        float i = 0xa_5.67p-35f;
        @SuppressWarnings("unused")
        double j = 0x3_fp3D;

        /*
        //comments do not nest
        /*
        */

        var l2 = new ArrayList<>(Arrays.asList(e, f, g, h));

        double p = (a=6) + b - (d++-f---5)+(g += ++h)+(f <= 3? 7 : 66f);

        createCouples(l1);

        System.out.println(l2.toString());
        System.out.println(p);

        String s1 = "sad\077\7dd\n\b\r334";
        String s2 = """  
             A man can be destroyed,
             but not defeted.""";
        
        cha\u0072 u = '\\';
        @SuppressWarnings("unused")
        String fakeComment = "/* This must not be gotten as comment. */";
        System.out.println(s1 + s2 + u);
    }

    public static <E, F, T extends Tuple<E, F>> List<T> getCartesianProduct(List<E> l1, List<F> l2, BiFunction<E, F, T> constructor){
        List<T> returnList = new ArrayList<>();
        for(int i = 0; i < l1.size() - 1; i++){
            for(int j = i+1; j < l2.size(); j++){
                returnList.add(constructor.apply(l1.get(i), l2.get(j)));
            }
        }
        return returnList;
    }

    
    public static <E> List<Pair<E>> createCouples(List<E> list){
        return getCartesianProduct(list, list, Pair::new)
                .stream()
                .filter(pair -> pair.e1.equals(pair.e2))
                .toList();
    }

    public static sealed class Tuple<T, U> permits Pair<List<Number>>{
        final T e1;
        final U e2;

        public Tuple(T e1, U e2){
            this.e1 = e1;
            this.e2 = e2;
        }
    }

    private static non-sealed class Pair<T> extends Tuple<T, T>{

        public Pair(T e1, T e2){
            super(e1, e2);
        }

    }

    public @interface A{

    }

    /*//comment by @win
    if (getA_Asset_Class_ID() <= 0)
	{
		throw new FillMandatoryException(COLUMNNAME_A_Asset_Class_ID);
	}
			
	// Fix Asset Class
	MAssetClass assetClass = MAssetClass.get(getCtx(), getA_Asset_Class_ID());
	setA_Asset_Class_Value(assetClass.getValue());
	*/



@Anno("TYPE")
public class Source{
    @Anno("TYPE")
    class Inner{
        class InnerInner{
            public @Anno("CONSTRUCTOR") InnerInner(@Anno("TYPE_USE") Source. @Anno("TYPE_USE") Inner Inner.this,
                                                   @Anno("PARAMETER") java.lang. @Anno("TYPE_USE") Runnable p){
                Runnable r = () ->{
                    @Anno("TYPE_USE") Object tested = null;
                    @Anno("TYPE_USE") boolean isAnnotated = tested instanceof @Anno("TYPE_USE") String;
                };

                @Anno("TYPE_USE") Object tested = (@Anno("TYPE_USE") String @Anno("TYPE_USE") []) null;
                @Anno("TYPE_USE") boolean isAnnotated = tested instanceof@Anno("TYPE_USE") String;

                tested = new java.lang. @Anno("TYPE_USE") Object();
                tested = new @Anno("TYPE_USE") Object();
            }
        }
    }

   {
        Runnable r = () ->{
            @Anno("TYPE_USE") Object tested = null;
            @Anno("TYPE_USE") boolean isAnnotated = tested instanceof @Anno("TYPE_USE") String;
        };

        @Anno("TYPE_USE") Object tested = (@Anno("TYPE_USE") String @Anno("TYPE_USE") []) null;
        @Anno("TYPE_USE") boolean isAnnotated = tested instanceof@Anno("TYPE_USE") String;

        tested = new java.lang. @Anno("TYPE_USE") Object();
        tested = new @Anno("TYPE_USE") Object();
    }

    @Anno("TYPE")
    @interface A{ }
    abstract class Parameterized<@Anno("TYPE_PARAMETER") T extends @Anno("TYPE_USE") CharSequence &
                                                                   @Anno("TYPE_USE") Runnable>
        implements @Anno("TYPE_USE") List<@Anno("TYPE_USE") Runnable>{ }
}


@interface Anno{
   
    String value();
}

}


class TrailingComma{
    enum a{ , };
    enum b{ x , };
    enum c{ , ; };
    enum d{ x , ; };
}

class LocalInnerReceiverTest{
    void m(){
        class Inner{
            Inner(LocalInnerReceiverTest LocalInnerReceiverTest.this){}
        }
    }
}

/*
 * This class is partially replicated from test/tools/javac/annotations/typeAnnotations/classfile/CombinationsTargetTest1.java; CombinationsTargetTest2.java; CombinationsTargetTest3.java
 */
@RepTypeA @RepTypeA @RepTypeB @RepTypeB class RepeatingAtClassLevel{
}

@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB class RepeatingAtClassLevel2{
}

@RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB class RepeatingAtClassLevel3{
}

class RepeatingOnConstructor{

    @RepConstructorA @RepConstructorA @RepConstructorB @RepConstructorB
    RepeatingOnConstructor(){
    }

    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnConstructor(int i){
    }

    @RepConstructorA @RepConstructorA @RepConstructorB @RepConstructorB
    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnConstructor(int i, int j){
    }

    @RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB RepeatingOnConstructor(int i, int j, int k){
    }

    RepeatingOnConstructor(@RepParameterA @RepParameterA @RepParameterB @RepParameterB String parameter, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB ... vararg){
    }

    class Inner{
        Inner(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnConstructor RepeatingOnConstructor.this, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String parameter, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB ... vararg){
        }
    }
}

class RepeatingOnField{
    @RepFieldA @RepFieldA @RepFieldB @RepFieldB
    Integer i1;

    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Integer i2;

    @RepFieldA @RepFieldA @RepFieldB @RepFieldB
    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Integer i3;

    @RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB Integer i4;

    String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB [] @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB [] sa = null;
}

class RepeatingOnMethod{

    @RepMethodA @RepMethodA @RepMethodB @RepMethodB
    String test1(){
        return null;
    }

    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB String test2(){
        return null;
    }

    @RepMethodA @RepMethodA @RepMethodB @RepMethodB
    @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB String test3(){
        return null;
    }

    @RepAllContextsA @RepAllContextsA @RepAllContextsB @RepAllContextsB String test4(){
        return null;
    }

    String test5(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnMethod this, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String parameter, @RepParameterA @RepParameterA @RepParameterB @RepParameterB String @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB ... vararg){
        return null;
    }
}

class RepeatingOnTypeParametersBoundsTypeArgumentsOnClassDecl <@RepTypeParameterA @RepTypeParameterA @RepTypeParameterB @RepTypeParameterB T extends @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Object>{

    <T> String genericMethod(T t){
        return null;
    }
}

class RepeatingOnTypeParametersBoundsTypeArgumentsOnClassDecl2 <@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T extends @RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB Object>{

    <T> String genericMethod(T t){
        return null;
    }
}

class RepeatingOnTypeParametersBoundsTypeArgumentsOnMethod <T extends Object>{

    String test(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB RepeatingOnTypeParametersBoundsTypeArgumentsOnMethod<@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T> this){
        return null;
    }

    <@RepTypeParameterA @RepTypeParameterA @RepTypeParameterB @RepTypeParameterB T> String genericMethod(@RepParameterA @RepParameterA @RepParameterB @RepParameterB T t){
        return null;
    }

    <@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T> String genericMethod2(@RepTypeUseA @RepTypeUseA @RepTypeUseB @RepTypeUseB T t){
        return null;
    }
}

class RepeatingOnVoidMethodDeclaration{

    @RepMethodA @RepMethodA @RepMethodB @RepMethodB void test(){}
}

class RepeatingOnStaticMethodOfInterface{

    interface I{
        static @RepMethodA @RepMethodA @RepMethodB @RepMethodB String m(){
            return null;
        }
    }
}

//------------------------------------------------------------------------------
@Target({TYPE})
@Repeatable(ContTypeA.class)
@Documented
@interface RepTypeA{ }

@Target({TYPE})
@Documented
@interface ContTypeA{ RepTypeA[] value(); }

@Target({TYPE})
@Repeatable(ContTypeB.class)
@Documented
@interface RepTypeB{ }

@Target({TYPE})
@Documented
@interface ContTypeB{ RepTypeB[] value(); }

//------------------------------------------------------------------------------
@Target({CONSTRUCTOR})
@Repeatable(ContConstructorA.class)
@Documented
@interface RepConstructorA{ }

@Target({CONSTRUCTOR})
@Documented
@interface ContConstructorA{ RepConstructorA[] value(); }

@Target({CONSTRUCTOR})
@Repeatable(ContConstructorB.class )
@Documented
@interface RepConstructorB{ }

@Target({CONSTRUCTOR})
@Documented
@interface ContConstructorB{ RepConstructorB[] value(); }

//------------------------------------------------------------------------------
@Target({METHOD})
@Repeatable(ContMethodA.class)
@Documented
@interface RepMethodA{}

@Target({METHOD})
@Documented
@interface ContMethodA{
    RepMethodA[] value();
}

@Target({METHOD})
@Repeatable(ContMethodB.class)
@Documented
@interface RepMethodB{}

@Target({METHOD})
@Documented
@interface ContMethodB{
    RepMethodB[] value();
}

//------------------------------------------------------------------------------
@Target({FIELD})
@Repeatable(ContFieldA.class)
@Documented
@interface RepFieldA{}

@Target({FIELD})
@Documented
@interface ContFieldA{
    RepFieldA[] value();
}

@Target({FIELD})
@Repeatable(ContFieldB.class)
@Documented
@interface RepFieldB{}

@Target({FIELD})
@Documented
@interface ContFieldB{
    RepFieldB[] value();
}

//------------------------------------------------------------------------------
@Target({TYPE_USE})
@Repeatable(ContTypeUseA.class)
@Documented
@interface RepTypeUseA{}

@Target({TYPE_USE})
@Documented
@interface ContTypeUseA{
    RepTypeUseA[] value();
}

@Target({TYPE_USE})
@Repeatable(ContTypeUseB.class)
@Documented
@interface RepTypeUseB{}

@Target({TYPE_USE})
@Documented
@interface ContTypeUseB{
    RepTypeUseB[] value();
}

//------------------------------------------------------------------------------
@Target({TYPE_PARAMETER})
@Repeatable(ContTypeParameterA.class)
@Documented
@interface RepTypeParameterA{}

@Target({TYPE_PARAMETER})
@Documented
@interface ContTypeParameterA{
    RepTypeParameterA[] value();
}

@Target({TYPE_PARAMETER})
@Repeatable(ContTypeParameterB.class)
@Documented
@interface RepTypeParameterB{}

@Target({TYPE_PARAMETER})
@Documented
@interface ContTypeParameterB{
    RepTypeParameterB[] value();
}

//------------------------------------------------------------------------------
@Target({PARAMETER})
@Repeatable(ContParameterA.class)
@Documented
@interface RepParameterA{}

@Target({PARAMETER})
@Documented
@interface ContParameterA{
    RepParameterA[] value();
}

@Target({PARAMETER})
@Repeatable(ContParameterB.class)
@Documented
@interface RepParameterB{}

@Target({PARAMETER})
@Documented
@interface ContParameterB{
    RepParameterB[] value();
}


//------------------------------------------------------------------------------
@Target({PACKAGE})
@Repeatable(ContPackageA.class)
@Documented
@interface RepPackageA{}

@Target({PACKAGE})
@Documented
@interface ContPackageA{
    RepPackageA[] value();
}

@Target({PACKAGE})
@Repeatable(ContPackageB.class)
@Documented
@interface RepPackageB{}

@Target({PACKAGE})
@Documented
@interface ContPackageB{
    RepPackageB[] value();
}

//------------------------------------------------------------------------------
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Repeatable(ContAllContextsA.class)
@Documented
@interface RepAllContextsA{}

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Documented
@interface ContAllContextsA{
    RepAllContextsA[] value();
}

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Repeatable(ContAllContextsB.class)
@Documented
@interface RepAllContextsB{}

@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE})
@Documented
@interface ContAllContextsB{
    RepAllContextsB[] value();
}
