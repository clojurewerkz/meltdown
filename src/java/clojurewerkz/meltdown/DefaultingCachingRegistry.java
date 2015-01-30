// Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package clojurewerkz.meltdown;

import reactor.event.registry.CachableRegistration;
import reactor.event.registry.CachingRegistry;
import reactor.event.registry.Registration;
import reactor.event.selector.MatchAllSelector;
import reactor.function.Consumer;
import reactor.event.Event;

import java.util.List;

public class DefaultingCachingRegistry<T extends Consumer<Event>> extends CachingRegistry<T> {

  public final T defaultConsumer;

  public DefaultingCachingRegistry(T consumer) {
    super();
    this.defaultConsumer = consumer;
  }

  @Override
  public List<Registration<? extends T>> select(final Object key) {
    List<Registration<? extends T>> res = super.select(key);
    if (res.isEmpty()) {
      res.add(new CachableRegistration<T>(new MatchAllSelector(),
                                          defaultConsumer,
                                          null));
    }
    return res;
  }



}
