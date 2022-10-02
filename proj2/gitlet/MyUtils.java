package gitlet;

import org.antlr.v4.runtime.misc.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MyUtils {
    /** A test main method */
    public static void main(String[] args) throws IOException {
        File src=join(CWD,"source.txt");
        File des=join(CWD,"destination.txt");
        Files.copy(src.toPath(),des.toPath(),REPLACE_EXISTING);
        //copyFileUsingStream(src,des);
        //System.out.println(CWD);
    }

    public static void safeDelete(File file){
        if(file.exists()){
            file.delete();
        }
    }
    /** Return the gitlet state object */
    public static State getState(){
        if(STATE_PATH.exists()){
            return readObject(STATE_PATH, State.class);
        }
        System.out.println("Not in an initialized Gitlet directory.");
        return null;
    }

    /** Return a commit object with the specified ID */
    public static Commit getCommit(String commitID){
        String ID;
        if(commitID.length()==8){
            State gitletState=getState();
            ID=gitletState.shortIDs.get(commitID);
            if(ID==null){
                return null;
            }
        }else {
            ID=commitID;
        }

        File commitPath=join(COMMITS_DIR,ID);
        if(commitPath.exists()&&commitPath.isFile()){
            return readObject(commitPath,Commit.class);
        }else {
            return null;
        }

    }

    /** Delete all files in a directory */
    public static void clearDir(File dirPath){
        File filesList[] = dirPath.listFiles();
        for(File file:filesList){
            file.delete();
        }
    }

    /** Mark all commits of a branch. */
    public static void markCommits(String commitID,HashSet<String> markedCommits){
        Commit commit=getCommit(commitID);
        if(commit!=null){
            if(markedCommits.add(commit.UID)){
                for(String parent:commit.parents){
                    markCommits(parent,markedCommits);
                }
            }
        }
    }

    public static Commit getSplitPointHelper(String commitID,HashSet<String> markedCommits){
        Commit commit=getCommit(commitID);
        if(commit!=null){
            if(markedCommits.contains(commit.UID)){
                return commit;
            }

            for(String parent:commit.parents){
                commit=getSplitPointHelper(parent,markedCommits);

            }
        }
    }
    public static Commit getSplitPoint(String currentBranchName,String givenBranchName){
        State gitletState=getState();
        String currentBranch=gitletState.branches.get(currentBranchName);
        String givenBranch=gitletState.branches.get(givenBranchName);

        HashSet<String> markedCommits=new HashSet<>();
        markCommits(currentBranch,markedCommits);

        Queue<String> fringe=new LinkedList<>();
        fringe.add(givenBranch);

        while(!fringe.isEmpty()){
            Commit commit=getCommit(fringe.poll());
            if(markedCommits.contains(commit.UID)){
                return commit;
            }
            for()
            fringe.add()
        }
    }

}
