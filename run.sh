#!/bin/bash
sm2 --stop AGENT_ACCESS_CONTROL
sm2 --start AGENT_ACCESS_CONTROL -r 0.81.0
sbt run
