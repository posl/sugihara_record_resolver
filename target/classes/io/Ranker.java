package rm4j.io;

import java.util.ArrayList;
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
        List<Component<T>> ranking = new ArrayList<>();
        for(var e : data.entrySet()){
            ranking.add(new Component<>(e.getKey(), e.getValue()));
        }
        ranking.sort((c1, c2) -> {
            if(c1.count() > c2.count()){
                return -1;
            }else if(c1.count() < c2.count()){
                return 1;
            }
            return 0;
        });
        return ranking;
    }

    record Component<T>(T t, int count){};
    
}