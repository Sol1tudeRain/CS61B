package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static gitlet.Repository.COMMITS_DIR;
import static gitlet.Utils.*;


/** Represents a gitlet commit object.
 *  This file defines the structure of commit object
 *  and includes some methods.
 *
 *  @author Sol1tudeRain
 */
public class Commit implements Serializable, Cloneable{

    public String message;
    public String parentID;
    public String date;
    public String UID;

    /**
     *  A map used to save mappings from filenames to the corresponding IDs.
     *  Filenames are normal names and contents are referred to by SHA-1 value.
     */
    public HashMap<String,String> trackedFiles;

    public Commit(String message) {
        this.message=message;
        this.trackedFiles=new HashMap<>();
    }

    /** Save this commit object to hard disk. */
    public void save(){
        File commitPath=join(COMMITS_DIR,this.UID);
        writeObject(commitPath,this);
    }
    //@SuppressWarnings("unchecked")
    @Override
    public Object clone() throws CloneNotSupportedException {
        Commit ret=(Commit)super.clone();
        ret.trackedFiles=(HashMap<String,String>)trackedFiles.clone();
        return ret;
    }
}
