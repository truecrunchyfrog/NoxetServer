#!/bin/bash

source .env

curl -T $SOURCE -u $USER:$PASS $DESTINATION