#!/bin/bash
_CONCAT_ARGS="$1"
for argz in "${@:2}"
do
  _CONCAT_ARGS+=" $argz"
done
_UNDERSCORED_NAME="${_CONCAT_ARGS// /_}"
_FILE="V`date +%Y%m%d%H%M%S`__$_UNDERSCORED_NAME.sql"
echo "--Migration on $(date)" > "`dirname $0`/migration/$_FILE"
