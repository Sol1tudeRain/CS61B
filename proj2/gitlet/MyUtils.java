package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MyUtils {
    /**
     * A test main method
     */
    public static void main(String[] args) throws IOException {
        File src = join(CWD, "source.txt");
        File des = join(CWD, "destination.txt");
        Files.copy(src.toPath(), des.toPath(), REPLACE_EXISTING);
        //copyFileUsingStream(src,des);
        //System.out.println(CWD);
    }

    public static void safeDelete(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Return the gitlet state object
     */
    public static State getState() {
        if (STATE_PATH.exists()) {
            return readObject(STATE_PATH, State.class);
        }
        System.out.println("Not in an initialized Gitlet directory.");
        System.exit(0);
        return null;
    }

    /**
     * Return a commit object with the specified ID
     */
    public static Commit getCommit(String commitID) {
        if (commitID == null) {
            return null;
        }

        String ID;
        if (commitID.length() == 8) {
            State gitletState = getState();
            ID = gitletState.shortIDs.get(commitID);
            if (ID == null) {
                return null;
            }
        } else {
            ID = commitID;
        }

        File commitPath = join(COMMITS_DIR, ID);
        if (commitPath.exists()) {
            return readObject(commitPath, Commit.class);
        } else {
            return null;
        }

    }

    /**
     * Delete all files in a directory
     */
    public static void clearDir(File dirPath) {
        File filesList[] = dirPath.listFiles();
        for (File file : filesList) {
            file.delete();
        }
    }

    public static void stage(File file) {

    }

    /**
     * Find the latest common ancestor of the current and given branch heads.
     * Inspired by BFS algorithm.
     *
     * @return A commit object if found, otherwise null.
     */
    public static Commit getSplitPoint(String currentBranch, String givenBranch) {
        HashSet<String> markedCommitsA = new HashSet<>();
        HashSet<String> markedCommitsB = new HashSet<>();

        Queue<String> fringeA = new LinkedList<>();
        Queue<String> fringeB = new LinkedList<>();
        fringeA.add(currentBranch);
        fringeB.add(givenBranch);

        while (!fringeA.isEmpty() || !fringeB.isEmpty()) {
            Commit commitA = getCommit(fringeA.poll());
            if (commitA != null) {
                if (markedCommitsB.contains(commitA.UID)) {
                    return commitA;
                }
                markedCommitsA.add(commitA.UID);
                for (String commitID : commitA.parents) {
                    if (!markedCommitsA.contains(commitID)) {
                        fringeA.add(commitID);
                    }
                }
            }

            Commit commitB = getCommit(fringeB.poll());
            if (commitB != null) {
                if (markedCommitsA.contains(commitB.UID)) {
                    return commitB;
                }
                markedCommitsB.add(commitB.UID);
                for (String commitID : commitB.parents) {
                    if (!markedCommitsB.contains(commitID)) {
                        fringeB.add(commitID);
                    }
                }
            }
        }
        return null;
    }

}
