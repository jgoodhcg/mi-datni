# backend

Start a [REPL](#repls) in your editor or terminal of choice.

Start the server with:

```clojure
(go)
```

The default API is available under http://localhost:3000/api

System configuration is available under `resources/system.edn`.

To reload changes:

```clojure
(reset)
```

## REPLs

### Cursive

Configure a [REPL following the Cursive documentation](https://cursive-ide.com/userguide/repl.html). Using the default "Run with IntelliJ project classpath" option will let you select an alias from the ["Clojure deps" aliases selection](https://cursive-ide.com/userguide/deps.html#refreshing-deps-dependencies).

### CIDER

Use the `cider` alias for CIDER nREPL support (run `clj -M:dev:cider`). See the [CIDER docs](https://docs.cider.mx/cider/basics/up_and_running.html) for more help.

Note that this alias runs nREPL during development. To run nREPL in production (typically when the system starts), use the kit-nrepl library through the +nrepl profile as described in [the documentation](https://kit-clj.github.io/docs/profiles.html#profiles).

For convenience I use a `.dir-locals.el` file so that `cider-jack-in-clj` works as expected. I only utilize this for `cider-jack-in-clj`. When using the dev.sh start script and `cider-connect` this is not necessary.

``` lisp
((clojure-mode . ((cider-clojure-cli-global-options . "-M:dev:cider"))))
```


### Command Line

Run `clj -M:dev:nrepl` or `make repl`.

Note that, just like with [CIDER](#cider), this alias runs nREPL during development. To run nREPL in production (typically when the system starts), use the kit-nrepl library through the +nrepl profile as described in [the documentation](https://kit-clj.github.io/docs/profiles.html#profiles).

### Automatic Repl and Browser Reload
#### Repl Reload
Using Doom Emacs, put this in your `config.el` file:

``` lisp
(defun kit-reset ()
  (interactive)
  (save-buffer) ; Save the current buffer
  (cider-interactive-eval
   "(require 'integrant.repl) (if (resolve 'integrant.repl/reset) (integrant.repl/reset) (println \"No integrant.repl/reset found.\"))"))

(defun kit-reset-on-save ()
  (add-hook 'after-save-hook 'kit-reset nil 'make-it-local))

(add-hook 'clojure-mode-hook 'kit-reset-on-save)
```

This will automatically `(reset)` the kit webserver on file save for anything with `clojure-mode` on.

#### Browser Reload
Install BrowserSync globally:

``` shell
npm install -g browser-sync
```

Then run a proxy recursively watching all files from project root. Make sure to run this from the project root directory:

``` shell
browser-sync start --proxy "localhost:3000" --files "**/*.clj"
```

If it works it should open on a proxy web page on a different port (usually 3001). 


#### All in one dev start script
It's not perfect but one command can start the dev environment.  

Install some requirements:

``` shell
npm install -g browser-sync
npm install -g concurrently
```

Copy `.secrets.sh.example` to `.secrets.sh` and set the values for each envvar.

``` shell
cd backend
./dev.sh
```

This will set some necessary environment variables, start up the repl with babashka, and wait 60 seconds to start browser-sync.  

In that 60 seconds you need to `cider-connect` to the repl that starts up and run `(go)` to start the server.


