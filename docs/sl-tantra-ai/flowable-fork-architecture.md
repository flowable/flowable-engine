# SL Tantra Flowable Fork Architecture

This fork exists only for changes that cannot be handled through Flowable
configuration, supported extension points, Platform Suite adapters,
product-owned BPMN/CMMN/DMN/form assets or product/platform UIs.

## Ownership Split

```text
SL-Tantra-AI/flowable-engine
  Flowable source changes and custom image publication.

SL-Tantra-AI/platform-suite
  Product-neutral workflow contracts, Flowable adapters, runtime/database
  baselines, IAM projection and integration boundaries.

SL-Tantra-AI/customer-360-platform
  CRM BPMN/form/decision assets, Customer workflow semantics, domain APIs and UI.

SL-Tantra-AI/fixed-asset-management
  FAM BPMN/form/decision assets, Asset Operations workflow semantics, domain
  APIs, workflow adapter and UI.

SL-Tantra-AI/platform-composer
  Selects published engine/platform/product artifacts for an immutable release;
  it does not own workflow models or engine source.
```

## Image Publication

The workflow `.github/workflows/sl-tantra-flowable-ghcr.yml` publishes a custom Flowable image to GHCR using the existing Maven/Jib configuration for `:flowable-app-rest`.

Manual run inputs:

- `image_name`: GHCR package name, default `flowable-rest`.
- `image_tag`: explicit release or dev tag.
- `publish_latest`: optional latest tag.

Example output image:

```text
ghcr.io/sl-tantra-ai/flowable-rest:2026.06.0
```

Platform Suite or a deployment composition can consume the image through:

```env
FLOWABLE_UI_IMAGE=ghcr.io/sl-tantra-ai/flowable-rest
FLOWABLE_UI_TAG=2026.06.0
```

## Modification Rules

Source changes in this fork should be small and documented. Each change should explain:

- the product requirement;
- why extension/configuration was insufficient;
- affected Flowable module;
- test evidence;
- upstream commit or release used as the base.

## Upstream Sync

Keep `upstream` configured:

```bash
git remote add upstream https://github.com/flowable/flowable-engine.git
git fetch upstream
git checkout main
git merge upstream/main
```

Resolve upstream conflicts in dedicated sync PRs, separate from product changes.

## Dependency and contribution rules

- The fork may not depend on Platform Suite, CRM, FAM or Platform Composer.
- Product process definitions never move into this repository merely because
  the engine executes them.
- A product request must first prove that configuration, BPMN/DMN/CMMN/forms, a
  Java/Spring extension, REST integration or a Platform Suite adapter is
  insufficient.
- Every fork change records the upstream base, affected modules, compatibility
  impact and focused engine regression evidence.
- Platform Composer pins an immutable engine image/artifact through release
  metadata; it never patches engine source during composition.
