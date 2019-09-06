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

#### [Create a New Project ticket](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134)

For github group id `io.github.YOUR_GIT_HUB_NAME` you might be asked to create a repository with the name of the ticket to validate that this is your account. 
Example: https://issues.sonatype.org/browse/OSSRH-51322

read more: https://central.sonatype.org/pages/choosing-your-coordinates.html

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
                <version>1.6</version>
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
                  <version>1.6.8</version>
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

* Make sure you have GPG installed. Run on MacOS `brew install gpg`
* Generate GPG key, you will be asked for passphrase(you should note it down) `gpg --gen-key`
* Distribute your key so that everyone `gpg --keyserver hkp://ipv4.pool.sks-keyservers.net --send-keys YOUR_GPG_KEY`

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

### 5. Configure Circleci

#### Setup circleci configuration 

Create a file `.circleci/config.yml`

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

#### Setup environmant variables 

Configure environment variables in circleci https://circleci.com/docs/2.0/env-vars/#setting-an-environment-variable-in-a-project

* `OSS_PASSWORD` sonatype username 
* `USERNAME` sonatype password 
* `PASSPHRASE` GPG passphrase

### 6. Automate GitHub Releases with CircleCI

#### Setup circleci configuration
If you want to setup github auto-deploy, you should extend  configuration file `.circleci/config.yml` with: 

```yml 
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

#### Setup environmant variables 

You should add environment variables to circleci

* `CIRCLE_PROJECT_REPONAME` - project name, for us it's `ossrh-demo`
* `CIRCLE_PROJECT_USERNAME` - project username, for us it's `volkodavs`
* `GITHUB_TOKEN` - you should generate personal access token, [read more](https://help.github.com/en/articles/creating-a-personal-access-token-for-the-command-line), with permission: `read:packages, repo, write:packages`

### 7. Summary

Hopefully this guide saves you some of the troubles we’ve encountered while setting up our integration with the Maven Central Repository!
