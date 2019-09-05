[![CircleCI](https://circleci.com/gh/volkodavs/ossrh-demo.svg?style=svg)](https://circleci.com/gh/volkodavs/ossrh-demo)


# Multi module maven central deployment 

## Steps 

### Sign up for Sonatype’s JIRA
https://issues.sonatype.org/secure/Signup!default.jspa

### Create a New Project ticket

Example: https://issues.sonatype.org/browse/OSSRH-51322

### Configure POM xml 

```xml 

```

### Generate gpg key 

* brew install gpg
* gpg --gen-key
* gpg --keyserver hkp://ipv4.pool.sks-keyservers.net --send-keys XXXX

### Configure maven settings 

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<settings xsi:schemaLocation='http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd'
          xmlns='http://maven.apache.org/SETTINGS/1.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>

    <servers>
        <server>
            <id>ossrh</id>
            <username>${env.username}</username>
            <password>${env.OSS_PASSWORD}</password>
        </server>
    </servers>

    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg2</gpg.executable>
                <gpg.passphrase>${env.PASSPHRASE}</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
</settings>
```

### Configure CI 

```yml

version: 2.1
jobs:
  build-java8:
    docker:
      - image: circleci/openjdk:8-jdk
    working_directory: ~/nexus-demo

    environment:
      MAVEN_OPTS: -Xmx3200m

    steps:
      - checkout
      - restore_cache:
          key: nexus-demo-{{ checksum "pom.xml" }}
      - run: mvn --fail-never dependency:go-offline || true


      - save_cache:
          paths:
            - ~/.m2
          key: nexus-demo-{{ checksum "pom.xml" }}

      - run:
          name: Deploy Nexus Demo on Java 8
          command: |
            mvn -s settings.xml clean deploy

workflows:
  version: 2
  build_and_test:
    jobs:
      - build-java8
```
