package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import static gitlet.Repository.COMMITS_DIR;
import static gitlet.Utils.*;


/** Represents a gitlet commit object.
 *  This file defines what the structure of a commit class looks like
 *  and includes some methods.
 *
 *  @author Sol1tudeRain
 */
public class Commit implements Serializable, Cloneable{
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */


    /** The variables of this Commit. */
    public String message;
    public String parentID;
    public String date;
    public String UID;

    /** A map used to save the relationship between filenames and their contents.
     *  Filenames are normal names and contents are referred to by SHA-1 ID.
     */
    public HashMap<String,String> trackedFiles;

    public Commit(String message) {
        this.message=message;
        this.trackedFiles=new HashMap<>();
    }
    
    public void save(){
        File commitPath=join(COMMITS_DIR,this.UID);
        writeObject(commitPath,this);
    }
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException {
        Commit ret=(Commit)super.clone(); // "ret" is the abbreviation of return
        ret.trackedFiles=(HashMap<String,String>)trackedFiles.clone();
        return ret;
    }
}
