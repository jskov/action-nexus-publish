# action-maven-publish
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jskov_action-maven-publish&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jskov_action-maven-publish)

This Action will [sign and publish](https://central.sonatype.org/publish/publish-manual/) Maven artifacts to [MavenCentral](https://central.sonatype.org/) for security aware (and/or paranoid) developers.

**IT IS NOT READY YET**

## Simple(r) and Transparent

The Action does the same as the publishing plugins of Maven and Gradle (or near enough).
But it does so in less than 1kLOC self-contained java that any (java) programmer should be able to verify the logic of.

When using a plugin in Maven or Gradle, all other activated plugins (and potentially any dependency jar used for compilation/testing) will be able to exfiltrate your GPG signing key and MavenCentral credentials.

Depending on your level of paranoia this may be OK. Or it could be a problem.

For me it is a problem, hence this Action.

### Runtime

The Action runtime is built just using `javac` without any dependencies (just the classes in ['src/main/java'](./src/main/java)).
This is done as the Action is run - see [action.yaml](./action.yaml).

There are no binaries or third party dependencies used for running this Action. Just the Java SE library.

### Releases / Tags

I will be tagging this repository to have named base lines and to make it simpler to make release notes.

You should however **not** use a tag name (or worse, a branch name) when referencing this Action.
Otherwise you run the theoretical risk of the tag being moved in bad faith (or worse - and more likely - suffer bugs in new commits due to my incompetence).

You *should* be using the Git hash from a release (see `Code Review` section below).

I do not foresee many releases. Maybe expose some extra settings for configuration.
And at least one more when SonaType takes the new Publishing API out of early access.

### Code Review

So if you cannot trust your secrets to Maven or Gradle (and whatever dependencies you drag into your build), why should you trust this Action?

Well, you should not!

You would do well to fork this repository and review the code. And then use the Action from your forked repository!
Basically the [official](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions#using-third-party-actions) recommendations but with extra paranoia :)

According to [SonarCloud](https://sonarcloud.io/project/information?id=jskov_action-maven-publish) there are ~800 lines of java code.

If you have written enough code to publish anything on MavenCentral, it should be a piece of cake to review.

Also remember to have look at [action.yaml](./action.yaml) to verify that only the ['src/main/java'](./src/main/java) classes are used.

### Testing

The repository contains both `gradlew` and a build file with dependencies to junit and assertj.

These are **only** used for development. They are not used in the execution of the Action.

There are unit-tests which can be executed without context. 
And there is a single integration-test which uploads a bundle to OSSRH (this needs credentials, so can only be run manually by someone providing said credentials).

### FAQish

* Why did you not write this Action in javascript - it is more conventional for Actions?  
  I do not have enough experience with the plain javascript language to do that (efficiently, anyway).  
  And including 27 NPMs to solve the problem would defeat the purpose of this Action.  

  The target audience of this Action consists of java programmers; they should be able to easily review the code.

* Why did you not write this Action in shell script - it is more conventional for Actions?  
  Same as above, really.  
  But I do believe it could be done much simpler in shell script by someone with enough experience there.
  Not sure if it would be easy for java developers to review, though.

* Can my dependencies in Maven/Gradle really read my secrets?  
  Sure, any class that is loaded/instantiated can do anything with the environment provided.  

  Can you be sure what classes are loaded during a Maven/Gradle invocation?  
  You may trust the two projects behind Maven and Gradle, but do you trust all the dependencies they use during a run?  
  
  As a mitigation, you can publish with Maven/Gradle in a separate step that does not build/test anything.  
  This way you can at least exclude risk from all your project's (transitive) build and test dependencies.

* Are you really this paranoid?  
  When it suits me.


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
| companion_suffixes | The companion files to include with found POM files, comma-separated | `false` | ".jar, .module, -javadoc.jar, -sources.jar" |
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

This is a [composite](https://docs.github.com/en/actions/creating-actions/creating-a-composite-action) Action, automating the instructions of [OSSRH manual publishing](https://central.sonatype.org/publish/publish-manual/).

The java code in ['src/main/java'](./src/main/java) is compiled and started.

The `search_directory` is searched for '*.pom' files.

For each pom-file a bundle is created. The bundle contains the pom-file and companion files matching the pom-file's basename appended each of the strings in `companion_suffixes`.

The bundle files are packaged into a jar-file which is signed using GPG (with `signing_key`/`signing_key_secret`).

Then the bundle jar-files are uploaded to OSSRH using `ossrh_username`/`ossrh_token`.

Finally, all the uploaded bundles are dropped/kept/promoted according to `target_action`.
Note that if any of the bundles fail validation, 'promote' will fall back to 'keep' (hence 'promote_or_keep').

See [OSSRH instructions](https://central.sonatype.org/register/central-portal/) for how to prepare the necessary [GPG](https://central.sonatype.org/publish/requirements/gpg/) and [Token](https://central.sonatype.org/publish/generate-token/) arguments.

<!-- action-docs-runs -->
