#!/bin/bash
. ./.secrets.sh
# concurrently --names "cmd1,cmd2" --prefix-colors "bgBlue.bold,bgMagenta.bold" "command1" "command2"
concurrently --names "repl,browser-sync" --prefix-colors "bgBlue.bold,bgMagenta.bold" "bb cider" "sleep 60 && browser-sync start --proxy \"localhost:3000\" --files \"**/*.clj\""
