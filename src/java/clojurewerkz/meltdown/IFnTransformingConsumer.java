package clojurewerkz.meltdown;

import clojure.lang.IFn;
import reactor.function.Consumer;

public class IFnTransformingConsumer<T> implements Consumer<T> {

  private final IFn acceptor;
	private final IFn transformer;

  public IFnTransformingConsumer(IFn acceptor, IFn transformer) {
    this.acceptor = acceptor;
		this.transformer = transformer;
  }

  @Override
  public void accept(T value) {
		acceptor.invoke(transformer.invoke(value));
  }

}
