import com.dxm.auto.rxtension.Dynamic;
import com.dxm.auto.rxtension.Partial;
import com.dxm.auto.rxtension.RXtension;
import com.dxm.auto.rxtension.RXtensionClass;
import com.dxm.auto.rxtension.Receiver;

import rx.functions.Action0;
import rx.functions.Func0;

/**
 * Created by Phoebe on 9/10/16.
 */
@RXtensionClass("testabc")
public class Test {
  @RXtension
  public void test(@Partial String test) {

  }
  Action0 a = new Action0() {
    @Override
    public void call() {

    }
  };


//  static class TestBindings {
//    private final Test test;
//
//    TestBindings(@Partial Test test) {
//      this.test = test;
//    }
//
//    public Func0<Integer> bindings0() {
//      return new Bindings_Class(test);
//    }
//
//    private static class Bindings_Class implements Func0<Integer> {
//      private final Test test;
//
//      private Bindings_Class(Test test) {
//        this.test = test;
//      }
//
//      @Override
//      public Integer call() {
//        return test.test();
//      }
//    }
//  }
}

