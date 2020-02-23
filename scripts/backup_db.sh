#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

if [ $# -lt 3 ]
then
   echo "backup_db.sh <user-name> <db-name> <bucket-name>"
else
    username=$1
    db=$2
    bucket=$3
    name="$db.db.backup.$(date +%F)"
    mongodump -vv -u $username -d $db -o $name
    tar -zcvf "$name.tar.gz" $name
    aws s3 cp "$name.tar.gz" "s3://$bucket/"
fi

