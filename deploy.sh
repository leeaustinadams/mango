#!/bin/bash

if [ $1 ] ; then
    echo "Deploying '$1'"
    git checkout $1
    lein with-profile prod uberjar && scp -i ~/.ssh/second-aws-key-pair.pem target/mango-*-standalone.jar ubuntu@4d4ms.com:
else
    echo "Specify a tag or commit"
fi
