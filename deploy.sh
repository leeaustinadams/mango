#!/bin/bash

if [ $1 ] ; then
    echo "Deploying '$1'"
    git checkout $1
    lein uberjar && scp target/mango-*-standalone.jar 4d4ms:
else
    echo "Specify a tag or commit"
fi
