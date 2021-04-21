An experiment to combine re-frame subscriptions with fulcro components.



todo

write usage
- (defsc-re-frame)
- subs
- write your own subs
- use sub-name

need to add support to reitit to match on a common prefix to support github pages use case.

# Usage


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
