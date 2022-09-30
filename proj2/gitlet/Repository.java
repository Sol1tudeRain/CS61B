package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.MyUtils.*;
import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
/**
 * This class contains some methods used in Main.
 * <p></p>
 * In CWD, files are named by their original name, like "t.txt",
 * but in staging area directory and blobs directory, files are named by their SHA-1 value,
 * like "2ef7bde608ce5404e97d5f042f95f89f1c232871".
 * And commits objects are also using SHA-1 value as their names.
 * @author Sol1tueRain
 */
public class Repository {
     /** The current working directory */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**　The staging area directory　*/
    public static final File STAGING_DIR = join(GITLET_DIR, "stagingArea");

    /**　The commits directory */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");

    /** Where to store blobs */
    public static final File BLOBS_DIR = join(GITLET_DIR,"blobs");

    /** Where to store gitlet state object */
    public static final File STATE_PATH = join(GITLET_DIR, "state");

     /** A main method for test */
    public static void main(String[] args) throws IOException {
        restrictedDelete(join(CWD,"abc"));
        Date date=new Date(0);
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ",Locale.ENGLISH);
        String str=formatter.format(date);
        System.out.println(str);
    }
    public static void init() {
        // If there is already a Gitlet version-control system in the current directory, abort. */
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        // Make directories.
        GITLET_DIR.mkdir();
        STAGING_DIR.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();

        // First commit
        Commit initCommit = new Commit("initial commit");

        // Set the timestamp to 00:00:00 UTC, Thursday, 1 January 1970
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ",Locale.ENGLISH);
        Date date=new Date(0);
        initCommit.date =formatter.format(date);
        initCommit.parentID = null;// The init commit has no parent
        initCommit.UID = sha1(serialize(initCommit));// Every commit has a unique ID, which is its SHA-1 value
        initCommit.save();// Write the commit to hard disk

        // Create and initialize gitlet state
        State gitletState = new State();
        gitletState.HEAD = initCommit.UID; // Set the current commit to init commit.
        gitletState.branches.put("master", initCommit.UID);// Add master to branches map, which maps from name to ID.
        gitletState.currentBranch = "master";// The default branch is master.
        gitletState.save();
    }

    public static void add(String fileName) throws IOException {
        // If the file doesn't exist, abort.
        File fileToAdd=join(CWD,fileName);
        if(!fileToAdd.exists()){
            System.out.println("File does not exist.");
            System.exit(0);
        }

        State gitletState = getState();
        /*
         * If the current working version of the file is identical to the version in the current commit,
         * do not stage it to be added, and remove it from the staging area
         * if it is already there (as can happen when a file is changed, added,
         * and then changed back to its original version).
         */
        Commit currentCommit=getCommit(gitletState.HEAD);
        String fileID=currentCommit.trackedFiles.get(fileName); //Get the ID of the tracked file.

        // If there exists a tracked file with the same name
        if(fileID!=null){
            File trackedFile=join(BLOBS_DIR,fileID);
            // If two files are the same
            if(Files.mismatch(trackedFile.toPath(),fileToAdd.toPath())==-1L){
                gitletState.stagedFiles.remove(fileName);
                File stagedFile=join(STAGING_DIR,fileID);
                stagedFile.delete();
                gitletState.save();
                System.exit(0);
            }
        }

        String fileToAddID=sha1(readContentsAsString(fileToAdd));// Produce file ID
        File des = join(STAGING_DIR, fileToAddID);
        Files.copy(fileToAdd.toPath(), des.toPath(), REPLACE_EXISTING);
        gitletState.stagedFiles.put(fileName,fileToAddID);

        gitletState.removedFiles.remove(fileName);
        gitletState.save();
    }

    public static void commit(String message) throws CloneNotSupportedException{
        // There must be a message for every commit.
        if(message==null){
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        State gitletState = getState();

        //If no files have been staged, abort.
        if(gitletState.stagedFiles.isEmpty()){
            System.out.println("No changes added to the commit.");
        }

        Commit currentCommit=getCommit(gitletState.HEAD);
        // By default, each commit’s snapshot of files will be exactly the same as its parent commit’s
        Commit newCommit= (Commit) currentCommit.clone();
        newCommit.parentID= currentCommit.UID;
        newCommit.message=message;
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ", Locale.ENGLISH);
        newCommit.date=formatter.format(new Date());

        // The new commit will save and start tracking any files that were staged for addition.
        gitletState.stagedFiles.forEach((fileName,fileID)->{
            newCommit.trackedFiles.put(fileName,fileID);
            File stagedFilePath=join(STAGING_DIR,fileID);
            File des=join(BLOBS_DIR,fileID);
            // If there is a file with the same ID, don't need to copy because same ID implies same contents.
            if(!des.exists()){
                try {
                    Files.copy(stagedFilePath.toPath(),des.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        /*
         * Files tracked in the current commit will be untracked
         * in the new commit as a result being staged for removal by the rm
         */
        for(String removedFileName:gitletState.removedFiles){
            gitletState.stagedFiles.remove(removedFileName);
        }

        newCommit.UID=sha1(serialize(newCommit));
        newCommit.save();

        clearDir(STAGING_DIR);
        gitletState.stagedFiles.clear();
        gitletState.HEAD= newCommit.UID;
        gitletState.branches.put(gitletState.currentBranch, gitletState.HEAD);
        gitletState.save();
    }

    public static void rm(String fileName){
        State gitletState=getState();
        Commit currentCommit=getCommit(gitletState.HEAD);
        boolean staged=gitletState.stagedFiles.containsKey(fileName);
        boolean tracked=currentCommit.trackedFiles.containsKey(fileName);
         // If the file is neither staged nor tracked by the head commit, abort.
        if(!staged&&!tracked){
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        // Unstage the file if it is currently staged for addition.
        if(staged){
            String fileID=gitletState.stagedFiles.get(fileName);
            File fileToUnstage=join(STAGING_DIR,fileID);
            fileToUnstage.delete();
            gitletState.stagedFiles.remove(fileName);
            gitletState.save();
        }
        /* If the file is tracked in the current commit, stage it for removal
           and remove the file from the working directory if the user has not already done so. */
        if(tracked){
            gitletState.removedFiles.add(fileName);
            File fileToRemove = join(CWD,fileName);
            restrictedDelete(fileToRemove);
            gitletState.save();
        }
    }

    public static void log(){
        State gitletState=getState();
        Commit commit=getCommit(gitletState.HEAD);
        while (true){
            System.out.println("===\n" +
                    "commit "+commit.UID+"\n"+
                    "Date: "+commit.date+"\n"+
                    commit.message+"\n");
            if(commit.parentID==null){
                break;
            }else {
                commit=getCommit(commit.parentID);
            }
        }
    }

    public static void global_log(){
        File filesList[]=COMMITS_DIR.listFiles();
        for(File file:filesList){
            Commit commit=readObject(file, Commit.class);
            System.out.println("===\n" +
                    "commit "+commit.UID+"\n"+
                    "Date: "+commit.date+"\n"+
                    commit.message+"\n");
        }
    }

    public static void find(String message){
        File filesList[]=COMMITS_DIR.listFiles();
        boolean found=false;
        for(File file:filesList){
            Commit commit=readObject(file, Commit.class);
            if(commit.message.equals(message)){
                found=true;
                System.out.println(commit.UID);
            }
        }
        if(!found){
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status(){
        State gitletState=getState();

        System.out.println("=== Branches ===");
        System.out.println("*"+gitletState.currentBranch);
        gitletState.branches.remove(gitletState.currentBranch);

        TreeMap<String, String> sortedMap = new TreeMap<>(gitletState.branches);

        for(String branch:sortedMap.keySet()){
            System.out.println(branch);
        }

        System.out.println("\n=== Staged Files ===");
        sortedMap = new TreeMap<>(gitletState.stagedFiles);
        for(String fileName:sortedMap.keySet()){
            System.out.println(fileName);
        }

        System.out.println("\n=== Removed Files ===");
        TreeSet<String> sortedSet=new TreeSet<>(gitletState.removedFiles);
        for(String fileName:sortedSet){
            System.out.println(fileName);
        }

        System.out.println("\n=== Modifications Not Staged For Commit ===");
        System.out.println("\n=== Untracked Files ===\n");

    }

    /**
     * Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     */
    public static void checkout(String fileName) throws IOException {
        State gitletState=getState();
        Commit currentCommit=getCommit(gitletState.HEAD);

        String blobID=currentCommit.trackedFiles.get(fileName);
        //If the file does not exist in the current commit, abort.
        if(blobID==null){
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        File blob=join(BLOBS_DIR,blobID);
        File des=join(CWD,fileName);
        Files.copy(blob.toPath(),des.toPath(),REPLACE_EXISTING);
        gitletState.stagedFiles.remove(fileName);
        gitletState.save();
        File fileToUnstage=join(STAGING_DIR,blobID);
        if(fileToUnstage.exists()){
            fileToUnstage.delete();
        }
    }

    /**
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     */
    public static void checkout(String commitID,String fileName) throws IOException {
        Commit commit=getCommit(commitID);
        String blobID=commit.trackedFiles.get(fileName);
        //If the file does not exist in the denoted commit, abort.
        if(blobID==null){
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        File blob=join(BLOBS_DIR,blobID);
        File des=join(CWD,fileName);
        Files.copy(blob.toPath(),des.toPath(),REPLACE_EXISTING);

        State gitletState=getState();
        gitletState.stagedFiles.remove(fileName);
        gitletState.save();
        File fileToUnstage=join(STAGING_DIR,blobID);
        if(fileToUnstage.exists()){
            fileToUnstage.delete();
        }
    }

    /**
     * Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions of the files
     * that are already there if they exist. Also, at the end of this command,
     * the given branch will now be considered the current branch (HEAD).
     * Any files that are tracked in the current branch
     * but are not present in the checked-out branch are deleted.
     * The staging area is cleared, unless the checked-out branch is the current branch
     */
    public static void checkoutBranch(String branchName){
        State gitletState=getState();
        if(gitletState.currentBranch.equals(branchName)){
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        String commitID=gitletState.branches.get(branchName);
        if(commitID==null){
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        Commit commit=getCommit(commitID);
        // Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
        //List<String> fileNames=plainFilenamesIn(CWD);
        Commit currentCommit=getCommit(gitletState.HEAD);
        for(String fileName:currentCommit.trackedFiles.keySet()){
            if(!commit.trackedFiles.containsKey(fileName)){
                File fileToDelete=join(CWD,fileName);
                restrictedDelete(fileToDelete);
            }
        }
        /**
         * Takes all files in the commit at the head of the given branch,
         * and puts them in the working directory, overwriting the versions of the files
         * that are already there if they exist.
         */
        commit.trackedFiles.forEach((fileName,fileID)->{
            File src=join(BLOBS_DIR,fileID);
            File des=join(CWD,fileName);
            try {
                Files.copy(src.toPath(),des.toPath(),REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        gitletState.currentBranch=branchName;
        gitletState.HEAD= gitletState.branches.get(branchName);
        gitletState.stagedFiles.clear();
        clearDir(STAGING_DIR);
        gitletState.removedFiles.clear();
        gitletState.save();
    }

    public static void branch(String branchName){
        State gitletState=getState();
        // If a branch with the given name already exists, abort.
        if(gitletState.branches.containsKey(branchName)){
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        gitletState.branches.put(branchName, gitletState.HEAD);
        gitletState.save();
    }

    public static void rm_branch(String branchName){
        State gitletState=getState();
        // If a branch with the given name does not exist, abort.
        if(!gitletState.branches.containsKey(branchName)){
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        // If you try to remove the branch you’re currently on, abort.
        if(gitletState.currentBranch.equals(branchName)){
            System.out.println("Cannot remove the current branch.");
        }
        gitletState.branches.remove(branchName);
        gitletState.save();
    }

    public static void reset(String commitID){
        Commit commit=getCommit(commitID);
        // If no commit with the given id exists, abort.
        if(commit==null){
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        State gitletState=getState();
        Commit currentCommit=getCommit(gitletState.HEAD);
        // Check out all the files tracked by the given commit.
        commit.trackedFiles.forEach((fileName,fileID)->{
            File src=join(BLOBS_DIR,fileID);
            File des=join(CWD,fileName);
            try {
                Files.copy(src.toPath(),des.toPath(),REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // Remove tracked files that are not present in that commit.
        for(String fileName:currentCommit.trackedFiles.keySet()){
            if(!commit.trackedFiles.containsKey(fileName)){
                File fileToDelete=join(CWD,fileName);
                restrictedDelete(fileToDelete);
            }
        }

        gitletState.HEAD=commitID;
        gitletState.stagedFiles.clear();
        clearDir(STAGING_DIR);
        gitletState.removedFiles.clear();
        gitletState.save();

    }
}
