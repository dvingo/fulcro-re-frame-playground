# Prerequisites

This template utilizes GNU Make 4.x. You'll need to install it first 
before executing `make`.

This template uses `yarn` to handle npm dependencies.

# Start dev

## Shadow cljs tasks

In one terminal:

```bash
make
```
this runs `yarn` and starts the shadow-cljs server.

Wait for this to complete, then:

In another terminal run:
```bash
make fe
```
This starts the shadow cljs watches.

The file `scripts/start_dev.sh` (invoked by `make fe`) generates a pprint helper ClojureScript file to
allow requiring cljs.pprint during development and not having to remove the namespace for production builds.
If you don't use `make fe` to build your ClojureScript this namespace won't exist on the first build.

Likewise, When building for production use: `make fe-releaes`

Please see the `shadow-cljs.edn` file for ports used for development builds.

If any of those ports are used already shadow-cljs will try different ports so please see the console output 
by shadow-cljs.


## Editor setup

In your editor:
add 2 repls:

### frontend repl:

nREPL remote:

  localhost:$port
  
The $port defaults to 9000 but may be different if 9000 is already in use.

Using this repl you connect to the various ClojureScript builds using `(shadow/repl :build-id)`




