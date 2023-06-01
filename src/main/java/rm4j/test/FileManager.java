package rm4j.test;

import java.io.File;

public class FileManager {

    public static void main(String[] args){
        File projects = new File("../data_original/repositories");
        for(File project : projects.listFiles()){
            if(project.isDirectory()){
                File repository = new File(project, "original").listFiles()[0];
            }
        }
    }
    
}
