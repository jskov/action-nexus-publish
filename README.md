# action-maven-publish
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jskov_action-maven-publish&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jskov_action-maven-publish)

This action will sign Maven artifacts and publish them to MavenCentral.

**IT IS NOT READY YET**

## Simple(r) and Transparent

The action does the same as the publishing plugins of Gradle and Maven (or near enough).
But it does so in less than 1kLOC self-contained java that any (java) programmer should be able to verify the logic of.

When using a plugin in Gradle or Maven, all other plugins (and potentially any dependency jar used for compilation/testing) will have access to your GPG signing key and your credentials for MavenCentral.

Depending on your level of paranoia this may be OK. Or it could be a problem.

For me it is a problem, hence this action.

### Runtime

The action runtime is built just using `javac` without any dependencies (just the classes in ['src/main/java'](./src/main/java)).
This is done as the action is run - see [action.yaml](./action.yaml).

There are no binaries or third party dependencies used for running this action. Just the Java SE library.

### Releases / Tags

I will be tagging this repository to have named base lines and to make it simpler to make release notes.

You should however **not** use a tag name (or worse, a branch name) when referencing this action.
Otherwise you run the theoretical risk of the tag being moved in bad faith (or worse - and more likely - suffer bugs in new commits due to my incompetence).

You *should* be using the Git hash from a release (see `Code Review` section below).

I do not foresee many releases. Maybe expose some extra settings for configuration.
And at least one more when SonaType takes the new Publishing API out of early access.

### Code Review

So if you cannot trust your secrets to Gradle or Maven (and whatever dependencies you drag into your build), why should you trust this action?

Well, you should not!

You would do well to fork this repository and review the code. And then use the action from your forked repository!
Basically the [official](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions#using-third-party-actions) recommendations but with extra paranoia :)

According to [SonarCloud](https://sonarcloud.io/project/information?id=jskov_action-maven-publish) there are ~800 lines of java code.

If you have written enough code to publish anything on MavenCentral, it should be a piece of cake to review.

Also remember to have look at [action.yaml](./action.yaml) to verify that only the ['src/main/java'](./src/main/java) classes are used.

### Testing

The repository contains both `gradlew` and a build file with dependencies to junit and assertj.

These are **only** used for development. They are not used in the execution of the action.

There are 'unit' tests which can be executed without context. 
And there is a single 'integration' test which uploads a bundle to OSSRH (this needs credentials, so can only be run manually).

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
| search_directory   | The directory to search for POM files | `true` | |
| companion_suffixes | The companion files to include with found POM files | `false` | ".jar, .module, -javadoc.jar, -sources.jar" |
| signing_key        | GPG private signing key | `true` | |
| signing_key_secret | GPG signing key secret | `true` | |
| ossrh_username     | The OSSRH login name | `true` | |
| ossrh_token        | The OSSRH token | `true` | |
| target_action      | The action to take for bundles after upload (drop/keep/promote_or_keep) | `false` | keep |
| log_level          | Log level (for JUL framework) (info/fine/finest) | `false` | info |

<!-- action-docs-inputs -->


<!-- action-docs-outputs -->

<!-- action-docs-outputs -->

<!-- action-docs-runs -->

### Runs

This action is a `composite` action.

The java code in ['src/main/java'](./src/main/java) is compiled and started.

The `search_directory` is searched for '*.pom' files.

For each pom-file a bundle is created. The bundle contains the pom-file and companion files matching the pom-file's basename appended each of the strings in `companion_suffixes`.

The bundle files are packaged into a jar-file which is signed using GPG (with `signing_key`/`signing_key_secret`).

Then the bundle jar-files are uploaded to OSSRH using `ossrh_username`/`ossrh_token`.

Finally, all the uploaded bundles are dropped/kept/promoted according to `target_action`.
Note that if any of the bundles fail validation, 'promote' will fall back to 'keep' (hence 'promote_or_keep').



<!-- action-docs-runs -->
