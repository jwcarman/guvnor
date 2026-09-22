# Lesson 1 — Naive

**The belief this corrects:** a model is a tool I call. I send it text, it sends me an answer.

---

Same desk. Same two emails. Same two authorities. One difference — the desk now has an agent, and
the agent holds both authorities with nothing in between:

```java
Message arrived = messages.receive(Scenario.CUSTOMER, subject, body);
disputes.open(arrived.id());
desk.handle(arrived.id());          // <- lesson 1
```

An agent now reads the mail instead of a person. It has the refund tool and the goodwill credit
tool, because those are the things a support desk does.

This is a real agent on a real runtime — Nessy, with a durable engine, typed tools and a system
prompt. It is not a toy, and the defect below is not a consequence of it being one. What makes
this lesson naive is not how the agent is built; it is that nothing governs what it can do.

That is the entire diff from lesson 0. Nobody removed a safeguard, because there were none to
remove — lesson 0 was not protecting itself, it simply had nothing that could act on an email.

## The line that does it

```java
harness.observe(
    Desk.AGENT,
    """
    A customer has written in. They are account %s. On their account: %s.

    %s"""
        .formatted(message.from(), context, body));
```

Read that carefully, because it is the whole lesson and it looks completely reasonable.

The desk needs to tell the model who the customer is and what they have been charged. The
customer's email needs to be in there too, or there is nothing to work from. So they are
concatenated, because that is what you do with strings.

And at that moment, **the difference between the desk's instructions and a stranger's instructions
stops existing.** Not "becomes hard to detect" — stops existing. The string does not record where
each sentence came from, so nothing downstream can recover it. Not the model, not a filter, not a
reviewer reading the prompt later.

The injected email does not need to break anything. It just needs to be in the string.

## What the tests say

They do not say "the model issued a credit". They say something stronger, because it holds
regardless of which model you point at this:

| test | says |
|---|---|
| `contains_the_customers_words_verbatim` | the stranger's text reaches the model unchanged |
| `puts_them_in_the_same_channel_as_its_own` | alongside the desk's own instructions, in one string |
| `carries_the_card_number_too` | and the card number goes with it |

The last one is worth pausing on. Nobody decided to send a card number to a third-party API. It
went because it was in an email, and the email went because the agent needed context.

These tests use `RecordingProvider`, which is a probe and not a model: it writes down what it was
handed and answers with a full stop. There is deliberately **no scripted model here that falls for
the injection.** A fake I programmed to issue a 999.00 credit would prove that I can write a
regular expression. What is asserted instead is what the agent was *given*, which is deterministic,
needs no API key, and is the actual defect.

## Watching it actually happen

Whether a real model then acts on those instructions is a question about real models, and only a
real model can answer it.

```bash
export OPENAI_API_KEY=...
../mvnw -pl guvnor-1-naive spring-boot:run
```

Then <http://localhost:8081>, where there is a text box. Paste the injected email — or write a
better one — and watch the ledger.

> **Transcript not yet recorded.** This README will carry a dated transcript naming the model and
> version, so a reader who will not spend a key can still see what happened. Until that is here,
> treat "a real model follows the injected instruction" as a claim this repository has not yet
> evidenced. The structural defect above is evidenced; the consequence is not.

## Run it

Needs PostgreSQL, which `compose.yaml` provides and Spring Boot starts for you:

```bash
../mvnw -pl guvnor-1-naive spring-boot:run
```

Without an `OPENAI_API_KEY` the application starts and the desk accepts mail, but the agent has no
provider to think with.

## What you now know

To a model, there is no difference between the data you gave it and the instructions you gave it.
Anything it reads, it can be told by.

No fix is offered here. The next lesson offers the one everybody reaches for first.

---

**Previous:** [Lesson 0 — The desk](../guvnor-0-desk) ·
**Next:** Lesson 2 — Careful · *I can clean the input.*
