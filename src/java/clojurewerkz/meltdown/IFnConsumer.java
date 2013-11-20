package clojurewerkz.meltdown;

import clojure.lang.IFn;
import reactor.function.Consumer;

public class IFnConsumer<T> implements Consumer<T> {

  private final IFn fn;

  public IFnConsumer(IFn fn) {
    this.fn = fn;
  }

  @Override
  public void accept(T value) {
		fn.invoke(value);
  }

}
