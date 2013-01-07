# compile sources
cd ..
sbt compile
cd -
# generate jar from the compiled sources
jar cvf blafxml-0.0.1-SNAPSHOT.jar -C ../target/scala-2.9.1/classes/  .
# generate jar for the sources
jar cvf blafxml-0.0.1-SNAPSHOT-sources.jar -C  ../src/main/scala/  .
# publish generated artifact on the local maven repository
mvn install:install-file -DgroupId=net.caoticode -DartifactId=blafxml -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar -Dsources=blafxml-0.0.1-SNAPSHOT-sources.jar -Dfile=blafxml-0.0.1-SNAPSHOT.jar
