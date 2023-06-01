package rm4j.io;

import java.util.HashSet;
import java.util.Set;

import rm4j.compiler.tree.ClassTree;
import rm4j.compiler.tree.Tree.DeclarationType;

public record CommitDifferenceInfo(CommitInfo info, TypeSet before, TypeSet after, int[] data){

    public CommitDifferenceInfo(CommitInfo info, TypeSet before, TypeSet after){
        this(info, before, after, new int[9]);
    }

    /*
     * record addition
     * 0 : records in new file
     * 1 : class to record refactoring
     * 2 : record newly added in an existing file
     * 3 : others
     * 
     * record deletion
     * 4 : records in deleted file
     * 5 : record to class refactoring
     * 6 : record removed from an existing file
     * 7 : others
     * 
     * 8 : record changes
     * 
     */

    public CommitDifferenceInfo{
        int sameRecords = 0;
        if(before.fileExists || after.fileExists){
            if(!before.fileExists){
                data[0] = after.records.size();
            }else if(!after.fileExists){
                data[4] = before.records.size();
            }else{
                for(String classPath : before.classes.keySet()){
                    ClassTree beforeClass = before.classes.get(classPath);
                    ClassTree afterClass = after.classes.get(classPath);
                    if(afterClass != null){
                        if(beforeClass.declType() != DeclarationType.RECORD && afterClass.declType() == DeclarationType.RECORD){
                            data[1]++;
                        }
                        if(beforeClass.declType() == DeclarationType.RECORD && afterClass.declType() != DeclarationType.RECORD){
                            data[5]++;
                        }
                        if(beforeClass.declType() == DeclarationType.RECORD && afterClass.declType() == DeclarationType.RECORD){
                            sameRecords++;
                            if(!beforeClass.equals(afterClass)){
                                data[8]++;
                            }
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
                        data[6]++;
                    }
                }
                data[3] = after.records.size() - sameRecords - data[1] - data[2];
                data[7] = before.records.size() - sameRecords - data[5] - data[6];
            }
        }
    }

}
