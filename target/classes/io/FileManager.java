package rm4j.io;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

public interface FileManager {
    
    default public void forEachFiles(File dir, Consumer<File> query){
        if(dir.isDirectory()){
            for(File child : dir.listFiles()){
                forEachFiles(child, query);
            }
        }else{
            query.accept(dir);
        }
    }

    default public boolean deleteAll(File dir){
        boolean isEmpty = true;
        if(dir.isDirectory()){
            for(File child : dir.listFiles()){
                isEmpty &= deleteAll(child);
            }
        }
        return isEmpty && dir.delete();
    }

    default public boolean resetDirectory(File dir){
        if(dir.exists()){
            deleteAll(dir);
        }
        return dir.mkdir();
    }

    default public boolean copyFiles(File original, File target){
        if(!original.exists()){
            return false;
        }
        if(original.isDirectory()){
            target.mkdir();
            for(File file : original.listFiles()){
                if(!copyFiles(file, new File(target, file.getName()))){
                    return false;
                }
            }
        }else{
            try{
                target.createNewFile();
                try(FileReader in = new FileReader(original);
                    FileWriter out = new FileWriter(target)){
                        in.transferTo(out);
                }
            }catch(IOException e){
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    default public void recordDataInCSV(String label, Collection<String> data, File csv) throws IOException{
        if(!csv.exists()){
            csv.createNewFile();
        }
        try(FileWriter writer = new FileWriter(csv)){
            writer.write(label + "\n");
            for(String line : data){
                writer.write(line + "\n");
            }
        }
    }

}
