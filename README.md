<div align="center">

# Identica-ChatBlocker

Addon-plugin for [Identica](https://github.com/whereareiam/Identica) that blocks chat messages during authentication.

</div>

---

### Information

![build](https://img.shields.io/github/actions/workflow/status/purpurcof/Identica-ChatBlocker/build.yml?branch=dev)
![version](https://img.shields.io/github/v/release/purpurcof/Identica-ChatBlocker)

[![GitHub Downloads](https://img.shields.io/github/downloads/purpurcof/Identica-ChatBlocker/total?&label=GitHub%20Downloads)](https://github.com/purpurcof/Identica-ChatBlocker/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/5oaiXkWo?&label=Modrinth%20Downloads)](https://modrinth.com/plugin/5oaiXkWo)

### Requirements

* **Dependencies:**
  * [Identica](https://github.com/whereareiam/Identica) dev build `b3ab26d` or newer (the unreleased 3.0.0 line)
  * [packetevents 2.14.x](https://github.com/retrooper/packetevents)
* **Platforms:**
  * Velocity
  * BungeeCord (untested, may work)

### Installation

1. Download the latest version of Identica-ChatBlocker from [Releases](https://github.com/purpurcof/Identica-ChatBlocker/releases).
2. Place the `.jar` file into the `plugins` folder.
3. Restart the server to generate the configuration and necessary files.

### How it works

While a player is authenticating, registering or migrating, chat sent to them is withheld unless it is one of Identica's own messages:

* Identica's messages are recognised from **every** message configuration Identica has loaded: the core messages from the root injector, and the provider messages (such as the credential provider's login/register prompts) through the config providers registered with Identica. Provider modules live in child injectors that the root injector does not list, which is why both sources are needed.
* Placeholders in the templates are treated as gaps, so `You have {remaining} tries left.` matches `You have 3 tries left.`. Templates with very little literal text are ignored on purpose.
* Player chat (`DISGUISED_CHAT`, and `CHAT_MESSAGE` for 1.19+ clients) is always withheld; it can never be an Identica message.
* If no message templates could be loaded (for example Identica is still starting), chat is **not** filtered, so that Identica's prompts are never hidden by mistake.
* Matching runs under a step budget; a message that would make a template backtrack excessively is treated as "not an Identica message".

> BungeeCord support is untested. Its Adventure classes are loaded separately from PacketEvents', so verify it on a test proxy before relying on it.


### Live tests

The `testing` module starts the packaged addon on a real Velocity proxy next to Identica, with Paper backends, and drives simulated players through it with [Anvil](https://github.com/whereareiam/anvil), in the way Identica's own `identica-testing` module does: a test declares its network with an annotation and describes a player with `Journey`, and a message expectation names the message through the configuration models the plugins read, never through copied text. Unit tests stay in their own modules; `./gradlew test` does not start these.

```shell
./gradlew :testing:anvil
```

Java 25 is needed. The first run downloads the proxy and server distributions; later runs reuse them. The tests install the jars of the Identica release named by `identica` in `gradle/libs.versions.toml`, which Gradle downloads from the release on GitHub. To run against a build of your own, pass its jars: `-Pidentica.jar=<file>`, `-Pidentica.credential.jar=<file>` and `-Pidentica.premium.jar=<file>`.

A failed test keeps its network under `testing/build/anvil/`, with an `anvil-console.log` per process, and Identica's debug logging is on. The *Live tests* workflow runs the same task on pull requests and on pushes to `main` and `dev`.
