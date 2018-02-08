#!/bin/bash
set -e

description="$1"
shift 1
cmd="$@"

until java -classpath /waitForOracle/ojdbc7-12.1.0.1.jar:/waitForOracle WaitForOracle; do
    echo "$description is unavailable - sleeping"
    sleep 10
done

>&2 echo "$description is up - executing command"
exec $cmd
