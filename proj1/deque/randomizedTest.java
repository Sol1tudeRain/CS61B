package deque;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class randomizedTest {

    @Test
    public void randomizedTest() {
        ArrayDeque<Integer> A = new ArrayDeque<>();
        LinkedListDeque<Integer> B = new LinkedListDeque<>();
        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 2);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                A.addLast(randVal);
                B.addLast(randVal);
            } else if (operationNumber == 1) {
                //removeLast
                if (A.size() > 0 && B.size() > 0) {
                    assertEquals(A.removeLast(), B.removeLast());
                }
            }
        }
    }
}