# Nimaz — Authenticating to the content repository

> **Owns:** how CI and local builds obtain a credential for the private `arshad-shah/nimaz-data`
> repository — the GitHub App setup, how the workflows consume it, local usage, rotation, and
> failure modes.
> **Update when:** you change the credential mechanism, the App's permissions, the secret names,
> or which workflows need it.
> **Verified by:** review only — the build itself fails at `fetchNimazData` with a message naming
> the three ways to supply a credential.
> **Related:** [`SUBSYSTEMS.md` §7](SUBSYSTEMS.md#7-content-seeding--versioning) for what the
> fetched artifact is and how it reaches a device,
> [`DOCUMENTATION.md`](DOCUMENTATION.md) for the update contract.

The prepackaged database is fetched from **[arshad-shah/nimaz-data](https://github.com/arshad-shah/nimaz-data)**,
which is private. `fetchNimazData` (registered in `app/build.gradle.kts`, implemented by
`FetchNimazDataTask` in `build-logic`) needs a credential that can read its releases.

CI uses a **GitHub App**, not a personal access token. The difference matters:

| | PAT | GitHub App |
|---|---|---|
| lifetime | until revoked | ~1 hour, minted per job |
| scope | whatever the human who made it holds | exactly `nimaz-data`, contents read-only |
| tied to | a person — dies with their account | the installation |
| leak blast radius | every repo that person can read | one repo, one hour, read-only |

The repository already uses this pattern for the version bump (`BUMP_APP_ID` /
`BUMP_APP_PRIVATE_KEY`), so this is the second App rather than a new idea.

## One-time setup

**1. Create the App** — <https://github.com/settings/apps/new>

| field | value |
|---|---|
| Name | `nimaz-data-reader` (any unused name) |
| Homepage URL | the nimaz-data repo URL — unused, but required |
| Webhook | **uncheck Active**. It never receives events |
| Repository permissions → **Contents** | **Read-only** |
| Everything else | leave at *No access* |
| Where can this be installed | *Only on this account* |

Contents read-only is the whole permission set. Release assets are served under Contents; it
needs nothing more, and anything more would widen the blast radius for no gain.

**2. Note the App ID** from the App's settings page.

**3. Generate a private key** — same page, *Private keys* → *Generate a private key*. A `.pem`
downloads. It is shown once.

**4. Install it on `nimaz-data` only** — *Install App* → your account → *Only select
repositories* → `nimaz-data`.

**5. Add the two secrets to the Nimaz repository:**

```bash
gh secret set NIMAZ_DATA_APP_ID --repo arshad-shah/Nimaz --body '<the App ID>'
gh secret set NIMAZ_DATA_APP_PRIVATE_KEY --repo arshad-shah/Nimaz < /path/to/key.pem
```

Then delete the `.pem`. It is recoverable by generating a new one; a copy sitting in
`~/Downloads` is not.

## How CI uses it

Every job that runs `./gradlew` starts with:

```yaml
- name: Token for the content repository
  id: nimaz-data-token
  continue-on-error: true
  uses: actions/create-github-app-token@v3
  with:
    app-id: ${{ secrets.NIMAZ_DATA_APP_ID }}
    private-key: ${{ secrets.NIMAZ_DATA_APP_PRIVATE_KEY }}
    owner: arshad-shah
    repositories: nimaz-data
```

`continue-on-error` is deliberate. If the secrets are missing, the job should fail at
`fetchNimazData` with a message naming the three ways to supply a credential — not at an action
whose failure says nothing about the cause.

## Locally

No App involved. The Gradle task resolves, in order:

1. `NIMAZ_DATA_TOKEN` environment variable
2. `nimazDataToken` in `~/.gradle/gradle.properties`
3. `gh auth token`

If you already have `gh auth login` with access to the repo, there is nothing to configure.

The chain is a lazy `orElse` of three Gradle providers, so `gh` is only invoked when the first
two are absent, and a machine without the `gh` CLI produces the "no credential" message rather
than a stack trace. The credential is an `@Internal` task property and never an `@Input`: input
values are written into the configuration-cache entry on disk, and a token is not something to
leave in `.gradle/configuration-cache`. It takes no part in up-to-date checks either, which is
correct — the artifact's identity is its sha256, not the credential used to fetch it.

## Rotating

Generate a new private key on the App, re-run the `gh secret set` above, then delete the old key
from the App's settings page. No workflow change, no window where builds break — the App can
hold two keys at once.

## When it fails

```
nimaz-data: no credential for the private content repository.
```

The App is not installed on `nimaz-data`, one of the secrets is missing, or the local fallbacks
found nothing. Check the *Token for the content repository* step: it will have failed silently
by design.

```
nimaz-data: https://api.github.com/... returned 404.
```

The credential works but cannot see the repo or the tag. Almost always the App is installed on
the account but not on `nimaz-data` itself.

```
nimaz-data: https://api.github.com/... returned 403 — authenticated, but not permitted.
```

The token minted, so the App *is* installed on the repository — `create-github-app-token` fails
outright when it is not. What is missing is the permission:

1. <https://github.com/settings/apps> → your App → **Permissions & events**
2. **Repository permissions → Contents → Read-only**
3. Changing permissions raises a request the installation has to accept:
   <https://github.com/settings/installations> → your App → **Review request** → Accept

That second step is the one people miss. GitHub does not apply a permission change until the
installation approves it, so the App page can show Contents: Read while the installation is still
running on the old, narrower set.
