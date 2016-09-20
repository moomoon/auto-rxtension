import com.dxm.auto.rxtension.RXtension;
import com.dxm.auto.rxtension.RXtensionClass;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * Created by Phoebe on 9/10/16.
 */
@RXtensionClass(Test.class)
public class Test {
  @RXtension
  public int test () {
    return 0;
  }
}

