package rm4j.test;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class Sample{

    private final int x;
    private final int y;

    Sample(int x, int y) throws RuntimeException{
        this.x = x;
        this.y = y;
    }

    int x(){
        return this.x;
    }

}
