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

    public HashMap<String,String> branches; // Stores branch name and SHA-1 value pairs
    public String HEAD;
    public String currentBranch;
    public HashMap<String,String> stagedFiles; //Stores stagedFiles name and SHA-1 value pairs

    public LinkedList<String> removedFiles;

    State(){
        this.branches=new HashMap<>();
        this.currentBranch="master";
        this.stagedFiles=new HashMap<>();
        this.removedFiles=new LinkedList<>();
    }

    /** Save the state object to hard disk */
    public void save(){
        writeObject(STATE_PATH,this);
    }

}