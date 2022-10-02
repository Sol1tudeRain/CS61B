package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static gitlet.Repository.STATE_PATH;
import static gitlet.Utils.writeObject;

/**
 * This class stores the status of gitlet
 * For reducing ambiguity, even though both file names or object names and their SHA-1 value
 * are String. In this project, name is referred to real name, and ID is referred to SHA-1 value.
 * @author Sol1tudeRain
 */
public class State implements Serializable {

    /** Stores branch name and SHA-1 value pairs */
    public HashMap<String,String> branches;
    public String HEAD;
    public String currentBranch;
    /** StagedFiles name and SHA-1 value pairs */
    public HashMap<String,String> stagedFilesForAddition;

    /** Names of files staged for removal */
    public HashSet<String> stagedFilesForRemoval;

    /** Map abbreviated commit UID to full UID */
    public HashMap<String,String> shortIDs;

    State(){
        this.branches=new HashMap<>();
        this.currentBranch="master";
        this.stagedFilesForAddition=new HashMap<>();
        this.stagedFilesForRemoval=new HashSet<>();
        this.shortIDs=new HashMap<>();
    }

    /** Save the state object */
    public void save(){
        writeObject(STATE_PATH,this);
    }

}
