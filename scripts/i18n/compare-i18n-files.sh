#!/bin/bash


# ----------------------------------
# Colors
# ----------------------------------
NOCOLOR='\033[0m'
RED='\033[0;31m'
GREEN='\033[0;32m'
ORANGE='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
LIGHTGRAY='\033[0;37m'
DARKGRAY='\033[1;30m'
LIGHTRED='\033[1;31m'
LIGHTGREEN='\033[1;32m'
YELLOW='\033[1;33m'
LIGHTBLUE='\033[1;34m'
LIGHTPURPLE='\033[1;35m'
LIGHTCYAN='\033[1;36m'
WHITE='\033[1;37m'

# Is target language selected?
if [ -z "$1" ]; then
   echo "Usage:   compare-i18n-files.sh <target-language>"
   echo "Example: compare-i18n-files.sh es"; 
   return 1; exit 1
fi

# Find english sources
find ../../modules/ -path \*src/\*/i18n/en.json | while read en; do
    lang=$1
    target=${en/en.json/$lang.json}
    echo -e "${LIGHTBLUE}Processing file '$target'${YELLOW}"
    diff -w <(sed -E 's/:[ ]*".*"/:""/' $en) <(sed -E 's/:[ ]*".*"/:""/' $target)
    if [ $? -ne 0 ]; then
      echo -e "${RED}Files $lang.json and en.json have different keys.${NOCOLOR}"
    else
      echo -e "${GREEN}Files $lang.json and en.json have the same keys.${NOCOLOR}"
    fi
    echo
done
