package rm4j.compiler.tokens;

public class WhiteSpace extends InputElement{

    public WhiteSpace(String text, Reference ref){
        super(text, ref);
    }

    public int terminatedLines(){
        int count = 0;
        boolean detectedCR = false;
        for(char c : text.toCharArray()){
            if(c == '\r'){
                count++;
                detectedCR = true;
            }else{
                if(c == '\n' && !detectedCR){
                    count++;
                }
                detectedCR = false;
            }
        }
        return count;
    }

}
