#!/bin/bash

if [ $1 ] ; then
    echo "Deploying v$1"
    git checkout "v$1"
    lein build-prod-client
    lein build-prod-uberjar
    scp -i ~/.ssh/second-aws-key-pair.pem target/mango-$1-standalone.jar ubuntu@4d4ms.com:mango/
    ssh -i ~/.ssh/second-aws-key-pair.pem ubuntu@4d4ms.com "cd mango && ln -s -f ./mango/mango-$1-standalone.jar ./mango/mango-current-standalone.jar"
    ssh -i ~/.ssh/second-aws-key-pair.pem ubuntu@4d4ms.com sudo /etc/init.d/mango restart
    ssh -i ~/.ssh/second-aws-key-pair.pem ubuntu@4d4ms.com tail -f /var/log/mango.log&
    ssh -i ~/.ssh/second-aws-key-pair.pem ubuntu@4d4ms.com sleep 10
else
    echo "Specify a version number"
fi

