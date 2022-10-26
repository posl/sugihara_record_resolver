package rm4j.compiler.resolution;

public final class SyntheticTypeVariable implements Type{

    Type upperBound;
    Type lowerBound;

    @Override
    public Type actualType(TreeTracker tracker){
        throw new UnsupportedOperationException();
    }

    
    
}
