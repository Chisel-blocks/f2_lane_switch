#!/bin/sh
DIR="$( cd "$( dirname $0 )" && pwd )"
git submodule update --init


SUBMODULES="\
    decouple_branch \
    " 
for module in $SUBMODULES; do
    cd ${DIR}/${module}
    if [ -f "./init_submodules.sh" ]; then
        ./init_submodules.sh
    fi
    sbt publishLocal
done

exit 0

