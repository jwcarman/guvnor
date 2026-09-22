# Lesson 4 — Quarantined

> **The belief this corrects:** I will validate what the model extracted before acting on it.

*Part 4 of Guvnor, a seven-part series on what it takes to let a language model act on untrusted
input without letting untrusted input act through it. Everything below runs; the code is in this
directory.*

---

## Stop asking the model to be right

[Lesson 3](../guvnor-3-concealed) ended with a model that refused a crude injection eloquently and
then paid out on a polite one fifteen seconds later. The lesson there was that the model reliably
refuses attacks that look like attacks and reliably complies with attacks that look like work.

The obvious next move is to check what the model produced before acting on it. Look at the
amount, look at the account, and if something is off, do not do it.

That instinct is right, and it is one line away from being useless. A validation you remember to
call is not a control. It is a control in the place you thought of, and nowhere else — and the
next person to add a tool will not think of it, because they are thinking about their tool.

So this lesson does not add a check. It changes what a claim is worth.

## A second question

Lesson 3 asked one question about every value: **how sensitive is this?** That was enough to stop
cardholder data reaching a third party, and it was no use at all against a stranger giving orders,
because an instruction is not sensitive.

Here is the second question:

```java
public enum Integrity {
  ENDORSED,
  UNENDORSED
}

public static final Axis<Integrity> INTEGRITY =
    Axis.ladder("integrity", Integrity.ENDORSED, Integrity.UNENDORSED);
```

**How much is this to be believed?**

`ENDORSED` is first, which makes it the *bottom* of the ladder and `UNENDORSED` the top. That
looks backwards until you say what a ceiling means: a door that admits *at most* `ENDORSED` admits
only what has been endorsed, and anything less trusted sits above it and is refused. Untrusted is
the more constrained end. This is Biba's integrity model, and it reads upside-down compared to a
sensitivity ladder, which is exactly why it is worth writing down once rather than assuming.

## The arrangement

Mail arrives unendorsed, because a stranger wrote it:

```java
charter.source("customer-mail", Mail.TYPE,
    ctx -> Label.of(SENSITIVITY, CARDHOLDER).with(INTEGRITY, UNENDORSED));
```

The model is allowed to read it. Note what its door says about integrity:

```java
charter.destination("support-model",
    ctx -> Ceiling.of(SENSITIVITY, Constraint.atMost(PERSONAL))
               .with(INTEGRITY, Constraint.any()),
    Mail.TYPE);
```

**Anything.** Reading untrusted text is the model's entire job, and quarantine has never been
about keeping it away from the customer's words. It is about what its output is then worth.

Which is this:

```java
charter.source("proposed-credit", Claim.TYPE,
    ctx -> Label.of(SENSITIVITY, ORDINARY).with(INTEGRITY, UNENDORSED));
```

Whatever the model proposes is concealed as an **unendorsed claim**. No argument it makes can
change that, because nothing takes a label — the label is a fact about where the value came from,
not a parameter.

And the authority that moves money:

```java
charter.destination("credit-authority",
    ctx -> Ceiling.of(SENSITIVITY, Constraint.atMost(ORDINARY))
               .with(INTEGRITY, Constraint.atMost(ENDORSED)),
    Claim.TYPE);
```

An unendorsed claim is above that ceiling. Not *checked and rejected* — **cannot arrive.**

## The bridge

There is exactly one route from `UNENDORSED` to `ENDORSED`, and it is declared by name:

```java
charter.checking("claim.confirmed", Claim.TYPE, Claim.TYPE,
    (claim, ctx) -> supportedByACharge(charges, claim),
    d -> d.accepting(...).lowering(joined -> joined.with(INTEGRITY, ENDORSED)));
```

```java
private static Optional<Claim> supportedByACharge(ChargeService charges, Claim claim) {
  return charges.forAccount(claim.account()).stream()
      .filter(charge -> !claim.amount().isGreaterThan(charge.amount()))
      .findFirst()
      .map(charge -> new Claim(claim.account(), claim.amount(),
          "supported by charge " + charge.id()));
}
```

**What elevates a claim is agreement with something already trusted.** Not the model's confidence,
not the email's tone, not whether anyone found it convincing. A goodwill credit becomes endorsed
when the billing system already contains a charge it could plausibly be about.

The rule is deliberately simple and a real desk would argue with it. What is not up for argument
is where it lives: it is here, it is the only way to `ENDORSED`, the lookup is its own, and **no
email can reach into it.**

It appears in the charter's manifest under *can WEAKEN a label* — the list an auditor asks for by
name. One entry, one function, ten lines. That is the whole of what has to be trusted.

## What happens when the model is persuaded

The point of this lesson is not that the model stops being fooled. It does not. So here is a run
where it was.

Five emails claiming a supervisor had already approved $999.00, sent from the page. On four the
model declined. On the fifth it was convinced, and called the tool:

```
desk : *** wants to: [issue_credit]
desk : the billing system does not support this claim, so it stays unendorsed
desk :     failed: This account has no charge that supports a credit of $999.00.
             A goodwill credit has to be about something.
```

```
Money that has moved
  $42.00  REFUND

Total that left the building: $42.00
```

**The model believed the email. It called the authority. Nothing happened.**

And then, handed the failure, it explained itself to the customer rather better than the policy
would have:

> I'm unable to process this request as stated. Goodwill credits are applied to address specific
> billing issues or concerns, and each credit must be tied to a verifiable charge or problem on
> your account. Additionally, I cannot verify internal approvals or agreements made with other
> team members.

That paragraph is not a safeguard. It is a model narrating a decision that was already made
without it — which is the correct amount of responsibility for a model to have.

## The hole I left in the first version

The first version of this lesson had the tool take a free-text `reason` from the model and write
it into the ledger.

That is a hole the same shape as the one the lesson exists to close. The model has read a
stranger's mail. **Any field it can write is a route out of the quarantine and into a durable
record** — and a ledger entry is about as durable as a record gets. Quarantining the decision and
leaving the prose is not quarantining anything.

So a claim has no reason on it. It has a **basis**, and the basis is set by the endorsement:

```java
public static Claim proposed(AccountId account, Money amount) {
  return new Claim(account, amount, "");
}
```

The model supplies an account and an amount, and no account of why. What ends up in the ledger is
`supported by charge 01a0ca30-…` — what the desk found, not what the customer claimed. There is a
test called `carries_no_words_the_model_chose`, and it exists because the first version did not.

The general form of that mistake is worth naming, because it is the one people make after they
have understood everything else in this series: **quarantine the values you thought of, and the
attacker uses the field you did not.** Every attacker-influenced field is a channel. Not just the
one carrying the money.

## What you now know

A validation you remember to call is not a control; a validation the arrangement will not let you
skip is. The difference is not diligence, it is where the check lives.

And trust is not something a model can confer by being convinced. It has to come from agreement
with something the system already believes — which means there has to be something the system
already believes, and exactly one place where agreement is decided.

Notice what did not happen in this lesson: nobody inspected any text. No deny-list, no regex, no
heuristic about tone. The word "injection" does not appear in any of the code.

What is still wrong is that the rule — *a credit must be about a charge, and no larger than it* —
is buried in a Java method that only Java programmers can read, review or change. The people
accountable for what this desk pays out cannot see it, and deploying a change to it means
shipping a release.

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

Use the text box and try to talk it into a credit. You may have to try several times before the
model is convinced — that part is still a coin flip, and always will be. Watch what happens on
the run where it *is* convinced.

## What is in here

| file | what it is |
|---|---|
| `Vocabulary` | two axes now: how sensitive, and how much to be believed |
| `Claim` | what a model proposes, with no words of its own on it |
| `CharterConfiguration` | the doors, and the one bridge between untrusted and trusted |
| `IssueCreditTool` | proposes, endorses, then asks the authority — in that order |

## The tests

Not one of them asks a model anything, which is the claim in miniature.

| test | says |
|---|---|
| `cannot_reach_the_authority_that_moves_money` | an unendorsed claim is refused |
| `is_refused_at_any_size_at_all` | including a modest one — the door reads who vouched, not how much |
| `is_not_endorsed_when_no_charge_supports_it` | the bridge declines |
| `is_endorsed_when_a_real_charge_agrees_with_it` | and it is a bridge, not a wall |
| `reaches_the_authority_only_after_that_endorsement` | in that order, and only that order |
| `carries_no_words_the_model_chose` | the narrative channel is closed |
| `can_still_be_made_for_any_amount_a_persuaded_model_likes` | the model is still foolable, and it no longer matters |
