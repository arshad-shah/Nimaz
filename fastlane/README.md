fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android deploy_internal

```sh
[bundle exec] fastlane android deploy_internal
```

Deploy a new version to the Google Play internal track

### android deploy_beta

```sh
[bundle exec] fastlane android deploy_beta
```

Deploy a new version to the Google Play beta track

### android deploy_production

```sh
[bundle exec] fastlane android deploy_production
```

Deploy a new version to the Google Play production track

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
