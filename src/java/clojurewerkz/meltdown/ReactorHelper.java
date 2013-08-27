package clojurewerkz.meltdown;

import reactor.event.registry.Registry;
import reactor.event.Event;
import reactor.event.registry.CachingRegistry;
import reactor.function.Consumer;


import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;

public class ReactorHelper {

	public static IPersistentMap eventToMap(Event event) {
		ITransientMap ret = PersistentHashMap.EMPTY.asTransient();

		ret.assoc(Keyword.intern("data"), event.getData());
		ret.assoc(Keyword.intern("reply-to"), event.getReplyTo());
		ret.assoc(Keyword.intern("headers"), event.getHeaders());
		ret.assoc(Keyword.intern("id"), event.getId());

		return ret.persistent();
	}
}
