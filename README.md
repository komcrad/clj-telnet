# clj-telnet

A simple clojure telnet client

## Usage
[![Clojars Project](https://img.shields.io/clojars/v/clj-telnet.svg)](https://clojars.org/clj-telnet)
<br>
[![CircleCI](https://circleci.com/gh/komcrad/clj-telnet/tree/master.svg?style=shield&circle-token=6b419fb5b3d199db9734c9820df8e9686d3bee4d)](https://circleci.com/gh/komcrad/clj-telnet/tree/master)

## Example
### Assuming you're in a repl (`lein repl`)...

Import the core namespace:

`(use 'clj-telnet.core)`

Create a telnet object (Don't forget to close when finished)

`(def telnet (get-telnet url port))`

Now you can read and write from that object.

Read buffer until some-string:

`(read-until telnet some-string)`

Send command to telnet server:

`(write telnet some-command)`

Read everything from the buffer (don't use in production)

`(read-all telnet)`

Close the telnet connection

`(kill-telnet telnet)`

Use read-in-char and write-out-data hooks

```
(binding [*read-in-char* prn *write-out-data* prn]
  (read-until telnet some-string))
```

Control what happens on read exception.
Eg. Swallow exception
```
(binding [*read-err* (constantly nil)]
  (read-a-char telnet))
```

Change charset (default: `UTF-8`)

```
(binding [*charset* "UTF-16"]
  (read-all telnet))
```
