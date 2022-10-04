#!/usr/bin/env bash

set -eo pipefail
set -vx

version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
tag=$(git rev-parse --short HEAD)
name=metrist-java-agent
bucket=s3://dist.metrist.io/orchestrator-plugins/java/

fullname=$name-$version-$tag

mvn clean package
cd target
tar -zcvf $fullname.tar.gz metrist-java-agent-*.jar
gpg --sign --armor --detach-sign $fullname.tar.gz
echo $fullname > latest.txt

aws s3 cp $fullname.tar.gz $bucket
aws s3 cp $fullname.tar.gz.asc $bucket
aws s3 cp latest.txt $bucket
