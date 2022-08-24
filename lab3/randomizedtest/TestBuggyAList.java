package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {

    @Test
    public void test_add_and_remove() {
        AListNoResizing<Integer> A=new AListNoResizing<>();
        BuggyAList<Integer> B=new BuggyAList<>();
        int i;
        for (i = 0; i < 3; i++) {
            A.addLast(i + 3);
            B.addLast(i + 3);
            assertEquals(A.getLast(), B.getLast());
        }
        for (i = 0; i < 3; i++) {
            assertEquals(A.removeLast(), B.removeLast());
        }
    }

    @Test
    public void randomizedTest()
    {
        AListNoResizing<Integer> A = new AListNoResizing<>();
        BuggyAList<Integer> B=new BuggyAList<>();
        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                A.addLast(randVal);
                B.addLast(randVal);
            } else if (operationNumber == 1) {
                // size
                assertEquals(A.size(), B.size());
            } else if (operationNumber==2) {
                //getLast
                if(A.size()>0&&B.size()>0){
                    assertEquals(A.getLast(),B.getLast());
                }
            } else if (operationNumber==3) {
                //removeLast
                if(A.size()>0&&B.size()>0){
                    assertEquals(A.removeLast(),B.removeLast());
                }
            }
        }
    }
}
