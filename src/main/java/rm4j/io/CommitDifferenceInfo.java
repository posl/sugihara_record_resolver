package rm4j.io;

import java.util.HashSet;
import java.util.Set;

import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.Tree.DeclarationType;

public record CommitDifferenceInfo(CommitInfo info, TypeSet before, TypeSet after, int[] data){

    public CommitDifferenceInfo(CommitInfo info, TypeSet before, TypeSet after){
        this(info, before, after, new int[7]);
    }

    /*
     * record addition
     * 0 : records in new file
     * 1 : class to record refactoring
     * 2 : record newly added in an existing file
     * 
     * record deletion
     * 3 : records in deleted file
     * 4 : record to class refactoring
     * 5 : record removed from an existing file
     */

    public CommitDifferenceInfo{
        if(before.fileExists || after.fileExists){
            if(!before.fileExists){
                for(ClassTree c : after.classes().values()){
                    if(c.declType() == DeclarationType.RECORD){
                        data[0]++;
                    }
                }
            }else if(!after.fileExists){
                for(ClassTree c : before.classes().values()){
                    if(c.declType() == DeclarationType.RECORD){
                        data[3]++;
                    }
                }
            }else{
                for(String classPath : before.classes.keySet()){
                    ClassTree beforeClass = before.classes.get(classPath);
                    ClassTree afterClass = after.classes.get(classPath);
                    if(afterClass != null){
                        if(beforeClass.declType() != DeclarationType.RECORD && afterClass.declType() == DeclarationType.RECORD){
                            data[1]++;
                        }
                        if(beforeClass.declType() == DeclarationType.RECORD && afterClass.declType() != DeclarationType.RECORD){
                            data[4]++;
                        }
                    }
                }
                Set<String> newClasses = new HashSet<>(after.classes.keySet());
                newClasses.removeAll(before.classes.keySet());
                for(String classPath : newClasses){
                    if(after.classes.get(classPath).declType() == DeclarationType.RECORD){
                        data[2]++;
                    }
                }

                Set<String> oldClasses = new HashSet<>(before.classes.keySet());
                oldClasses.removeAll(after.classes.keySet());
                for(String classPath : oldClasses){
                    if(before.classes.get(classPath).declType() == DeclarationType.RECORD){
                        data[5]++;
                    }
                }
            }
        }
    }

}
