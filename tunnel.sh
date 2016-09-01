#!/bin/bash

# tunnel to production mongodb
ssh -f -L27017:localhost:27017 ubuntu@ec2-184-169-172-57.us-west-1.compute.amazonaws.com -i ~/.ssh/4d4ms.keys -N
