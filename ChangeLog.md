## Initial Release: 1.0.0-alpha1

Initial release

Supported features:

  * Reactor operations, such as `notify`, `on`, `send`, `receive`
  * Reactor configuration options, such as `dispatcher` and `routing-strategy`
  * Selectors, `$` and `regexp` ones
  * Support for raw operations on reactor to avoid overhead of wrapping and deserialization
    on Meltdown side
  * Stream & deferred operations such as `map*`, `reduce*`, `filter*` and `batch*`
