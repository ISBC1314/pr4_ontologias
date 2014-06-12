#!/bin/sh

java -Xss4m -Xms30m -Xmx200m -jar lib/pellet.jar "$@"
