![OSS Review Toolkit Logo](./logos/ort.png)

&nbsp;

[![Slack][1]][2]

[![Linux build status][4]][3] [![Windows build status][5]][3] [![Docker build status][6]][3] [![JitPack build status][10]][11]

[![Linux analyzer tests][7]][3] [![Windows analyzer tests][8]][3] [![Code coverage][12]][13]

[![TODOs][14]][15] [![Static Analysis][9]][3] [![LGTM][16]][17] [![REUSE status][18]][19] [![CII][20]][21]

[1]: https://img.shields.io/badge/Join_us_on_Slack!-ort--talk-blue.svg?longCache=true&logo=slack
[2]: https://join.slack.com/t/ort-talk/shared_invite/enQtMzk3MDU5Njk0Njc1LThiNmJmMjc5YWUxZTU4OGI5NmY3YTFlZWM5YTliZmY5ODc0MGMyOWIwYmRiZWFmNGMzOWY2NzVhYTI0NTJkNmY
[3]: https://github.com/oss-review-toolkit/ort/actions/workflows/static-analysis.yml
[4]: https://dev.azure.com/oss-review-toolkit/ort/_apis/build/status/oss-review-toolkit.ort?branchName=master&jobName=LinuxTest&label=Linux%20Build
[5]: https://dev.azure.com/oss-review-toolkit/ort/_apis/build/status/oss-review-toolkit.ort?branchName=master&jobName=WindowsTest&label=Windows%20Build
[6]: https://dev.azure.com/oss-review-toolkit/ort/_apis/build/status/oss-review-toolkit.ort?branchName=master&jobName=DockerBuild&label=Docker%20Build
[7]: https://dev.azure.com/oss-review-toolkit/ort/_apis/build/status/oss-review-toolkit.ort?branchName=master&jobName=LinuxAnalyzerTest&label=Linux%20Analyzer%20Tests
[8]: https://dev.azure.com/oss-review-toolkit/ort/_apis/build/status/oss-review-toolkit.ort?branchName=master&jobName=WindowsAnalyzerTest&label=Windows%20Analyzer%20Tests
[9]: https://github.com/oss-review-toolkit/ort/actions/workflows/static-analysis.yml/badge.svg?branch=master
[10]: https://jitpack.io/v/oss-review-toolkit/ort.svg
[11]: https://jitpack.io/#oss-review-toolkit/ort
[12]: https://codecov.io/gh/oss-review-toolkit/ort/branch/master/graph/badge.svg?token=QD2tCSUTVN
[13]: https://app.codecov.io/gh/oss-review-toolkit/ort
[14]: https://badgen.net/https/api.tickgit.com/badgen/github.com/oss-review-toolkit/ort
[15]: https://www.tickgit.com/browse?repo=github.com/oss-review-toolkit/ort
[16]: https://img.shields.io/lgtm/alerts/g/oss-review-toolkit/ort.svg?logo=lgtm&logoWidth=18
[17]: https://lgtm.com/projects/g/oss-review-toolkit/ort/alerts/
[18]: https://api.reuse.software/badge/github.com/oss-review-toolkit/ort
[19]: https://api.reuse.software/info/github.com/oss-review-toolkit/ort
[20]: https://bestpractices.coreinfrastructure.org/projects/4618/badge
[21]: https://bestpractices.coreinfrastructure.org/projects/4618

# Introduction

The OSS Review Toolkit (ORT) aims to assist with the tasks that commonly need to be performed in the context of license
compliance checks, especially for (but not limited to) Free and Open Source Software dependencies.

It does so by orchestrating a _highly customizable_ pipeline of tools that _abstract away_ the underlying services.
These tools are implemented as libraries (for programmatic use) and exposed via a command line interface (for scripted
use):

* [_Analyzer_](#analyzer) - determines the dependencies of projects and their metadata, abstracting which package
  managers or build systems are actually being used.
* [_Downloader_](#downloader) - fetches all source code of the projects and their dependencies, abstracting which
  Version Control System (VCS) or other means are used to retrieve the source code.
* [_Scanner_](#scanner) - uses configured source code scanners to detect license / copyright findings, abstracting
  the type of scanner.
* [_Advisor_](#advisor) - retrieves security advisories for used dependencies from configured vulnerability data 
  services.
* [_Evaluator_](#evaluator) - evaluates license / copyright findings against customizable policy rules and license
  classifications.
* [_Reporter_](#reporter) - presents results in various formats such as visual reports, Open Source notices or
  Bill-Of-Materials (BOMs) to easily identify dependencies, licenses, copyrights or policy rule violations.

# Installation

## From binaries

Preliminary binary artifacts for ORT are currently available via
[JitPack](https://jitpack.io/#oss-review-toolkit/ort). Please note that due to limitations with the JitPack build
environment, the reporter is not able to create the Web App report.

## From sources

Install the following basic prerequisites:

* Git (any recent version will do).

Then clone this repository. If you intend to run tests, you need to clone with submodules by running
`git clone --recurse-submodules`. If you have already cloned non-recursively, you can initialize submodules afterwards
by running `git submodule update --init --recursive`.

### Build using Docker

Install the following basic prerequisites:

* Docker 18.09 or later (and ensure its daemon is running).
* Enable [BuildKit](https://docs.docker.com/develop/develop-images/build_enhancements/) for Docker.

Change into the directory with ORT's source code and run: 
```
docker build -t ort .
```

Tool versions can be changed passing as args like **--build-arg TOOL_ENV=VERSION**
This is the list of tools variables you can change:

| Tool Env Var | To obtain versions list |
| ------------ | ------------------------ |
ANDROID_SDK_VERSION |
BOWER_VERSION |
CONAN_VERSION |
GO_DEP_VERSION |
GO_VERSION |
HASKELL_STACK_VERSION |
NODEJS_VERSION | Any of **nvm ls-remote**
NPM_VERSION |
PIP_VERSION |
PYTHON_VERSION | Any of **pyenv install --list**
RUBY_VERSION | Any of **rbenv install --list-all**
SBT_VERSION |
SCANCODE_VERSION |
VIRTUALENV_VERSION |
YARN_VERSION |

Example:
```
--build-arg PYTHON_VERSION="3.9.1"
```

### Build natively

Install these additional prerequisites:

* Java Development Kit (JDK) version 11 or later; also remember to set the `JAVA_HOME` environment variable accordingly.

Change into the directory with ORT's source code and run `./gradlew installDist` (on the first run this will bootstrap
Gradle and download all required dependencies).

## Basic usage

ORT can now be run using

    ./cli/build/install/ort/bin/ort --help

Note that if you make any changes to ORT's source code, you would have to regenerate the distribution using the steps
above.

To avoid that, you can also build and run ORT in one go (if you have the prerequisites from the
[Build natively](#build-natively) section installed):

    ./gradlew cli:run --args="--help"

Note that in this case the working directory used by ORT is that of the `cli` project, not the directory `gradlew` is
located in (see https://github.com/gradle/gradle/issues/6074).

# Running the tools

Like for building ORT from sources you have the option to run ORT from a Docker image (which comes with all runtime
dependencies) or to run ORT natively (in which case some additional requirements need to be fulfilled).

## Run using Docker

After you have built the image as [described above](#build-using-docker), simply run
`docker run <DOCKER_ARGS> ort <ORT_ARGS>`. You typically use `<DOCKER_ARGS>` to mount the project directory to analyze
into the container for ORT to access it, like:

    docker run -v /workspace:/project ort --info analyze -f JSON -i /project -o /project/ort/analyzer

You can find further hints for using ORT with Docker in the [documentation](./docs/hints-for-use-with-docker.md).

## Run natively

First of all, make sure that the locale of your system is set to `en_US.UTF-8` as using other locales might lead to
issues with parsing the output of some external tools.

Then install any missing external command line tools as listed by

    ./cli/build/install/ort/bin/ort requirements

or

    ./gradlew cli:run --args="requirements"

Then run ORT like

    ./cli/build/install/ort/bin/ort --info analyze -f JSON -i /project -o /project/ort/analyzer

or

    ./gradlew cli:run --args="--info analyze -f JSON -i /project -o /project/ort/analyzer"

## Running on CI

A basic ORT pipeline (using the _analyzer_, _scanner_ and _reporter_) can easily be run on
[Jenkins CI](https://jenkins.io/) by using the [Jenkinsfile](./integrations/Jenkinsfile) in a (declarative)
[pipeline](https://jenkins.io/doc/book/pipeline/) job. Please see the [Jenkinsfile](./integrations/Jenkinsfile) itself
for documentation of the required Jenkins plugins. The job accepts various parameters that are translated to ORT command
line arguments. Additionally, one can trigger a downstream job which e.g. further processes scan results. Note that it
is the downstream job's responsibility to copy any artifacts it needs from the upstream job.

A demo instance of a Jenkins pipeline for ORT will soon be

![Fosshost Logo](./logos/fosshost.png)

## Getting started

Please see [Getting Started](./docs/getting-started.md) for an introduction to the individual tools.

## Configuration

### Environment variables

ORT supports several environment variables that influence its behavior:

| Name | Default value | Purpose |
| ---- | ------------- | ------- |
| ORT_DATA_DIR | `~/.ort` | All data, like caches, archives, storages (read & write) |
| ORT_CONFIG_DIR | `$ORT_DATA_DIR/config` | Configuration files, see below (read only) |
| ORT_HTTP_USERNAME | Empty (n/a) | Generic username to use for HTTP(S) downloads |
| ORT_HTTP_PASSWORD | Empty (n/a) | Generic password to use for HTTP(S) downloads |
| http_proxy | Empty (n/a) | Proxy to use for HTTP downloads |
| https_proxy | Empty (n/a) | Proxy to use for HTTPS downloads |

### Configuration files

ORT looks for its configuration files in the directory pointed to by the `ORT_CONFIG_DIR` environment variable. If this
variable is not set, it defaults to the `config` directory below the directory pointed to by the `ORT_DATA_DIR`
environment variable, which in turn defaults to the `.ort` directory below the current user's home directory.

The following provides an overview of the various configuration files that can be used to customize ORT behavior:

#### [ORT configuration file](./model/src/main/resources/reference.conf)

The main configuration file for the operation of ORT. This configuration is maintained by an administrator who manages
the ORT instance. In contrast to the configuration files in the following, this file rarely changes once ORT is
operational.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| HOCON | Global | `$ORT_CONFIG_DIR/ort.conf` | Empty |

The [reference configuration file](./model/src/main/resources/reference.conf) gives a good impression about the content
of the main ORT configuration file. It consists of sections related to different sub components of ORT. The meaning
of these sections and the properties they can contain is described together with the corresponding sub components.

While the file is rather static, there are means to override configuration options for a specific run of ORT or to
customize the configuration to a specific environment. The following options are supported, in order of precedence:

* Properties can be defined via environment variables by using the full property path as the variable name.
  For instance, one can override the Postgres schema by setting 
  `ort.scanner.storages.postgresStorage.schema=test_schema`. The variable's name is case sensitive.
  Some programs like Bash do not support dots in variable names. For this case, the dots can be
  replaced by double underscores, i.e., the above example is turned into 
  `ort__scanner__storages__postgresStorage__schema=test_schema`.
* In addition to that, one can override the values of properties on the command line using the `-P` option. The option expects a
  key-value pair. Again, the key must define the full path to the property to be overridden, e.g.
  `-P ort.scanner.storages.postgresStorage.schema=test_schema`. The `-P` option can be repeated on the command
  line to override multiple properties.
* Properties in the configuration file can reference environment variables using the syntax `${VAR}`.
  This is especially useful to reference dynamic or sensitive data. As an example, the credentials for the
  Postgres database used as scan results storage could be defined in the `POSTGRES_USERNAME` and `POSTGRES_PASSWORD`
  environment variables. The configuration file can then reference these values as follows:

  ```hocon
  postgres {
    url = "jdbc:postgresql://your-postgresql-server:5444/your-database"
    username = ${POSTGRES_USERNAME}
    password = ${POSTGRES_PASSWORD}
  }
  ```

#### [Copyright garbage file](./docs/config-file-copyright-garbage-yml.md)

A list of copyright statements that are considered garbage, for example statements that were incorrectly classified as
copyrights by the scanner.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| YAML / JSON | Global | `$ORT_CONFIG_DIR/copyright-garbage.yml` | Empty (n/a) |

#### [Curations file](./docs/config-file-curations-yml.md)

A file to correct invalid or missing package metadata, and to set the concluded license for packages.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| YAML / JSON | Global | `$ORT_CONFIG_DIR/curations.yml` | Empty (n/a) |

#### [Custom license texts dir](./docs/dir-custom-license-texts.md)

A directory that contains license texts which are not provided by ORT.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| Text | Global | `$ORT_CONFIG_DIR/custom-license-texts/` | Empty (n/a) |

#### [How to fix text provider script](./docs/how-to-fix-text-provider-kts.md)

A Kotlin script that enables the injection of how-to-fix texts in markdown format for ORT issues into the reports.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| Kotlin script | Global | `$ORT_CONFIG_DIR/how-to-fix-text-provider.kts` | Empty (n/a) |

#### [License classifications file](docs/config-file-license-classifications-yml.md)

A file that contains user-defined categorization of licenses.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| YAML / JSON | Global | `$ORT_CONFIG_DIR/license-classifications.yml` | Empty (n/a) |

#### [Resolution file](./docs/config-file-resolutions-yml.md)

Configurations to resolve any issues or rule violations by providing a mandatory reason, and an optional comment to
justify the resolution on a global scale.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| YAML / JSON | Global | `$ORT_CONFIG_DIR/resolutions.yml` | Empty (n/a) |

#### [Repository configuration file](./docs/config-file-ort-yml.md)

A configuration file, usually stored in the project's repository, for license finding curations, exclusions, and issues
or rule violations resolutions in the context of the repository.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| YAML / JSON | Repository (project) | `[analyzer-input-dir]/.ort.yml` | Empty (n/a) |

#### [Package configuration file / directory](./docs/config-file-package-configuration-yml.md)

A single file or a directory with multiple files containing configurations to set provenance-specific path excludes and
license finding curations for dependency packages to address issues found within a scan result. `helper-cli`'s
[`package-config create` command](./helper-cli/src/main/kotlin/commands/packageconfig/CreateCommand.kt)
can be used to populate a directory with template package configuration files.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| YAML / JSON | Package (dependency) | `$ORT_CONFIG_DIR/package-configurations/` | Empty (n/a) |

#### [Policy rules file](./docs/file-rules-kts.md)

The file containing any policy rule implementations to be used with the _evaluator_.

| Format | Scope | Default location | Default value |
| ------ | ----- | ---------------- | ------------- |
| Kotlin script (DSL) | Evaluator | `$ORT_CONFIG_DIR/rules.kts` | Empty (n/a) |

# Details on the tools

<a name="analyzer"></a>

[![Analyzer](./logos/analyzer.png)](./analyzer/src/main/kotlin)

The _analyzer_ is a Software Composition Analysis (SCA) tool that determines the dependencies of software projects
inside the specified input directory (`-i`). It does so by querying the detected package managers; **no modifications**
to your existing project source code, like applying build system plugins, are necessary for that to work. The tree of
transitive dependencies per project is written out as part of an
[OrtResult](https://github.com/oss-review-toolkit/ort/blob/master/model/src/main/kotlin/OrtResult.kt) in YAML (or
JSON, see `-f`) format to a file named `analyzer-result.yml` in the specified output directory (`-o`). The output file
exactly documents the status quo of all package-related metadata. It can be further processed or manually edited before
passing it to one of the other tools.

Currently, the following package managers (grouped by the programming language they are most commonly used with) are
supported:

* C / C++
  * [Conan](https://conan.io/) (limitations:
  [receipe vs. source repository](https://github.com/oss-review-toolkit/ort/issues/2037))
  * Also see: [SPDX documents](#analyzer-for-spdx-documents)
* Dart / Flutter
  * [Pub](https://pub.dev/)
* Go
  * [dep](https://golang.github.io/dep/)
  * [Glide](https://github.com/Masterminds/glide)
  * [Godep](https://github.com/tools/godep)
  * [GoMod](https://github.com/golang/go/wiki/Modules) (limitations:
  [no `replace` directive](https://github.com/oss-review-toolkit/ort/issues/4445))
* Haskell
  * [Stack](https://haskellstack.org/)
* Java
  * [Gradle](https://gradle.org/)
  * [Maven](https://maven.apache.org/) (limitations:
  [default profile only](https://github.com/oss-review-toolkit/ort/issues/1774))
* JavaScript / Node.js
  * [Bower](https://bower.io/)
  * [NPM](https://www.npmjs.com/) (limitations:
  [no scope-specific registries](https://github.com/oss-review-toolkit/ort/issues/3741),
  [no peer dependencies](https://github.com/oss-review-toolkit/ort/issues/95))
  * [Yarn](https://yarnpkg.com/) (limitations:
  [no Yarn 2 / 3 support](https://github.com/oss-review-toolkit/ort/issues/2283))
* .NET
  * [DotNet](https://docs.microsoft.com/en-us/dotnet/core/tools/) (limitations:
  [no floating versions / ranges](https://github.com/oss-review-toolkit/ort/pull/1303#issue-253860146),
  [no target framework](https://github.com/oss-review-toolkit/ort/issues/4083))
  * [NuGet](https://www.nuget.org/) (limitations:
  [no floating versions / ranges](https://github.com/oss-review-toolkit/ort/pull/1303#issue-253860146),
  [no target framework](https://github.com/oss-review-toolkit/ort/issues/4083))
* Objective-C / Swift
  * [Carthage](https://github.com/Carthage/Carthage) (limitation:
  [no `cartfile.private`](https://github.com/oss-review-toolkit/ort/issues/3774))
  * [CocoaPods](https://github.com/CocoaPods/CocoaPods) (limitations:
  [no custom source repositories](https://github.com/oss-review-toolkit/ort/issues/4188))
* PHP
  * [Composer](https://getcomposer.org/)
* Python
  * [PIP](https://pip.pypa.io/) (limitations:
  [Python 2.7 or 3.6 and PIP 18.1 only](https://github.com/oss-review-toolkit/ort/issues/3671))
  * [Pipenv](https://pipenv.readthedocs.io/) (limitations:
  [Python 2.7 or 3.6 and PIP 18.1 only](https://github.com/oss-review-toolkit/ort/issues/3671))
* Ruby
  * [Bundler](https://bundler.io/) (limitations:
  [restricted to the version available on the host](https://github.com/oss-review-toolkit/ort/issues/1308))
* Rust
  * [Cargo](https://doc.rust-lang.org/cargo/)
* Scala
  * [SBT](https://www.scala-sbt.org/)

<a name="analyzer-for-spdx-documents"></a>

If another package manager that is not part of the list above is used (or no package manager at all), the generic
fallback to [SPDX documents](https://spdx.dev/specifications/) can be leveraged to describe
[projects](./analyzer/src/funTest/assets/projects/synthetic/spdx/project/project.spdx.yml) or
[packages](./analyzer/src/funTest/assets/projects/synthetic/spdx/package/libs/curl/package.spdx.yml).

<a name="downloader">&nbsp;</a>

[![Downloader](./logos/downloader.png)](./downloader/src/main/kotlin)

Taking an ORT result file with an _analyzer_ result as the input (`-i`), the _downloader_ retrieves the source code of
all contained packages to the specified output directory (`-o`). The _downloader_ takes care of things like normalizing
URLs and using the [appropriate VCS tool](./downloader/src/main/kotlin/vcs) to checkout source code from version
control.

Currently, the following Version Control Systems (VCS) are supported:

* [CVS](https://en.wikipedia.org/wiki/Concurrent_Versions_System)
* [Git](https://git-scm.com/)
* [Git-Repo](https://source.android.com/setup/develop/repo)
* [Mercurial](https://www.mercurial-scm.org/)
* [Subversion](https://subversion.apache.org/)

<a name="scanner">&nbsp;</a>

[![Scanner](./logos/scanner.png)](./scanner/src/main/kotlin)

This tool wraps underlying license / copyright scanners with a common API so all supported scanners can be used in the
same way to easily run them and compare their results. If passed an ORT result file with an analyzer result (`-i`), the
_scanner_ will automatically download the sources of the dependencies via the _downloader_ and scan them afterwards.

We recommend to use ORT with one of the following scanners as their integration has been thoroughly tested (in
alphabetical order):

* [FossID](https://fossid.com/)
* [ScanCode](https://github.com/nexB/scancode-toolkit)

Additionally, the following reference implementations exist (in alphabetical order):

* [Askalono](https://github.com/amzn/askalono)
* [lc](https://github.com/boyter/lc)
* [Licensee](https://github.com/benbalter/licensee)

For a comparison of some of these, see this
[Bachelor Thesis](https://osr.cs.fau.de/2019/08/07/final-thesis-a-comparison-study-of-open-source-license-crawler/).

## Storage Backends

In order to not download or scan any previously scanned sources again, or to reuse scan results generated via other
services, the _scanner_ can be configured to use so-called storage backends. Before processing a package, it checks
whether compatible scan results are already available in one of the storages declared; if this is the case, they
are fetched and reused. Otherwise, the package's source code is downloaded and scanned. Afterwards, the new scan
results can be put into a storage for later reuse.

It is possible to configure multiple storages to read scan results from or to write scan results to. For reading,
the declaration order in the configuration is important, as the scanner queries the storages in this order and uses
the first matching result. This allows a fine-grained control over the sources, from which existing scan results are
loaded. For instance, you can specify that the scanner checks first whether results for a specific package are
available in a local storage on the file system. If this is not the case, it can look up the package in a Postgres
database. If this does not yield any results either, a service like [ClearlyDefined](https://clearlydefined.io) can be
queried. Only if all of these steps fail, the scanner has to actually process the package.

When storing a newly generated scan result the scanner invokes all the storages declared as writers. The storage
operation is considered successful if all writer storages could successfully persist the scan result.

The configuration of storage backends is located in the [ORT configuration file](#ort-configuration-file). (For the
general structure of this file and the set of options available refer to the
[reference configuration](./model/src/main/resources/reference.conf).) The file has a section named _storages_ that lists
all the storage backends and assigns them a name. Each storage backend is of a specific type and needs to be configured
with type-specific properties. The different types of storage backends supported by ORT are described below.

After the declaration of the storage backends, the configuration file has to specify which ones of them the
scanner should use for looking up existing scan results or to store new results. This is done in two list properties
named _storageReaders_ and _storageWriters_. The lists reference the names of the storage backends declared in the
_storages_ section. The scanner invokes the storage backends in the order they appear in the lists; so for readers,
this defines a priority for look-up operations. Each storage backend can act as a reader; however, some types do not
support updates and thus cannot serve as writers. If a storage backend is referenced both as reader and writer, the
scanner creates only a single instance of this storage class.

The following subsections describe the different storage backend implementations supported by ORT. Note that the name of
a storage entry (like `fileBasedStorage`) can be freely chosen. That name is then used to refer to the storage from the
`storageReaders` and `storageWriters` sections.

### Local File Storage

By default, the _scanner_ stores scan results on the local file system in the current user's home directory (i.e.
`~/.ort/scanner/scan-results`) for later reuse. Settings like the storage directory and the compression flag can be
customized in the ORT configuration file (`-c`) with a respective storage configuration:

```hocon
ort {
  scanner {
    storages {
      fileBasedStorage {
        backend {
          localFileStorage {
            directory = "/tmp/ort/scan-results"
            compression = false
          }
        }
      }
    }

    storageReaders: [
      "fileBasedStorage"
    ]

    storageWriters: [
      "fileBasedStorage"
    ]
  }
}
```

### HTTP Storage

Any HTTP file server can be used to store scan results. Custom headers can be configured to provide authentication
credentials. For example, to use Artifactory to store scan results, use the following configuration:

```hocon
ort {
  scanner {
    storages {
      artifactoryStorage {
        backend {
          httpFileStorage {
            url = "https://artifactory.domain.com/artifactory/repository/scan-results"
            headers {
              X-JFrog-Art-Api = "api-token"
            }
          }
        }
      }
    }

    storageReaders: [
      "artifactoryStorage"
    ]

    storageWriters: [
      "artifactoryStorage"
    ]
  }
}
```

### PostgreSQL Storage

To use PostgreSQL for storing scan results you need at least version 9.4, create a database with the `client_encoding`
set to `UTF8`, and a configuration like the following:

```hocon
ort {
  scanner {
    storages {
      postgresStorage {
        url = "jdbc:postgresql://example.com:5444/database"
        schema = "public"
        username = "username"
        password = "password"
        sslmode = "verify-full"
      }
    }

    storageReaders: [
      "postgresStorage"
    ]

    storageWriters: [
      "postgresStorage"
    ]
  }
}
```

The database needs to exist. If the schema is set to something else than the default of `public`, it needs to exist and be accessible by the configured username.

The _scanner_ will itself create a table called `scan_results` and
store the data in a [jsonb](https://www.postgresql.org/docs/current/datatype-json.html) column.

If you do not want to use SSL set the `sslmode` to `disable`, other possible values are explained in the
[documentation](https://jdbc.postgresql.org/documentation/head/ssl-client.html). For other supported configuration
options see [ScanStorageConfiguration.kt](./model/src/main/kotlin/config/ScanStorageConfiguration.kt).

### ClearlyDefined Storage

[ClearlyDefined](https://clearlydefined.io) is a service offering curated metadata for Open Source components. This
includes scan results that can be used by ORT's _scanner_ tool (if they have been generated by a compatible scanner
version with a suitable configuration). This storage backend queries the ClearlyDefined service for scan results of the
packages to be processed. It is read-only; so it will not upload any new scan results to ClearlyDefined. In the
configuration the URL of the ClearlyDefined service needs to be set:

```hocon
ort {
  scanner {
    storages {
      clearlyDefined {
        serverUrl = "https://api.clearlydefined.io"
      }
    }

    storageReaders: [
      "clearlyDefined"
    ]
  }
}
```

<a name="advisor">&nbsp;</a>

[![Advisor](./logos/advisor.png)](./advisor/src/main/kotlin)

The _advisor_ retrieves security advisories from configured services. It requires the analyzer result as an input. For
all the packages identified by the analyzer, it queries the services configured for known security vulnerabilities. The
vulnerabilities returned by these services are then stored in the output result file together with additional
information like the source of the data and a severity (if available).

Multiple providers for security advisories are available. The providers require specific configuration in the
[ORT configuration file](./model/src/main/resources/reference.conf), which needs to be placed in the _advisor_
section. When executing the advisor the providers to enable are selected with the `--advisors` option (or its short
alias `-a`); here a comma-separated list with provider IDs is expected. The following sections describe the providers
supported by the advisor:

## NexusIQ

A security data provider that queries [Nexus IQ Server](https://help.sonatype.com/iqserver). In the configuration,
the URL where Nexus IQ Server is running and the credentials to authenticate need to be provided:

```hocon
ort {
  advisor {
    nexusIq {
      serverUrl = "https://nexusiq.ossreviewtoolkit.org"
      username = myUser
      password = myPassword
    }
  }
}
```

To enable this provider, pass `-a NexusIQ` on the command line.

## OSS Index

This vulnerability provider does not require any further configuration as it uses the public service at
https://ossindex.sonatype.org/. Before using this provider, please ensure to comply with its
[Terms of Service](https://ossindex.sonatype.org/tos).

To enable this provider, pass `-a OssIndex` on the command line.

## VulnerableCode

This provider obtains information about security vulnerabilities from a
[VulnerableCode](https://github.com/nexB/vulnerablecode) instance. The configuration is limited to the server URL, as
authentication is not required:

```hocon
ort {
  advisor {
    vulnerableCode {
      serverUrl = "http://localhost:8000"
    }
  }
}
```

To enable this provider, pass `-a VulnerableCode` on the command line.

<a name="evaluator">&nbsp;</a>

[![Evaluator](./logos/evaluator.png)](./evaluator/src/main/kotlin)

The _evaluator_ is used to perform custom license policy checks on scan results. The rules to check against are
implemented as scripts (currently Kotlin scripts, with a dedicated DSL, but support for other scripting can be added as
well. See [rules.kts](./examples/rules.kts) for an example file.

<a name="reporter">&nbsp;</a>

[![Reporter](./logos/reporter.png)](./reporter/src/main/kotlin)

The _reporter_ generates a wide variety of documents in different formats from ORT result files. Currently, the
following formats are supported (reporter names are case-insensitive):

* [AsciiDoc Template](docs/reporters/AsciiDocTemplateReporter.md) (`-f AsciiDocTemplate`)
  * Content customizable with [Apache Freemarker](https://freemarker.apache.org/) templates and [AsciiDoc](https://asciidoc.org/)
  * PDF style customizable with Asciidoctor [PDF themes](https://github.com/asciidoctor/asciidoctor-pdf/blob/master/docs/theming-guide.adoc)
  * Supports multiple AsciiDoc backends:
    * PDF (`-f PdfTemplate`)
    * HTML (`-f HtmlTemplate`)
    * XHTML (`-f XHtmlTemplate`)
    * DocBook (`-f DocBookTemplate`)
    * Man page (`-f ManPageTemplate`)
    * AsciiDoc (`-f AdocTemplate`): Does not convert the created AsciiDoc files but writes the generated files as
      reports.
* [CycloneDX](https://cyclonedx.org/) BOM (`-f CycloneDx`)
* [Excel](https://products.office.com/excel) sheet (`-f Excel`)
* [GitLabLicenseModel](https://docs.gitlab.com/ee/ci/pipelines/job_artifacts.html#artifactsreportslicense_scanning-ultimate) (`-f GitLabLicenseModel`)
  * A nice tutorial video has been [published](https://youtu.be/dNmH_kYJ34g) by GitLab engineer @mokhan.
* [NOTICE](https://infra.apache.org/licensing-howto.html) file in two variants
  * List license texts and copyrights by package (`-f NoticeTemplate`)
  * Summarize all license texts and copyrights (`-f NoticeTemplate -O NoticeTemplate=template.id=summary`)
  * Customizable with [Apache Freemarker](https://freemarker.apache.org/) templates
* Opossum input that can be visualized and edited in the [OpossumUI](https://github.com/opossum-tool/opossumUI)  (`-f Opossum`)
* [SPDX Document](https://spdx.dev/specifications/), version 2.2 (`-f SpdxDocument`)
* Static HTML (`-f StaticHtml`)
* Web App (`-f WebApp`)

# System requirements

ORT is being continuously used on Linux, Windows and macOS by the
[core development team](https://github.com/orgs/oss-review-toolkit/people), so these operating systems are
considered to be well supported.

To run the ORT binaries (also see [Installation from binaries](#from-binaries)) at least Java 11 is required. Memory and
CPU requirements vary depending on the size and type of project(s) to analyze / scan, but the general recommendation is
to configure Java with 8 GiB of memory (`-Xmx=8g`) and to use a CPU with at least 4 cores.

If ORT requires external tools in order to analyze a project, these tools are listed by the `ort requirements` command.
If a package manager is not list listed there, support for it is integrated directly into ORT and does not require any
external tools to be installed.

# Development

ORT is written in [Kotlin](https://kotlinlang.org/) and uses [Gradle](https://gradle.org/) as the build system, with
[Kotlin script](https://docs.gradle.org/current/userguide/kotlin_dsl.html) instead of Groovy as the DSL.

When developing on the command line, use the committed
[Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) to bootstrap Gradle in the configured
version and execute any given tasks. The most important tasks for this project are:

| Task        | Purpose                                                           |
| ----------- | ----------------------------------------------------------------- |
| assemble    | Build the JAR artifacts for all projects                          |
| detekt      | Run static code analysis on all projects                          |
| test        | Run unit tests for all projects                                   |
| funTest     | Run functional tests for all projects                             |
| installDist | Build all projects and install the start scripts for distribution |

All contributions need to pass the `detekt`, `test` and `funTest` checks before they can be merged.

For IDE development we recommend the [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) which
can directly import the Gradle build files. After cloning the project's source code recursively, simply run IDEA and use
the following steps to import the project.

1. From the wizard dialog: Select *Import Project*.

   From a running IDEA instance: Select *File* -> *New* -> *Project from Existing Sources...*

2. Browse to ORT's source code directory and select either the `build.gradle.kts` or the `settings.gradle.kts` file.

3. In the *Import Project from Gradle* dialog select *Use auto-import* and leave all other settings at their defaults.

## Debugging

To set up a basic run configuration for debugging, navigate to `OrtMain.kt` in the `cli` module and look for the
`fun main(args: Array<String>)` function. In the gutter next to it, a green "Play" icon should be displayed. Click on it
and select `Run 'OrtMainKt'` to run the entry point, which implicitly creates a run configuration. Double-check that
running ORT without any arguments will simply show the command line help in IDEA's *Run* tool window. Finally, edit the
created run configuration to your needs, e.g. by adding an argument and options to run a specific ORT sub-command.

## Testing

For running tests and individual test cases from the IDE, the [kotest plugin](https://plugins.jetbrains.com/plugin/14080-kotest)
needs to be installed. Afterwards tests can be run via the green "Play" icon from the gutter as described above.

# License

Copyright (C) 2017-2021 HERE Europe B.V.\
Copyright (C) 2019-2020 Bosch Software Innovations GmbH\
Copyright (C) 2020-2021 Bosch.IO GmbH

See the [LICENSE](./LICENSE) file in the root of this project for license details.

OSS Review Toolkit (ORT) is a [Linux Foundation project](https://www.linuxfoundation.org) and part of [ACT](https://automatecompliance.org/).
