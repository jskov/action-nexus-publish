# action-maven-publish

This action will sign Maven artifacts and publish them to MavenCentral.

**IT IS NOT READY YET**

## Simple and Transparent

The action does the same as the publishing plugins of Gradle and Maven (or near enough).
But it does so in very little java code that any java programmer should be able to verify the logic of.

When using a plugin in Gradle or Maven, all other plugins (and potentially any dependency jar used for compilation) will have access to your GPG signing key and your credentials for MavenCentral.

Depending on your level of paranoia this may be OK. Or it could be a problem.

For me it is a problem, hence this action.

### Runtime

The action runtime is built just using `javac` without any dependencies.
This is done as the action runs - see [./action.yaml].

There are no binaries used for running the action.

### Testing

The repository contains both `gradlew` and a build file with dependencies to junit and assertj.

Note that these are *only* used for development. They are not used for execution of the action.

## Preparing GPG key

See [Sonatype's instructions](https://central.sonatype.org/publish/requirements/gpg/).




## Using the Action

<!-- action-docs-description -->

### Description

Publishing Maven artifacts to Maven Central.

<!-- action-docs-description -->

<!-- action-docs-inputs -->

### Inputs

| parameter | description | required | default |
| --- | --- | --- | --- |
| signing_key | GPG private+public signing key | `true` |  |
| signing_key_secret | GPG signing key secret | `true` |  |

FIXME: signing key encoding

<!-- action-docs-inputs -->


<!-- action-docs-outputs -->

<!-- action-docs-outputs -->

<!-- action-docs-runs -->

### Runs

This action is a `composite` action.
<!-- action-docs-runs -->
