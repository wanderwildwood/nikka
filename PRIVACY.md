# Privacy

Practice collects nothing and sends nothing anywhere. It has no internet permission, so it
cannot.

It keeps, in its own private storage on the phone: the habits you name and the days each was
ticked, the log of sittings and breathing sessions (when each began, how long it was meant to be and
how long it lasted, a breathing session's pattern, and any note you wrote), how the next sitting is set, and its few settings. Android does not
back it up, and uninstalling the app removes all of it.

**Save a backup** in Settings writes all of that as one file, to the place you choose in the
phone's file picker, and only when you press it. Where that file goes after that is up to you.

The permissions it asks for are for the bells and the breathing cues and nothing else: exact
alarms, so the bells ring on time; a foreground service and a wake lock, so they ring with the
screen off; notifications, for the quiet one that shows while a sitting is under way (Android
asks a foreground service to show one); vibration, to mark each change of breath when that is
the cue you choose; and, only if you switch it on, access to Do Not Disturb, so it can turn alarms-only
on for a sitting and back off afterwards. The one more in its manifest is not one you are asked
for: Android's support library adds it to every app built with it
(`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`), so that the app's own internal messages can reach
only the app itself.

The About dialog's links open a web address in your browser, and only when you press them: the
source code on GitHub (github.com), the author's site (wanderthe.dev) and the donation page at
Square (square.link). Those sites are separate from this app, and what they see of a visit is
covered by their own privacy policies; the app does not fetch any of them and learns nothing about
whether you went. "Write to email@wanderthe.dev" hands the address to your own email app, and
only when you press it. The About dialog also holds the full Privacy and Terms text and the
licenses.

<!-- shared-about privacy section -->
## Who we are

Practice is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Practice keeps what you put in it on your phone and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Practice is sold or shared.

Since Practice keeps what it stores only on your phone, you can exercise most of these rights yourself: delete a sitting or a habit in the app, save a copy with Save a backup, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->
