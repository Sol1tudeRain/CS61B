package gitlet;

import gitlet.Utils.*;

import java.io.IOException;

import static gitlet.Repository.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Sol1tudeRain
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args){
        if (args.length == 0) {
            System.out.println("Please enter a command");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                init();
                break;
            case "add":
                try{
                    add(args[1]);
                }catch (IOException ignored){

                }
                break;
            case "commit":
                try{
                    commit(args[1]);
                }catch (CloneNotSupportedException ignored){

                }
                break;
            case "rm":
                rm(args[1]);
                break;
            case "log":
                log();
                break;
            case "global-log":
                global_log();
                break;
            case "find":
                find(args[1]);
                break;
            case "status":
                status();
                break;
            case "checkout":
                if(args.length==2){
                    checkoutBranch(args[1]);
                } else if (args.length==3) {
                    try{
                        checkout(args[2]);
                    }catch (IOException ignored){

                    }
                } else if (args.length==4) {
                    try{
                        checkout(args[1],args[3]);
                    }catch (IOException ignored){

                    }
                }
                break;
            case "branch":
                branch(args[1]);
                break;
            case "rm-branch":
                rm_branch(args[1]);
                break;
            case "reset":
                reset(args[1]);
                break;
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Invalid number of arguments for: %s.", cmd));
        }
    }
}
