#!/bin/bash

if [ $1 ] ; then
    git checkout $1
fi
echo "Deploying " `lein v version`
lein with-profile prod uberjar && scp -i ~/.ssh/second-aws-key-pair.pem target/mango-*-standalone.jar ubuntu@4d4ms.com:
