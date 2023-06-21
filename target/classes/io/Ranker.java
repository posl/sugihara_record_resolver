package rm4j.io;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ranker<T>{

    final Map<T, Integer> data;

    public Ranker(int initialCapacity){
        this.data = new HashMap<>(initialCapacity);
    }

    public void count(T t){
        Integer count;
        if((count = data.get(t)) == null){
            data.put(t, 1);
        }else{
            data.put(t, count+1);
        }
    }

    public List<Component<T>> getRanking(){
        return data.entrySet()
                .stream()
                .map(e -> new Component<>(e.getKey(), e.getValue()))
                .sorted((c1, c2) -> Integer.compare(c1.count(), c2.count())).toList();
    }

    record Component<T>(T t, int count) implements CSVTuple{

        @Override
        public String toCSVLine() {
            return t.toString() + "," + count;
        }
        
    };
    
}