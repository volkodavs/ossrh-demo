[![CircleCI](https://circleci.com/gh/volkodavs/ossrh-demo.svg?style=svg)](https://circleci.com/gh/volkodavs/ossrh-demo)


# Multi module maven central deployment 

## Steps 

### 1. Create a New Project ticket

#### a. Sign up for Sonatype’s JIRA
Sonatype OSSRH (Open Source Repository Hosting) is a Maven repository that can easily sync with the Maven Central Repository.

**[Sing up](https://issues.sonatype.org/secure/Signup!default.jspa)**

**Recommendation:** you should register with a company account

#### b. Create a New Project ticket

Example: https://issues.sonatype.org/browse/OSSRH-51322
<img width="656" alt="jira tiket" src="https://user-images.githubusercontent.com/4140597/64367818-d69a4780-d010-11e9-946e-c52396f1cd29.png">


### 2. Configure POM xml 

```xml 

```

### 3. Generate gpg key 

* brew install gpg
* gpg --gen-key
* gpg --keyserver hkp://ipv4.pool.sks-keyservers.net --send-keys XXXX

### 4. Configure maven settings 

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<settings xsi:schemaLocation='http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd'
          xmlns='http://maven.apache.org/SETTINGS/1.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>

    <servers>
        <server>
            <id>ossrh</id>
            <username>${env.USERNAME}</username>
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

### 5. Configure CI 

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

### 6. Setup auto-deploy to github release

```java 
 publish-github-release:
    docker:
      - image: circleci/golang:1.13
    steps:
      - checkout
      - run:
          name: "Publish Release on GitHub"
          command: |
            go get github.com/tcnksm/ghr
            export VERSION=$(cat ~/project/pom.xml | grep "^    <version>.*</version>$" | awk -F'[><]' '{print $3}')
            ghr -t ${GITHUB_TOKEN} -u ${CIRCLE_PROJECT_USERNAME} -r ${CIRCLE_PROJECT_REPONAME} -c ${CIRCLE_SHA1} -delete ${VERSION} ~/project

```
### 7. Summary

