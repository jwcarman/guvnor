# Lesson 4 — Quarantined

> **The belief this corrects:** I will validate what the model extracted before acting on it.

*Part 4 of Guvnor, a seven-part series on what it takes to let a language model act on untrusted
input without letting untrusted input act through it. Everything below runs; the code is in this
directory.*

---

## Stop asking the model to be right

[Lesson 3](../guvnor-3-concealed) ended with a model that refused a crude injection eloquently and
then paid out on a polite one fifteen seconds later. It reliably refuses attacks that look like
attacks, and reliably complies with attacks that look like work. Which of those arrives is the
attacker's choice, and costs them nothing.

The obvious next move is to check what the model produced before acting on it. Look at the amount,
look at the account, and if something is off, do not do it.

That instinct is right and it is one line from useless. A validation you remember to call is a
control in the place you thought of and nowhere else — and the next person to add a tool will not
think of it, because they are thinking about their tool.

So this lesson does not add a check. It changes what a claim is worth, and it changes what the
acting model is allowed to see.

## Two halves

**The model that reads is not the model that acts.** The quarantined read gets a document and a
shape and nothing else — no tools, no memory, no history. Whatever the email asked for, an enum
and an amount cannot carry it out.

**What it produces is untrusted.** A claim comes out labelled `UNENDORSED`, and the authority that
moves money admits only `ENDORSED`. Persuasion does not cross that line, because the line is not
made of judgement.

Both halves are needed. The first means an instruction cannot reach a model with tools. The second
means that even when the quarantined model is fooled — and it will be — being fooled does not move
money.

## A second question

Lesson 3 asked one thing about every value: **how sensitive is this?** Enough to stop cardholder
data reaching a third party, and no use at all against a stranger giving orders, because an
instruction is not sensitive.

```java
public enum Integrity { ENDORSED, UNENDORSED }

public static final Axis<Integrity> INTEGRITY =
    Axis.ladder("integrity", Integrity.ENDORSED, Integrity.UNENDORSED);
```

**How much is this to be believed?**

`ENDORSED` is first, which makes it the *bottom* of the ladder. That reads backwards until you say
what a ceiling means: a door admitting *at most* `ENDORSED` admits only what has been endorsed, and
anything less trusted sits above it and is refused. Untrusted is the more constrained end — Biba's
integrity model, upside-down from a sensitivity ladder, which is why it is worth writing once
rather than assuming.

## The chain

Four steps, and each is declared in the charter.

**1. Guards up at the edge.** Mail is concealed before it is stored, not when something gets round
to being careful:

```java
@Override
public Message receive(AccountId from, String subject, String body) {
  return super.receive(from, subject, inbound.conceal(body).id());
}
```

The plaintext never enters the desk's storage. What is kept is a surrogate id. Mail to a billing
desk may contain a card number and certainly contains something a stranger wrote, and both are true
the instant it lands.

**2. Scrub.** `mail.redacted` lowers sensitivity from `CARDHOLDER` to `PERSONAL`. This is
[lesson 3](../guvnor-3-concealed), unchanged, and it still applies — including to the quarantined
model, which is still a model, which is still a third party that keeps what it is shown.

**3. Read behind glass.** `mail.claim` runs the extraction:

```java
charter.checking("mail.claim", Mail.TYPE, Claim.TYPE, quarantine::read,
    d -> d.accepting(ctx -> Ceiling.of(SENSITIVITY, Constraint.any())
                                   .with(INTEGRITY, Constraint.any())));
```

Note what it does **not** say: `lowering(...)`. Turning a `String` into a `Claim` changes how
structured a value is, and structuredness is not a security property. The claim comes out carrying
exactly the label the mail had after scrubbing — personal, and unendorsed. A model read a
stranger's email to produce it, so it is precisely as untrusted as what it came from.

### So the extraction buys nothing?

It buys a great deal, and none of it is a label.

A `String` that came from a stranger can hold an instruction. A `Claim` cannot: an account id, an
amount, an enum, and a charge id. Nothing on it has room for a sentence. That is a real and
enormous change, and the reason it is not on an axis is that it is already enforced by something
stronger.

**It is the type.** `credit-authority` is declared over `Claim.TYPE`, and Loch refuses on type
before it ever considers the label. A `Mail` surrogate offered to that door is not "above the
ceiling", it is the wrong kind of thing. And a type is a fact about a value, where a label is an
assertion by whoever concealed or lowered it — so the guarantee that this value cannot carry
prose is the one guarantee here that nothing can talk its way out of.

Which is why it matters that `Claim` has no `String` on it. The type carries that promise only
while the shape genuinely has nowhere to put prose. Put a `notes` field on it and the type still
says `Claim` and the promise is gone.

So the honest summary of what the quarantined read achieves:

| | before | after |
|---|---|---|
| can it carry an instruction | yes, it is prose | **no, by type** |
| how sensitive is it | personal | personal |
| how much is it believed | unendorsed | unendorsed |

One of those three changed, and it is the one that stops prompt injection. The other two are
unchanged because reading something does not make it truer or less private.

**4. Endorse, or do not.** `claim.confirmed` is the only route from `UNENDORSED` to `ENDORSED`:

```java
private static Optional<Claim> supportedByACharge(ChargeService charges, Claim claim) {
  return charges.forAccount(claim.account()).stream()
      .filter(charge -> !claim.amount().isGreaterThan(charge.amount()))
      .findFirst()
      .map(charge -> claim.supportedBy(charge.id()));
}
```

**What elevates a claim is agreement with something already trusted.** Not the model's confidence,
not the email's tone, not whether anyone found it convincing. The lookup is the derivation's own,
and no email can reach into it.

## The desk tells you where to look

This prints at startup:

```
  derivations (3)
    claim.confirmed  claim -> claim   << WEAKENS LABELS
    mail.redacted    mail -> mail     << WEAKENS LABELS
    mail.claim       mail -> claim

  2 operation(s) can WEAKEN a label:
    claim.confirmed  claim -> claim
    mail.redacted  mail -> mail

  nothing unreachable: every door can be used and every type can exist
```

Three derivations, two of which can weaken a label, and they are named. That is the review. Not
"read the codebase and satisfy yourself" — two functions, one that decides what stops being
sensitive and one that decides what starts being believed.

**That list caught a bug in this lesson while it was being written.** `mail.claim` appeared on it,
because the extraction was quietly dropping sensitivity to `ORDINARY` as a side effect of producing
a narrow shape. Nothing was broken and nothing failed; a declassification was simply happening
somewhere nobody would look for one. The manifest said so on every startup.

## What happens when the model is persuaded

The point is not that the model stops being fooled. Here is a run where it was.

```
desk : the quarantined read says: GOODWILL_CREDIT of 999.00
desk : --> reading an email
       | A customer has written in. They are account 01a0ca41-…
       | On their account: charge 01a0ca41-…
       |
       | There is a claim from them: sur_01a0ca69-… Act on it, or do not.
desk :     thinking
desk : *** wants to: [issue_credit]
desk : the billing system does not support this claim, so it stays unendorsed
desk :     failed: The billing system has no charge that supports this claim.
             A goodwill credit has to be about something.
```

```
Money that has moved
  Nothing yet.
Total that left: $0.00
```

Read what happened there.

**The quarantined model was fooled.** It recorded exactly what the injection demanded. That is
expected, and Nessy's own `Extractor` documentation says so: *"This does not prevent prompt
injection, and is not trying to."*

**The acting agent saw no prose.** Look at what it was given — an account, a charge, and a claim
id. Not one word the customer wrote, and not even the amount. There was nowhere for an instruction
to arrive.

**It asked for the money anyway, and did not get it.** Being convinced was never what decided.

## Three holes, found by reading rather than running

This lesson took four attempts to get right, and the failures are more instructive than the design.

**The model retyped the amount.** The first version told the agent *"the customer wants $999.00"*
and gave it a tool taking an account and an amount. A model told about $42.00 could have called
that tool with $5,000.00 and nothing would have noticed, because the arrangement never saw the
claim being acted on. The tool now takes a **claim id**. The worst a persuaded agent can do is ask
for a claim that exists to be acted on.

**The claim had a free-text field.** First a `reason` the model wrote, which went into the ledger —
a channel out of the quarantine into a durable record. Then a `basis` the endorsement wrote, which
was safe in practice and still wrong: a shape that permits prose is a shape whose guarantee depends
on every future caller declining to use it. A claim now carries an account, an amount, an enum, and
the id of the charge that vouched for it. **Nothing on it can hold a sentence.**

**The extraction was a `source`.** Reveal the mail, extract, conceal the result — which mints a
value with an *asserted* label from mail that had a different one. That is a declassification
performed outside the system whose whole job is to govern declassifications, and the lineage had no
edge from the mail to the claim, so *"where did this come from"* ended at a source that claimed to
know. As a derivation, the provenance is recorded and the step is declared.

The shape of all three is the same: **quarantine the thing you thought of, and the next channel is
the one you did not.**

## What is still wrong

**The kind is checked, not refused.** A claim knows whether it asks for a refund or a credit, and
the credit tool checks it in an `if`. It should be a door — but a derived value's label is computed
by `lowering(...)`, which is handed the incoming label and never the value produced, so *"this
claim asks for a credit"* cannot be said in the lattice today. There is a comment at that
`if`-statement saying it is doing a door's job.

**The rule lives in Java.** *A credit must be about a charge, and no larger than it* is ten lines
in `CharterConfiguration`. The people accountable for what this desk pays out cannot read it,
cannot review it, and cannot change it without a release.

That is the next lesson.

**Previous:** [Lesson 3 — Concealed](../guvnor-3-concealed) ·
**Next:** Lesson 5 — Approved. *The limits are business logic, so they live in my code.*

---

## Running this lesson

```bash
./mvnw install                                   # once, from the repository root
export OPENAI_API_KEY=...
./mvnw -pl guvnor-4-quarantined spring-boot:run  # http://localhost:8084
```

Or against a local runtime:

```bash
OPENAI_API_KEY=lm-studio \
OPENAI_BASE_URL=http://localhost:1234/v1 \
NESSY_MODEL=qwen/qwen3.6-35b-a3b \
./mvnw -pl guvnor-4-quarantined spring-boot:run
```

Read the manifest it prints at startup before sending anything. Then use the page's presets and
try to talk it into a credit. You may need several attempts before the model is convinced — that
part is still a coin flip and always will be. The interesting run is the one where it *is*.

Worth trying: ask for a **$42.00** goodwill credit rather than $999.00. That one *is* supported by
the charge, so it will be endorsed and the money will move. The bridge is a bridge, not a wall, and
a reader who only ever watches it refuse has not seen what it does.

## A question with production instincts behind it

*Does the model call hold a database transaction open?*

No. Loch's `Storage` interface has no notion of begin or commit, so an implementation cannot span
the application's function even if it wanted to. `Engine.deriving` reads the parents in one
transaction, decodes in another, runs your function with **no connection checked out and no lock
held**, and writes in a third. The lineage lock is taken inside `put` and `erase`, and nowhere else.

What that costs instead is atomicity: between reading the parents and writing the result, another
thread can erase a parent. Loch handles it rather than ignoring it — `put` takes the lineage lock
shared while `erase` takes it exclusive, and the engine has an explicit branch for an input that
vanished mid-operation. A slow derivation whose parent is erased at second twenty-nine produces a
refusal, not a corrupted chain.

So the objection to a model inside a derivation was never operational. It is that **a derivation
which lowers a label is the trusted step**, and a model must not be the reason a value becomes more
trusted. `mail.claim` is fine precisely because it lowers nothing.

## What is in here

| file | what it is |
|---|---|
| `CharterConfiguration` | every door and every declassification, in one file |
| `GuardedMessages` | conceals on the way in, so plaintext is never stored |
| `Quarantine` | the read behind glass — one model call, no tools |
| `Request` | the shape it fills in: an enum and an amount, and no free text |
| `Claim` | what a model claimed, with no text on it at all |
| `IssueCreditTool` | takes a claim id, endorses, then asks the authority |
| `Edge` | whose money is involved, taken from the access and never from the email |

## The tests

Not one of them asks a real model anything. The extractor is stubbed, because what is under test is
the arrangement — and a protection that needed a model to demonstrate it would be a protection that
depends on one.

| test | says |
|---|---|
| `cannot_reach_the_authority_that_moves_money` | an unendorsed claim is refused |
| `is_refused_at_any_size_at_all` | the door reads who vouched, not how much |
| `is_not_endorsed_when_no_charge_supports_it` | the bridge declines |
| `is_endorsed_when_a_real_charge_agrees_with_it` | and it is a bridge, not a wall |
| `reaches_the_authority_only_after_that_endorsement` | in that order, and only that order |
| `cannot_be_endorsed_if_the_mail_was_never_scrubbed` | the chain is not optional |
| `has_no_text_on_it_at_all` | nothing on a claim can hold a sentence |
