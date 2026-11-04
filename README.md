# 日課 nikka — Practice

A meditation timer and a habit tracker in one. Sit for any length, with a warm-up and an
interval bell if you want them; every sitting is logged, and it ticks meditation off for the
day. Beside it, the habits you name yourself, each ticked daily or a few times a week, with a
streak, rest days, a month of days and totals for each. Built for the
[Mudita Kompakt](https://mudita.com/products/kompakt/), and it will install on any Android 12
device.

*Nikka* is 日課 — the day's practice, the thing you do every day.

Written from scratch in Kotlin and Jetpack Compose, using Mudita's own
[MMD](https://github.com/mudita/MMD) design system so it looks like the apps the phone already
ships with.

| | |
|---|---|
| ![The Breathe tab set to 4-7-8: in, hold, out, three breaths a minute, five minutes, cued by vibration](screenshots/1-breathe.png) | ![Today's habits with their cue lines and streaks, three ticked and two still open](screenshots/2-today.png) |
| ![A month of Walk: days done filled in, Sundays marked as rest days with a dash](screenshots/3-history.png) | ![The log: a sitting, a 4-7-8 breathing session, and a sitting added by hand with a note](screenshots/4-log.png) |

## Sitting

- **Any length.** Seven presets from 5 to 60 minutes, or **Other** for anything up to ten hours.
- **A warm-up** of up to two minutes before the first bell, to settle. **An interval bell**
  every 5 to 30 minutes, if you want one. One strike of the bowl as the sitting begins and at
  each interval, three at the end.
- **The bells ring on the alarm stream**, so the alarm volume sets how loud they are. They ring
  with the media volume down and with the ringer silenced. **Settings → Try the bell** plays
  one.
- **The screen goes dark while you sit**, its light turned down, and nothing on it changes, so
  the panel is not redrawn until the sitting ends. It turns itself off as it always would.
- **The bells come on time with the screen off**, in Doze, and after the app has been stopped:
  each one is an exact alarm, and the end bell is an alarm clock, which Doze always lets
  through. A quiet notification says when the sitting ends.
- **Hold anywhere to end early.** A sitting ended early is still logged, for the time actually
  sat. **Back** leaves the sitting running.
- **Do not disturb while sitting**, in Settings, turns on alarms-only Do Not Disturb for the
  length of each sitting and puts it back afterwards. It needs the phone's permission once.

When a sitting ends you see how long it was, the streak it keeps, and a field for a one-line
note.

## Breathing

- **A pattern to breathe by.** 4-7-8, 6-6 and Box (4-4-4-4) are a press away, or **Other** for
  your own: seconds in, holding full, out and holding empty, up to 20 each. The tab shows the
  pattern as a large figure, with what each number is and how many breaths it makes a minute.
- **Any of 1 to 15 minutes**, rounded up to whole rounds, so a session never stops mid-breath.
- **A cue at each change**, so it can be followed with the eyes shut: a buzz, a soft tone, both,
  or nothing but the screen. In and out feel and sound different; a hold is the faintest tick.
  The tones play at the alarm volume, as the bells do.
- **The screen stays awake while you breathe**, on the app's usual light page: the phase in
  words, the seconds left in it, and the round. It changes once a second and no more. Hold
  anywhere to end early; **Back** or leaving the app ends it too, since the cues are timed on
  screen.
- **Logged, but apart from meditation.** Each session of ten seconds or more goes in the log
  with its pattern. It does not tick meditation or count toward its streak.
- **Breathe before a sitting.** The Sit tab's **Warm-up** can be two or five minutes of
  breathing, in the Breathe tab's pattern and cue. When the breathing ends, or with a long
  press, the sitting begins at once. **Back** ends both.

## Habits

- **Today** lists every habit with a box for today. Meditation is ticked by any sitting, not by
  hand; the others are ticked with a press.
- **A cue for each habit**, if you want one: **When** in the habit's dialog takes a few words
  of your own, such as "after breakfast", and Today shows it on the habit's line: "After
  breakfast · 4 days in a row". Tying a habit to something you already do builds it better
  than a reminder does. Meditation can have one too.
- **Rest days.** A long press on a habit on Today marks today done, a rest day, or neither. A
  rest day is a day off on purpose: it keeps a streak going without adding to it, so a day
  sick or traveling does not undo a month. Its box is an outline with a short dash. For
  meditation, which is done by sitting, the choice is only to rest or not.
- **Every day, or some days a week**: a habit meant three times a week is kept when it is done
  three times between one week and the next, on whichever days. The week starts on the day
  the phone's language starts it.
- **A press on a habit opens its history**: the current streak and the longest, how many of
  the last 30 days were done (and rested), totals for this week, this month and all time, and
  a month of days as squares: filled for done, outlined for not, outlined with a dash for a
  rest day. Earlier months are a press away. A press on a past day marks it done, a second
  makes it a rest day, a third clears it, for the day you forgot. On meditation's month, a
  press on a day with no sitting marks it rested or clears it.
- **No zero to look at.** When there is no streak under way, Today and the history show how
  many of the last 30 days were done instead of "0 days in a row": a missed day costs a day
  there, not the whole run. **Settings → Show streaks** turns streaks off altogether, leaving
  only the last-30 count.
- **Meditation's history** adds the sittings and minutes in each total, and the minutes sat on
  each of the last seven days as plain black bars.
- A streak counts in days for a daily habit and in weeks for the others. Today not yet done
  does not break a daily streak until the day is over, and this week not yet kept does not
  break a weekly one until the week is.

## The log

Every sitting, newest first: when it began, how long it was, how long it was meant to be when
it ended early, and its note. Breathing sessions are there too, marked as breathing with their
pattern, "Breathing · 4 · 7 · 8 · 5 min"; they do not tick meditation and are not counted
among the sittings. A press opens an entry to change its day, time, length or note, or to
delete it, which asks for a second press.

**Add a sitting**, at the top of the log, is for one sat without the phone: on a retreat, or
with the phone left at home. Pick the day (today or earlier), the time it began, the minutes
and a note if you like; it ticks meditation for that day like any other sitting.

## Backup

**Settings → Save a backup** writes habits with their cues, ticks, rest days and the log as one JSON file, wherever you
choose through the phone's file picker. **Restore from a backup** reads one back and replaces
everything in the app, after a second press. There is no other copy: Android does not back the
app up.

## One step on a Kompakt

The Kompakt's DuraSpeed setting can shut down apps a few minutes after the screen goes dark,
and an app shut down that way has its alarms canceled. A sitting runs as a foreground service
for its whole length, but to be sure of the bells, Settings has a row on a Kompakt: **Open
DuraSpeed**, tap **Open** there, switch Practice on in the list, and press **It's switched
on**. If DuraSpeed stops the app again later, the row comes back.

## What it does not do

- **Nothing leaves the phone.** The app has no internet permission. See
  [PRIVACY.md](PRIVACY.md).
- **The phone's alarm icon shows while a sitting runs**: the end bell is set as an alarm clock,
  the kind of alarm that rings on time whatever the phone is saving power for.
- **No charts that need color.** Everything is black on white: filled and outlined squares,
  and black bars.

## The bell

The bowl is synthesized for this app by [`tools/make-bowl.py`](tools/make-bowl.py): four
partials of a singing bowl, each a pair of tones a fraction of a hertz apart so it shimmers as
a real bowl does, each dying away at its own rate, struck softly. There is no recording, so
there is no one else's sound to credit or license; it is part of this program and under its
license.

## Building

```
./gradlew assembleDebug
```

A release build needs a keystore at `signing/signing.keystore` with a matching
`signing/signing.properties`. There is no fallback key in this repository: without one, a
release build comes out unsigned rather than wrongly signed.

`./gradlew testDebugUnitTest` runs the tests of the streaks (daily and weekly, across months,
years, an unfinished day or week, and rest days), the month grid (from Monday and from Sunday,
four to six rows), what a press on a day does, the line under each habit, the times of a
sitting added by hand, and the bells' timing.

## Getting it, and keeping it

Download <https://github.com/wanderwildwood/nikka/releases/latest/download/nikka.apk>
and sideload it. That address always points at the newest release, and every release
publishes a `.sha256` beside the APK if you would rather check than trust.

For updates without doing this by hand, add this repository to
[Obtainium](https://github.com/ImranR98/Obtainium):

    https://github.com/wanderwildwood/nikka

## License

GPL-3.0-only. See [LICENSE](LICENSE).

Copyright (C) 2026 wander wildwood

This program is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, version 3.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If
not, see <https://www.gnu.org/licenses/>.

Icons are from Material Symbols, Apache 2.0. The bowl sound is made by this program's own
script and is under its license.
