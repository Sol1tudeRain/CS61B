package gitlet;

import java.io.*;
import java.nio.file.Files;

import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MyUtils {
    /**
     * A test main method
     *
     */
    public static void main(String[] args) throws IOException {
        File src=join(CWD,"source.txt");
        File des=join(CWD,"destination.txt");
        Files.copy(src.toPath(),des.toPath(),REPLACE_EXISTING);
        //copyFileUsingStream(src,des);
        //System.out.println(CWD);
    }

    /**
     * Return the gitlet state object
     */
    public static State getState(){
        return readObject(STATE_PATH, State.class);
    }

    /**
     * Return a commit object with the specified ID
     */
    public static Commit getCommit(String commitID){
        File commitPath=join(COMMITS_DIR,commitID);
        if(commitPath.exists()){
            return readObject(commitPath,Commit.class);
        }else {
            return null;
        }

    }

    /**
     * Delete all files in a directory
     */
    public static void clearDir(File dirPath){
        File filesList[] = dirPath.listFiles();
        for(File file:filesList){
            file.delete();
        }
    }

}
