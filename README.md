## Build with Maven

Install dependencies and create shaded jar:

```
mvn package
```

## Using this as a dependency

Requires neon to be installed in a repository where it can be found from other projects:

```
mvn install:install-file -DgroupId=org.neon -DartifactId=neon -Dversion=0.0.3 -Dfile=lib/org/neon/neon/0.0.3/neon-0.0.3.jar -Dpackaging=jar -DgeneratePom=false -DcreateChecksum=true
```

Then run install:

```
mvn install
```

## NEON sources

https://github.com/adisorbo/NEON_tool

## Install neon as local dependency

```
mvn install:install-file -DlocalRepositoryPath=./lib -DgroupId=org.neon -DartifactId=neon -Dversion=0.0.3 -Dfile=./neon-0.0.3.jar -Dpackaging=jar -DgeneratePom=false -DcreateChecksum=true
```