git pull
./clean.sh
sbt -Dsbt.override.build.repos=true  docker:publish