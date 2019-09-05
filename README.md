[![CircleCI](https://circleci.com/gh/volkodavs/ossrh-demo.svg?style=svg)](https://circleci.com/gh/volkodavs/ossrh-demo)


# Multi module maven central deployment 

## High level overview

* Create a Sonatype OSSRH account
* Ensure your library is properly configured
* Configure CI

## Steps 

### 1. Create a New Project ticket

#### [Sign up for Sonatype’s JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
Sonatype OSSRH (Open Source Repository Hosting) is a Maven repository that can easily sync with the Maven Central Repository.

**Recommendation:** you should register with a company account

#### [Create a New Project ticket](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21)

Example: https://issues.sonatype.org/browse/OSSRH-51322

### 2. Configure POM xml 

```xml 
...
<name>${project.groupId}:${project.artifactId}</name>
<description>Your artifact description</description>
<url>Link to your page or GitHub</url>
<licenses>
  <license>
    <name>MIT License</name>
    <url>http://www.opensource.org/licenses/mit-license.php</url>
  </license>
</licenses>
<developers>
   <developer>
     <name>Your name</name>
     <email>Your email</email>
     <organization>Your company</organization>
     <organizationUrl>Your company's website</organizationUrl>
   </developer>
 </developers>
 <scm>
   <url>https://github.com/yourcompany/yourrepo</url>
   <connection>scm:git:git@github.com:yourcompany/yourrepo.git</connection>
   <developerConnection>scm:git:git@github.com:yourcompany/yourrepo.git</developerConnection>
 </scm>
...
```

We also need add some maven plugins 

```xml
...
<build>
  <plugins>
    ...
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-source-plugin</artifactId>
        <version>3.0.1</version>
        <executions>
            <execution>
                <id>attach-sources</id>
                <goals>
                    <goal>jar</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    ...
  </plugins>
</build>
...
<profiles>
  <profile>
    <id>release</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-javadoc-plugin</artifactId>
                <version>2.10.4</version>
                <executions>
                    <execution>
                        <id>attach-javadocs</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>1.5</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>verify</phase>
                          <goals>
                              <goal>sign</goal>
                          </goals>
                      </execution>
                  </executions>
              </plugin>
              <plugin>
                  <groupId>org.sonatype.plugins</groupId>
                  <artifactId>nexus-staging-maven-plugin</artifactId>
                  <version>1.6.7</version>
                  <extensions>true</extensions>
                  <configuration>
                      <serverId>ossrh</serverId>
                      <nexusUrl>https://oss.sonatype.org/</nexusUrl>
                      <autoReleaseAfterClose>true</autoReleaseAfterClose>
                  </configuration>
              </plugin>
          </plugins>
      </build>
  </profile>
</profiles>
...
```

### 3. Generate gpg key 

* Make sure you have GPG installed
`brew install gpg`

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

