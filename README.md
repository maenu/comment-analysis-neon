Install dependencies and create shaded jar:

```
mvn package
```

Run simple smoke test, there should be an output.xml in the project folder.

```
java -jar target/cli-0.0.1-SNAPSHOT.jar src/test/resources/text.txt src/test/resources/heuristics.xml output.xml
```

https://github.com/adisorbo/NEON_tool

mvn dependency:tree

mvn install:install-file -DlocalRepositoryPath=./lib -DgroupId=org.neon -DartifactId=neon -Dversion=0.0.3
-Dfile=./neon-0.0.2.jar -Dpackaging=jar -DgeneratePom=false -DcreateChecksum=true
