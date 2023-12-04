#!/usr/bin/env bash

sbt clean scalafmtAll scalastyleAll compile coverage test IntegrationTest/test coverageOff coverageReport dependencyUpdates