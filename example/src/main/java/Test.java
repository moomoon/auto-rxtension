import com.dxm.auto.rxtension.RXtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by Phoebe on 9/10/16.
 */
@RXtension
public class Test {
  public void test () {
    CountDownLatch c = new CountDownLatch(2);
    c.countDown();
    c.countDown();
    CyclicBarrier cb = new CyclicBarrier(2);
//    cb
  }
}

