#!/bin/bash

_SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"
_CONCAT_ARGS="$1"
for argz in "${@:2}"
do
  _CONCAT_ARGS+=" $argz"
done
_UNDERSCORED_NAME="${_CONCAT_ARGS// /_}"
_FILE="V`date +%Y%m%d%H%M%S`__$_UNDERSCORED_NAME.sql"
echo "--Migration on $(date)" > "$_SCRIPT_DIR/migration/$_FILE"
