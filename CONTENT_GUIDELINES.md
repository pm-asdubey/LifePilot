# Content Guidelines — plain language for all user-facing copy

LifePilot is for everyone, not just engineers. **A person who has never written code must be able to use
the app without learning its internal vocabulary.** Every user-facing string — screen labels, buttons,
empty states, error messages, notifications, onboarding copy, and the instructions we send the AI about
how to phrase its answers — must follow this guide. (Resolves ISSUE-053.)

## The rule

Never show the user an internal/technical term. Translate it.

| Internal / technical term | Say instead |
|---------------------------|-------------|
| Object, LifePilotObject | **Record** |
| Domain | **Life area** |
| Metadata | **Details** |
| Metadata field | **Detail** |
| Verification Status | **Confirmed / Unconfirmed** (rejected → **Dismissed**) |
| Life State | **Understanding** |
| Domain Life State | **&lt;Life area&gt; overview** |
| Attention Required | **Needs attention** |
| OCR | **Scanned text** (or just "Scanned") |
| Pipeline | *(never shown)* |
| Schema | *(never shown)* |
| Proposal / ActionItem | **Suggestion** |

## Applied so far (2026-07-03)

- `ActionPlanCard`: "Objects to create" → "Records to create"; "Domain update" → "Life area update".
- `LinkObjectSheet`: "Link Object" → "Link a record"; "Search objects" → "Search records".
- `DocumentViewerScreen`: "OCR" → "Scanned text".
- `ObjectDetailScreen` provenance badges: "Verified" → "Confirmed"; "Rejected" → "Dismissed";
  "OCR · unverified" → "Scanned · unconfirmed"; "AI · unverified" → "AI · unconfirmed".

## Writing rules for new copy

1. **Name the real thing, not the abstraction.** Prefer "your passport", "this job", "your insurance"
   over "this object/record" wherever the concrete type is known.
2. **Short, calm, human.** One idea per line. No exclamation-heavy marketing tone.
3. **Verbs the user understands** — "Confirm", "Add", "Scan", "Link", "Remove" — never "Execute",
   "Persist", "Sync engine".
4. **Errors say what happened + what to do**, in plain words. Not "NullPointerException" / "parse failed".
5. **The AI's phrasing counts too.** When `PromptBuilderImpl` instructs the model how to answer, tell it
   to use everyday language and the terms above — the model's output is user-facing copy.
6. **Code identifiers are exempt.** Class/variable/enum names (`LifeObject`, `DomainLifeState`,
   `MetadataEntry`) stay technical — only *strings the user can see* are translated.

## How to check

- Grep new UI strings before merge:
  `grep -rnE '"[^"]*\b(Object|Domain|Metadata|Verification|OCR|Schema|Pipeline)\b[^"]*"' features/*/src/main/java/**/ui`
  — any hit inside a `Text(...)`, `label`, `title`, `placeholder`, or `contentDescription` must be
  translated (matches on code identifiers like `objectId` are fine to ignore).
- New user-facing copy is reviewed against this table in code review.
