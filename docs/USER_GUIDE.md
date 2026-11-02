# Dragon Root — User Guide

## What you'll see
Open the menu (☰) to reach the ten sections.

| Section | What it does |
|---|---|
| Dashboard | Summary cards: device, firmware, kernel, root status, compatibility, database sync. |
| My Device | Every detected value with its **source** and **confidence**. Missing values read "Unavailable" — they are never guessed. |
| Firmware Center | Official firmware records, a Vivo/iQOO device catalog, search, sort, and build-vs-build comparison. |
| Kernel Analyzer | Parses your kernel string, compares two kernel strings, lists stored kernel profiles and public advisories. |
| Root Compatibility | Step 1: latest official firmware for your model. Step 2: per-project result with matched/unknown prerequisites and sources. |
| Boot Image Analyzer | Pick an image file you already have: SHA-256, header metadata, warnings, comparison of two files, text report export. |
| Research Library | Project metadata, advisories, and plain-language topics (rooting vs. iOS jailbreaking, bootloader status, custom kernels, recovery). |
| Update History | Database releases, their source, changelog, record counts and verification. |
| Settings | Background sync, Wi-Fi only, notifications, and **Delete all local data**. |

## Reading a compatibility result
| Label | Colour | Meaning |
|---|---|---|
| VERIFIED | green | Exact device + build + kernel record, with credible (grade A–C) evidence, recently reviewed. |
| PARTIALLY VERIFIED | yellow | Strong evidence but something is unconfirmed or the record needs review. |
| EXPERIMENTAL | yellow | Documented prerequisites match, but nothing proves it works on your exact build. **Not** a green light. |
| UNSUPPORTED | red | A documented prerequisite is definitely not met (for example the bootloader requirement). |
| UNKNOWN | grey | Not enough evidence. The normal result today. |

Evidence grades: **A** official manufacturer docs/advisory · **B** original project/researcher · **C** reproducible community test with exact build · **D** unverified repost (can never make a result VERIFIED).

A result for one firmware build never carries over to the next. When a newer official build is known, older records are shown as *outdated*.

## Boot image analyzer
Choose a file with *Select file*. It is read on your phone only. The app never claims the file belongs to your phone, and never modifies or flashes anything.

## If something looks wrong
* "Unavailable" values: Android restricts some information for normal apps. That is expected.
* "Root status: No root detected" is a weak signal — hidden root can't be ruled out.
* Missing firmware or compatibility data: the database only contains what reviewed, signed releases provide. Check your manufacturer's official support page for updates and recovery guidance.

Dragon Root does not give flashing instructions. Only change devices you own, keep backups, and follow official manufacturer guidance.
