<div align="center">

# Identica-ChatBlocker

Addon-plugin for [Identica](https://github.com/whereareiam/Identica) that blocks chat messages during authentication.

</div>

# Information

![version](https://img.shields.io/github/v/release/purpurcof/Identica-ChatBlocker)
![build](https://img.shields.io/github/actions/workflow/status/purpurcof/Identica-ChatBlocker/build.yml?branch=dev)

[![GitHub Downloads](https://img.shields.io/github/downloads/purpurcof/Identica-ChatBlocker/total?&label=GitHub%20Downloads)](https://github.com/purpurcof/Identica-ChatBlocker/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/5oaiXkWo?&label=Modrinth%20Downloads)](https://modrinth.com/plugin/5oaiXkWo)

# Requirements

* **Required Plugins:**
  * [Identica 3.0.x](https://github.com/whereareiam/Identica)
  * [packetevents 2.14.x](https://github.com/retrooper/packetevents)
* **Tested Platform:** Velocity

> [!WARNING]
> **BungeeCord Support:** BungeeCord is currently **untested** (may work). Its Adventure classes are loaded separately from PacketEvents, so verify it on a test proxy before relying on it in production.

# How it works

While a player is authenticating, registering or migrating, chat sent to them is withheld unless it is one of Identica's own messages:

* Identica's messages are recognised from **every** message configuration Identica has loaded: the core messages from the root injector, and the provider messages (such as the credential provider's login/register prompts) through the config providers registered with Identica. Provider modules live in child injectors that the root injector does not list, which is why both sources are needed.
* Placeholders in the templates are treated as gaps, so `You have {remaining} tries left.` matches `You have 3 tries left.`. Templates with very little literal text are ignored on purpose.
* Player chat (`DISGUISED_CHAT`, and `CHAT_MESSAGE` for 1.19+ clients) is always withheld; it can never be an Identica message.
* If no message templates could be loaded (for example Identica is still starting), chat is **not** filtered, so that Identica's prompts are never hidden by mistake.
* Matching runs under a step budget; a message that would make a template backtrack excessively is treated as "not an Identica message".
