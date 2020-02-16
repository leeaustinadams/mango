#!/bin/bash

# tunnel to production mongodb
ssh -f -L27017:localhost:27017 ubuntu@leeadams.dev -i ~/.ssh/second-aws-key-pair.pem -N
