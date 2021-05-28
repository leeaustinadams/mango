#!/bin/bash

sed -e s/{{mongo-db-name}}/$DB_NAME/ -e s/{{mongo-db-user}}/$DB_PSSWORD/ -e s/{{mongo-db-password}}/$DB_PASSWORD/ mongo-init/mongo-init.js
