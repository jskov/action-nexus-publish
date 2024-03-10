# action-nexus-publish

Publishing action for Nexus

## Preparing GPG key

See [Sonatype's instructions](https://central.sonatype.org/publish/requirements/gpg/).




## Using the Action

<!-- action-docs-description -->

### Description

Publishing Maven artifacts to Nexus (Maven Central).

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
