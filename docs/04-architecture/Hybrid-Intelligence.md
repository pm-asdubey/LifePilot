# Hybrid Intelligence — Strategy Analysis

**Date:** 2026-07-04
**Status:** Strategy note (not yet implemented) — read when planning the intelligence roadmap.
**Hard constraint driving everything below:** LifePilot must stay **free for anyone**, with **no
user-linked API key required**, and honour the offline-first / "AI retrieves, never remembers" principles.

---

## First, the uncomfortable truth about "free"

"Free + no API key" has only three real sources of LLM intelligence:

- **On-device inference** — free, private, offline, but weaker/slower and device-gated.
- **A shared hosted backend/key you pay for** — not free (you pay), not offline, keys get
  rate-limited/banned, abuse risk. Contradicts offline-first. Rule it out as the *default*.
- **Deterministic + knowledge-driven intelligence** — free, reliable, offline, but not "conversational"
  on its own.

So the honest framing: **the default intelligence must be on-device or static-file-driven; a cloud LLM
is an *optional upgrade* for users who choose to link a key — never a requirement.** The `AiProvider`
seam and the deterministic guards already in the codebase (`ActionPlanNormalizer`, parser hardening,
continuation) make that pivot cheap.

---

## The intelligence stack, layer by layer (value vs. cost)

### Layer 0 — Deterministic core (already built, free)
Schema Engine, Life State Engine, RetrievalEngine, `ActionPlanNormalizer`, `AiActionParser` hardening,
date/expiry math, reminder rules. Real intelligence that needs no LLM.
**Verdict: push more here.** Much of LifePilot ("what needs attention", "when does X expire", "generate
the standard tasks for event Y") should be rules, not model calls.

### Layer 1 — On-device embeddings + entity extraction (free, ~50–150MB)
MediaPipe text embedder (or a small MiniLM/ONNX) for *semantic* retrieval; ML Kit Entity Extraction
(already shipped) for dates/amounts/names.
**Verdict: high value, low cost.** Biggest grounding win short of an LLM. Do early.

### Layer 2 — On-device LLM (free, but the expensive one)
Gemma 2 2B / Llama 3.2 3B / Phi-3.5-mini via **Google AI Edge / MediaPipe LLM Inference** or llama.cpp.
Handles conversation + classification + plan phrasing — replacing NVIDIA as the *default*.
Honest costs: 1–3GB download; ~4GB+ RAM (budget phones can't); 5–30 tok/s (a 14-task plan ≈ 30–60s);
battery/heat; **materially weaker reasoning + instruction-following** than Llama-70B/Claude — marriage-plan
quality *will* drop. Saving grace: the deterministic guards are exactly what make a weak model usable.
**Verdict: necessary for the "free conversational" goal, but only viable because Layer 0 carries reliability.**

### Layer 3 — Internet search (freshness)
The hardest to do "free + legal + reliable." General web-search APIs need keys (Brave/Serp) or violate ToS
(scraping).
**Verdict: don't build general web search.** Do **targeted fetch from a few stable free sources**
(Wikipedia API, government sites/APIs) and RAG the result into the on-device model. Narrow, reliable, free.

### Layer 4 — On-device knowledge base, refreshed every X days (the sweet spot)
A bundled + periodically-updated structured pack: "documents needed for marriage/visa/new job", visa
processing times, tax deadlines, **cross-domain cascade templates** — as JSON + embeddings the
deterministic engine and retrieval consume. Refreshed by a WorkManager job pulling a **static file from a
free CDN** (GitHub Pages / Cloudflare Pages / R2 free tier) every X days.
**Verdict: highest value-per-effort.** No per-user, no per-query cost; sustainable; delivers the
comprehensive, specific, cross-domain plans **without a big model** — the model just personalizes and
phrases. The purest expression of "AI retrieves, never remembers."

---

## What's best for THIS project (ranked by value-per-effort)

1. **Knowledge base + deterministic cascade templates (L4 + L0)** — biggest comprehensiveness/reliability
   win, fully free/offline, aligns with architecture. *Start here.*
2. **On-device embeddings retrieval (L1)** — cheap, big grounding gain.
3. **Periodic static KB refresh (L4-update)** — trivial to add (WorkManager + static fetch), high freshness ROI.
4. **On-device LLM as the default provider (L2)** — the real "no API" unlock, but heavy; do after 1–3 so the
   model has less to carry.
5. **Targeted source fetch (L3)** — last, scoped to specific sources.

### The recommended shape: a capability-tiered hybrid, not one model
- **Default (everyone, free):** deterministic core + knowledge pack + on-device embeddings + small
  on-device LLM. No key, offline, private.
- **Graceful degradation:** on a 2–3GB-RAM phone that can't run the LLM, fall back to deterministic + KB +
  templated responses (still genuinely useful — where L0/L4 pay off). Design this as a first-class mode.
- **Optional upgrade:** power users *may* link a cloud key for stronger reasoning (the current path).
  Nobody is *required* to.

---

## Honest risks

- **Device fragmentation:** "free for everyone" really means "free intelligence *tiered by device*."
  The deterministic fallback must be a first-class mode, not an error state.
- **Quality drop:** small models are worse at long, strict, multi-domain output. Guards mitigate but don't
  erase it; the KB/templates must do the heavy lifting.
- **Maintenance:** the knowledge pack needs curation — ongoing human work, but cheap and static-hosted.
- **"Free for you":** static file + model hosting has *some* cost, but static/CDN free tiers cover it with
  no per-user scaling. A hosted *inference* backend would not be — avoid it.

---

## Suggested phased roadmap

- **Phase 1:** Deterministic cascade templates + bundled knowledge pack (JSON) → comprehensive plans without
  more LLM. Keep NVIDIA/Anthropic as optional providers.
- **Phase 2:** On-device embeddings for retrieval + classification (+ ML Kit entity extraction).
- **Phase 3:** Periodic KB refresh (WorkManager pulling a static CDN file every X days).
- **Phase 4:** Small on-device LLM as the default `AiProvider` (`OnDeviceLlmProvider`), with a deterministic
  fallback for weak phones. Cloud keys become optional upgrades.
- **Phase 5:** Targeted internet fetch (specific free sources) → RAG into the on-device model for freshness.

## One-line recommendation

**Invest in Layers 0 + 4 first (deterministic cascade + a refreshed on-device knowledge pack), add
on-device embeddings, then swap in a small on-device LLM as the default `AiProvider` with a deterministic
fallback for weak phones — keeping cloud keys as an optional upgrade.** That yields free-for-everyone,
offline, private intelligence whose *reliability comes from the system, not from a rented model* — the only
version of this that stays both free and good.

---

# Addendum — Training our own tiny, task-specific model (2026-07-04)

## "Train our own model" is three different things
- **Pre-train from scratch → NO, never.** Millions of dollars, huge data; a "tiny" from-scratch model
  would be *worse* than a random open 1B. Kill it.
- **Fine-tune a small open base** (Qwen2.5-0.5B/1.5B, Llama-3.2-1B, Gemma-2-2B, SmolLM2) with LoRA → the
  real, feasible version. A few dollars of GPU, a few hours.
- **Distill** — use a big model (Claude/GPT) to *generate* the training data, then fine-tune the small
  model to imitate it on our narrow tasks → the smart version for us.

The viable project = **fine-tune a tiny model on distilled, task-specific data.**

## Where a fine-tuned tiny model genuinely wins
On **narrow, structured, repetitive tasks**, a fine-tuned 0.5–1.5B model can **match or beat a general
70B model** while being ~100× smaller and running on-device. Our action-block generation is exactly that:
document → `OBJECT_CREATION` JSON; life event → `ACTION_PLAN`; metadata extraction; intent/classification.
A model *trained* to emit our schema follows it far more reliably than a general small model — fixing the
instruction-following weakness. Bonus: a 0.5–1.5B specialist runs on *more* phones than a 3B generalist.

## Where it fails (non-negotiable division of labor)
A 1B model cannot know tax rules, visa times, "what documents for X in country Y" — can't fit it, and it
goes stale. So:
- **Fine-tuned tiny model** = structured-task engine (understand → emit JSON → phrase short answers).
- **KB + targeted search** = the facts (RAG feeds the model; it never answers from memory).
- **Deterministic engine + templates** = comprehensiveness + reliability.
Blur this — expect the model to "know things" — and it hallucinates.

## The real cost is data + eval, not compute
- **Compute:** ~$5–50, trivial.
- **Data pipeline:** the actual work. We have zero training data. Synthesize thousands of (message → exact
  JSON) pairs across all 12 domains + event types via a big model; keep them diverse. Classic trap:
  **synthetic data ≠ how real users type** → brittleness on real input.
- **Eval harness:** the #1 failure mode is no rigorous eval. Need a held-out set + metrics (action-block
  validity %, field accuracy, domain accuracy). Weeks, not a weekend.
- **Schema coupling:** the model is tied to our schema — schema changes may force retraining. The
  deterministic layers don't have this problem, another reason they stay primary.

## Portfolio value — straight answer
**Strong — potentially stronger than most.** Shows distillation, LoRA, quantization, on-device deployment,
RAG, and a real product: a rare full-stack ML-eng story. But the impressive part is the **eval methodology
+ data pipeline + system design**, not the fine-tune itself (LoRA is a script). Done rigorously → excellent;
done as "ran a notebook" → forgettable.

---

# All plans compared (pros / cons)

| # | Plan | Free & no user key? | Offline | Quality | Effort | Verdict |
|---|------|:--:|:--:|:--:|:--:|------|
| 1 | **Cloud API, user links key** (current) | ✗ (needs key) | ✗ | ★★★★★ | done | Keep as **optional upgrade** only. |
| 2 | **Shared/bundled cloud key or hosted backend** | ✗ (you pay) | ✗ | ★★★★☆ | med | **Reject** — cost, abuse, key bans, breaks offline-first. |
| 3 | **Deterministic core + KB only, no LLM** | ✓ | ✓ | ★★★☆☆ (structured), ✗ conversational | med | **Foundation.** Reliable, free; not conversational alone. |
| 4 | **General small on-device LLM** (Gemma/Llama 3B) | ✓ | ✓ | ★★☆☆☆ | high | Works, but weak + heavy (3B, 4GB+ RAM); poor instruction-following. |
| 5 | **Fine-tuned tiny specialized model** (0.5–1.5B, LoRA+distill) | ✓ | ✓ | ★★★★☆ *on our tasks* | high (data+eval) | **Best model option** — beats #4 on our narrow tasks, lighter, more devices. |
| 6 | **Hybrid: deterministic + KB + embeddings + #5, cloud optional** | ✓ (default) | ✓ | ★★★★☆ | high (phased) | **The plan.** Reliability from the system, model does the last mile. |

Key point: quality for #5/#6 is high **only on our narrow structured tasks**, and only because the KB
supplies facts and the deterministic layer supplies comprehensiveness. None of these make a tiny model
"smart" in general — they make the *system* smart.

---

# DECISION — the plan going forward

**Build the Hybrid (Plan 6). Order matters: foundation → retrieval → knowledge → model.** Rationale: each
phase ships value on its own, de-risks the next, and the earlier phases generate the ground-truth structure
the fine-tuned model later imitates. Do NOT start with the fine-tune — it's the highest-uncertainty piece
and needs the rest to exist first.

- **Phase 1 — Deterministic cascade + knowledge pack (Layers 0 + 4).**
  Event→cascade templates (marriage → Identity/Finance/Career/… tasks) + a bundled JSON knowledge pack
  ("documents needed for X", processing times, deadlines). *Ships:* comprehensive, specific, cross-domain
  plans with today's cloud model — and works offline for the deterministic parts. *Gate:* plans are
  comprehensive without the user asking.
- **Phase 2 — On-device embeddings + entity extraction (Layer 1).**
  Semantic retrieval + ML Kit entity extraction. *Ships:* better grounding, better classification, all free.
- **Phase 3 — Periodic KB refresh (Layer 4-update).**
  WorkManager pulls a static CDN file every X days. *Ships:* freshness with zero per-user/per-query cost.
- **Phase 4 — Fine-tuned tiny model as an OPTIONAL `AiProvider` (Layer 2, specialized).**
  Distill a dataset from a big model → LoRA fine-tune a 0.5–1.5B base → quantize → run via
  llama.cpp/MediaPipe. A/B against NVIDIA on **action-block validity %**. *Gate:* promote to *default*
  only when eval ≥ agreed threshold; otherwise it stays optional and cloud/deterministic remain default.
- **Phase 5 — Targeted internet fetch (Layer 3).**
  RAG from a few stable free sources into whichever model is active. *Ships:* on-demand freshness.

**Fallback tiers (always):** capable phone → on-device model; weak phone (≤3GB RAM) → deterministic + KB +
templated responses (first-class mode, not an error); power user → optional cloud key.

**If the goal is portfolio-first**, Phase 4 (the distill + LoRA + eval + on-device deployment) is the
showcase — but it still needs Phases 1–3 as the credible product around it, and its worth is the **eval +
data pipeline**, not the training run.

**One-line decision:** *Reliability lives in the system (deterministic + KB); the tiny fine-tuned model is
the free, on-device last mile — added last, behind an eval gate, with cloud kept optional and a
deterministic fallback for weak devices.*
