# SL Tantra Flowable Fork Architecture

This fork exists only for changes that cannot be handled through Flowable configuration, BPMN assets, REST integration, or a wrapper UI in `SL-Tantra-AI/make-life-easy`.

## Ownership Split

```text
SL-Tantra-AI/flowable-engine
  Flowable source changes and custom image publication.

SL-Tantra-AI/make-life-easy
  Workflow architecture, BPMN/form/decision assets, workflow gateway, domain APIs, and product UI.
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

The platform repo can consume the image through:

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
