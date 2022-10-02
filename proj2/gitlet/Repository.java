package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
 *
 * @author Sol1tueRain
 */
public class Repository {
    /**
     * The current working directory
     */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * 　The staging area directory
     */
    public static final File STAGING_DIR = join(GITLET_DIR, "stagingArea");

    /**
     * 　The commits directory
     */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");

    /**
     * Where to store blobs
     */
    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");

    /**
     * Where to store gitlet state object
     */
    public static final File STATE_PATH = join(GITLET_DIR, "state");


    /**
     * A main method for test
     */
    public static void main(String[] args) throws IOException {
        List<String> p = new ArrayList<>();
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
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ", Locale.ENGLISH);
        Date date = new Date(0);
        initCommit.date = formatter.format(date);
        initCommit.UID = sha1(serialize(initCommit));// Every commit has a unique ID, which is its SHA-1 value
        initCommit.parents.add(null);
        initCommit.save();

        // Create and initialize gitlet state
        State gitletState = new State();
        gitletState.shortIDs.put(initCommit.UID.substring(0, 8), initCommit.UID);
        gitletState.HEAD = initCommit.UID; // Set the current commit to init commit.
        gitletState.branches.put("master", initCommit.UID);// Add master to branches map, which maps from name to ID.
        gitletState.save();
    }

    public static void add(String fileName) throws IOException {
        // If the file doesn't exist, abort.
        File fileToAdd = join(CWD, fileName);
        if (!fileToAdd.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        State gitletState = getState();

        // The file will no longer be staged for removal.
        gitletState.stagedFilesForRemoval.remove(fileName);

        /*
         * If the current working version of the file is identical to the version in the current commit,
         * do not stage it to be added, and remove it from the staging area
         * if it is already there (as can happen when a file is changed, added,
         * and then changed back to its original version).
         */
        Commit currentCommit = getCommit(gitletState.HEAD);
        String trackedFileID = currentCommit.trackedFiles.get(fileName); //Get the ID of the tracked file.
        String fileToAddID = sha1(readContentsAsString(fileToAdd)); // Produce ID of the file to add.

        /* Check if the current working version of the file
           is identical to the version in the current commit if there is one. */
        if (fileToAddID.equals(trackedFileID)) {
            gitletState.stagedFilesForAddition.remove(fileName);
            File stagedFile = join(STAGING_DIR, fileToAddID);
            safeDelete(stagedFile);
            gitletState.save();
            System.exit(0);
        }

        File des = join(STAGING_DIR, fileToAddID);
        // Staging an already-staged file overwrites the previous entry in the staging area with the new contents.
        Files.copy(fileToAdd.toPath(), des.toPath(), REPLACE_EXISTING);
        gitletState.stagedFilesForAddition.put(fileName, fileToAddID);
        gitletState.save();
    }

    public static void commit(String message) throws CloneNotSupportedException {
        // There must be a message for every commit.
        if (message == null || message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        State gitletState = getState();

        //If no files have been staged, abort.
        if (gitletState.stagedFilesForAddition.isEmpty() && gitletState.stagedFilesForRemoval.isEmpty()) {
            System.out.println("No changes added to the commit.");
        }

        Commit currentCommit = getCommit(gitletState.HEAD);

        // By default, each commit’s snapshot of files will be exactly the same as its parent commit’s
        Commit newCommit = (Commit) currentCommit.clone();

        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ", Locale.ENGLISH);
        newCommit.date = formatter.format(new Date());
        newCommit.parents = new LinkedList<>();
        newCommit.parents.add(currentCommit.UID);
        newCommit.message = message;

        /* The new commit will save and start tracking any files that were staged for addition,
           and update the contents of files it is tracking that have been staged for addition. */
        gitletState.stagedFilesForAddition.forEach((fileName, fileID) -> {
            newCommit.trackedFiles.put(fileName, fileID);
            File stagedFileToAdd = join(STAGING_DIR, fileID);
            File des = join(BLOBS_DIR, fileID);
            // If there is already a file with the same ID, there's need to overwrite because same ID implies same contents.
            if (!des.exists()) {
                try {
                    Files.copy(stagedFileToAdd.toPath(), des.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        /* Files tracked in the current commit will be untracked in the new commit
           as a result being staged for removal by the rm command */
        for (String removedFileName : gitletState.stagedFilesForRemoval) {
            newCommit.trackedFiles.remove(removedFileName);
        }

        newCommit.UID = sha1(serialize(newCommit));
        newCommit.save();

        //The staging area is cleared after a commit.
        clearDir(STAGING_DIR);
        gitletState.stagedFilesForAddition.clear();
        gitletState.stagedFilesForRemoval.clear();

        gitletState.shortIDs.put(newCommit.UID.substring(0, 8), newCommit.UID);
        gitletState.HEAD = newCommit.UID;
        gitletState.branches.put(gitletState.currentBranch, gitletState.HEAD);
        gitletState.save();
    }

    public static void rm(String fileName) {
        State gitletState = getState();
        Commit currentCommit = getCommit(gitletState.HEAD);
        boolean staged = gitletState.stagedFilesForAddition.containsKey(fileName);
        boolean tracked = currentCommit.trackedFiles.containsKey(fileName);

        // If the file is neither staged nor tracked by the head commit, abort.
        if (!staged && !tracked) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        // Unstage the file if it is currently staged for addition.
        if (staged) {
            String fileID = gitletState.stagedFilesForAddition.get(fileName);
            File fileToUnstage = join(STAGING_DIR, fileID);
            fileToUnstage.delete();
            gitletState.stagedFilesForAddition.remove(fileName);
            gitletState.save();
        }
        /* If the file is tracked in the current commit, stage it for removal
           and remove the file from the working directory if the user has not already done so. */
        if (tracked) {
            gitletState.stagedFilesForRemoval.add(fileName);
            File fileToRemove = join(CWD, fileName);
            safeDelete(fileToRemove);
            gitletState.save();
        }
    }

    public static void log() {
        State gitletState = getState();
        Commit commit = getCommit(gitletState.HEAD);
        while (true) {
            if(commit.parents.size()==2){
                System.out.println("===\n" +
                        "commit " + commit.UID + "\n" +
                        "Merge: " + commit.parents.get(0).substring(0,7)+" "+commit.parents.get(1).substring(0,7) + "\n" +
                        "Date: " + commit.date + "\n" +
                        commit.message + "\n");
            }else {
                System.out.println("===\n" +
                        "commit " + commit.UID + "\n" +
                        "Date: " + commit.date + "\n" +
                        commit.message + "\n");
            }

            if (commit.parents.get(0) == null) {
                break;
            } else {
                commit = getCommit(commit.parents.get(0));
            }
        }
    }

    public static void global_log() {
        File filesList[] = COMMITS_DIR.listFiles();
        for (File file : filesList) {
            Commit commit = readObject(file, Commit.class);
            System.out.println("===\n" +
                    "commit " + commit.UID + "\n" +
                    "Date: " + commit.date + "\n" +
                    commit.message + "\n");
        }
    }

    public static void find(String message) {
        File filesList[] = COMMITS_DIR.listFiles();
        boolean found = false;
        for (File file : filesList) {
            Commit commit = readObject(file, Commit.class);
            if (commit.message.equals(message)) {
                found = true;
                System.out.println(commit.UID);
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        State gitletState = getState();

        System.out.println("=== Branches ===");
        System.out.println("*" + gitletState.currentBranch);
        gitletState.branches.remove(gitletState.currentBranch);

        TreeMap<String, String> sortedMap = new TreeMap<>(gitletState.branches);

        for (String branch : sortedMap.keySet()) {
            System.out.println(branch);
        }

        System.out.println("\n=== Staged Files ===");
        sortedMap = new TreeMap<>(gitletState.stagedFilesForAddition);
        for (String fileName : sortedMap.keySet()) {
            System.out.println(fileName);
        }

        System.out.println("\n=== Removed Files ===");
        TreeSet<String> sortedSet = new TreeSet<>(gitletState.stagedFilesForRemoval);
        for (String fileName : sortedSet) {
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
        State gitletState = getState();
        Commit currentCommit = getCommit(gitletState.HEAD);

        String blobID = currentCommit.trackedFiles.get(fileName);
        //If the file does not exist in the current commit, abort.
        if (blobID == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        File blob = join(BLOBS_DIR, blobID);
        File des = join(CWD, fileName);
        Files.copy(blob.toPath(), des.toPath(), REPLACE_EXISTING);

        // Unstage the file
        gitletState.stagedFilesForAddition.remove(fileName);
        File fileToUnstage = join(STAGING_DIR, blobID);
        safeDelete(fileToUnstage);
        gitletState.save();
    }

    /**
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     */
    public static void checkout(String commitID, String fileName) throws IOException {
        Commit commit = getCommit(commitID);
        // If no commit with the given id exists
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        String blobID = commit.trackedFiles.get(fileName);
        //If the file does not exist in the denoted commit, abort.
        if (blobID == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        File blob = join(BLOBS_DIR, blobID);
        File des = join(CWD, fileName);
        Files.copy(blob.toPath(), des.toPath(), REPLACE_EXISTING);

        State gitletState = getState();
        gitletState.stagedFilesForAddition.remove(fileName);
        File fileToUnstage = join(STAGING_DIR, blobID);
        safeDelete(fileToUnstage);
        gitletState.save();
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
    public static void checkoutBranch(String branchName) {
        State gitletState = getState();
        if (gitletState.currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        String commitID = gitletState.branches.get(branchName);
        if (commitID == null) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        Commit currentCommit = getCommit(gitletState.HEAD);
        Commit commit = getCommit(commitID);
        List<String> fileNames = plainFilenamesIn(CWD);
        // If a working file is untracked in the current branch and would be overwritten by the checkout, abort.
        for (String fileName : fileNames) {
            if (!currentCommit.trackedFiles.containsKey(fileName) && commit.trackedFiles.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // Any files that are tracked in the current branch but are not present in the checked-out branch are deleted.
        for (String fileName : currentCommit.trackedFiles.keySet()) {
            if (!commit.trackedFiles.containsKey(fileName)) {
                File fileToDelete = join(CWD, fileName);
                safeDelete(fileToDelete);
            }
        }
        /* Takes all files in the commit at the head of the given branch,
           and puts them in the working directory, overwriting the versions of the files
           that are already there if they exist. */
        commit.trackedFiles.forEach((fileName, fileID) -> {
            File src = join(BLOBS_DIR, fileID);
            File des = join(CWD, fileName);
            try {
                Files.copy(src.toPath(), des.toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        gitletState.currentBranch = branchName;
        gitletState.HEAD = gitletState.branches.get(branchName);

        // The staging area is cleared
        gitletState.stagedFilesForAddition.clear();
        clearDir(STAGING_DIR);
        gitletState.stagedFilesForRemoval.clear();
        gitletState.save();
    }

    public static void branch(String branchName) {
        State gitletState = getState();
        // If a branch with the given name already exists, abort.
        if (gitletState.branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        gitletState.branches.put(branchName, gitletState.HEAD);
        gitletState.save();
    }

    public static void rm_branch(String branchName) {
        State gitletState = getState();
        // If a branch with the given name does not exist, abort.
        if (!gitletState.branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        // If you try to remove the branch you’re currently on, abort.
        if (gitletState.currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        }
        gitletState.branches.remove(branchName);
        gitletState.save();
    }


    public static void reset(String commitID) {
        Commit commit = getCommit(commitID);
        // If no commit with the given id exists, abort.
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        State gitletState = getState();
        Commit currentCommit = getCommit(gitletState.HEAD);
        List<String> fileNames = plainFilenamesIn(CWD);
        // If a working file is untracked in the current branch and would be overwritten by the checkout, abort.
        for (String fileName : fileNames) {
            if (!currentCommit.trackedFiles.containsKey(fileName) && commit.trackedFiles.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // Check out all the files tracked by the given commit.
        commit.trackedFiles.forEach((fileName, fileID) -> {
            File src = join(BLOBS_DIR, fileID);
            File des = join(CWD, fileName);
            try {
                Files.copy(src.toPath(), des.toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // Remove tracked files that are not present in that commit.
        for (String fileName : currentCommit.trackedFiles.keySet()) {
            if (!commit.trackedFiles.containsKey(fileName)) {
                File fileToDelete = join(CWD, fileName);
                safeDelete(fileToDelete);
            }
        }

        gitletState.HEAD = commitID;
        gitletState.branches.put(gitletState.currentBranch, gitletState.HEAD);
        gitletState.stagedFilesForAddition.clear();
        clearDir(STAGING_DIR);
        gitletState.stagedFilesForRemoval.clear();
        gitletState.save();

    }

    public static void merge(String branchName) throws CloneNotSupportedException {
        State gitletState = getState();
        // If there are staged additions or removals
        if (!gitletState.stagedFilesForAddition.isEmpty() || !gitletState.stagedFilesForRemoval.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        // If attempting to merge a branch with itself
        if (branchName.equals(gitletState.currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        String givenBranch = gitletState.branches.get(branchName);
        // If a branch with the given name does not exist
        if (givenBranch == null) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String currentBranch = gitletState.HEAD;
        Commit HEAD = getCommit(currentBranch);
        Commit Other = getCommit(givenBranch);
        Commit Split = getSplitPoint(currentBranch, givenBranch);

        // If the split point is the same commit as the given branch, then we do nothing.
        if(Other.UID.equals(Split.UID)){
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        // If the split point is the current branch, then the effect is to check out the given branch.
        if(HEAD.UID.equals(Split.UID)){
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }

        boolean conflict = false;

        Commit newCommit = (Commit) HEAD.clone();

        HashSet<String> namesOfAllFilesToHandle = new HashSet<>();
        namesOfAllFilesToHandle.addAll(HEAD.trackedFiles.keySet());
        namesOfAllFilesToHandle.addAll(Other.trackedFiles.keySet());
        namesOfAllFilesToHandle.addAll(Split.trackedFiles.keySet());

        HashMap<String, String> filesToAdd = new HashMap<>();// Names and IDs
        HashSet<String> filesToRemove = new HashSet<>();// Names
        HashMap<String, String> conflictFiles = new HashMap<>();// Names and contents

        for (String fileName : namesOfAllFilesToHandle) {
            String fileID_Split = Split.trackedFiles.get(fileName);
            String fileID_HEAD = HEAD.trackedFiles.get(fileName);
            String fileID_Other = Other.trackedFiles.get(fileName);

            boolean not_in_Split = fileID_Split == null;
            boolean in_Split = !not_in_Split;
            boolean not_in_HEAD = fileID_HEAD == null;
            boolean in_HEAD = !not_in_HEAD;
            boolean not_in_Other = fileID_Other == null;
            boolean in_Other = !not_in_Other;

            boolean modified_in_HEAD = in_HEAD&&!Objects.equals(fileID_HEAD, fileID_Split);
            boolean unmodified_in_HEAD = !modified_in_HEAD;
            boolean modified_in_Other = in_Other&&!Objects.equals(fileID_Other, fileID_Split);
            boolean unmodified_in_Other = !modified_in_Other;
            boolean in_the_same_way = Objects.equals(fileID_HEAD, fileID_Other);
            boolean not_in_the_same_way = !in_the_same_way;

            // 1. Modified in Other but not modified in HEAD -->checkout
            if (modified_in_Other && unmodified_in_HEAD) {
                newCommit.trackedFiles.put(fileName, fileID_Other);
                filesToAdd.put(fileName, fileID_Other);

                // 2. Modified in HEAD but not modified in Other -->do nothing
            } else if (modified_in_HEAD && unmodified_in_Other) {
                ;
                // 3. If two files have changed in the same way or both are not modified -->do nothing
            } else if (in_the_same_way) {
                ;

                // 4. Present only in HEAD -->do nothing
            } else if (not_in_Split && not_in_Other && in_HEAD) {
                ;

                // 5. Present only in Other -->checkout
            } else if (not_in_Split && not_in_HEAD && in_Other) {
                newCommit.trackedFiles.put(fileName, fileID_Other);
                filesToAdd.put(fileName, fileID_Other);

                // 6. Present in Other, unmodified in HEAD and absent in Other -->remove
            } else if (in_Split && unmodified_in_HEAD && not_in_Other) {
                newCommit.trackedFiles.remove(fileName);
                filesToRemove.add(fileName);

                // 7. Present in Other, unmodified in Other and absent in HEAD -->do nothing
            } else if (in_Split && unmodified_in_Other && not_in_HEAD) {
                ;

                // 8. Modified in different ways -->conflict
            } else if (not_in_the_same_way) {
                conflict = true;
                File fileInHEAD = join(BLOBS_DIR, fileID_HEAD);
                File fileInOther = join(BLOBS_DIR, fileID_Other);
                String str = "<<<<<<< HEAD\n";
                if (fileInHEAD.exists()) {
                    str = str + readContentsAsString(fileInHEAD);
                }
                str = str + "=======\n";
                if (fileInOther.exists()) {
                    str = str + readContentsAsString(fileInOther);
                }
                str = str + ">>>>>>>";

                conflictFiles.put(fileName, str);
            }

        }

        /* **************** No files or objects are saved or deleted above! ************************************* */

        // Check if an untracked file in the current commit would be overwritten or deleted by the merge
        for (String fileName : filesToAdd.keySet()) {
            File file = join(CWD, fileName);
            if (file.exists() && !HEAD.trackedFiles.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (String fileName : filesToRemove) {
            File file = join(CWD, fileName);
            if (file.exists() && !HEAD.trackedFiles.containsKey(fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        filesToAdd.forEach((fileName, fileID) -> {
            File src = join(BLOBS_DIR, fileID);
            File des = join(CWD, fileName);
            try {
                Files.copy(src.toPath(), des.toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        for (String fileName : filesToRemove) {
            File fileToRemove = join(CWD, fileName);
            safeDelete(fileToRemove);
        }

        conflictFiles.forEach((fileName, contents) -> {
            String conflictedFileID = sha1(contents);
            newCommit.trackedFiles.put(fileName, conflictedFileID);
            File conflictedFile = join(CWD, fileName);
            writeContents(conflictedFile, contents);
            writeContents(join(BLOBS_DIR, conflictedFileID), contents);
        });

        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }

        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy ZZZZ", Locale.ENGLISH);
        newCommit.date = formatter.format(new Date());
        newCommit.message = "Merged " + branchName + " into " + gitletState.currentBranch + ".";
        newCommit.parents = new LinkedList<>();
        newCommit.parents.add(currentBranch);
        newCommit.parents.add(givenBranch);
        newCommit.UID = sha1(serialize(newCommit));
        newCommit.save();

        gitletState.HEAD = newCommit.UID;
        gitletState.shortIDs.put(newCommit.UID.substring(0, 8), newCommit.UID);
        gitletState.branches.put(gitletState.currentBranch, gitletState.HEAD);
        gitletState.save();
    }
}
