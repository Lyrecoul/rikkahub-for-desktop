# Model Discovery Sources

Last verified: 2026-08-07

RikkaHub must not ship a static catalog as the source of truth for current models. Availability varies by account, region, rollout, and gateway routing. The application discovers models from the configured provider when the user chooses **Test and fetch models**; the raw ID remains the request value.

## Official API Evidence

| Provider / protocol | Official source | Discovery rule |
| --- | --- | --- |
| OpenAI-compatible APIs | [OpenAI Models API](https://platform.openai.com/docs/api-reference/models/list) | Query the configured `GET /models` endpoint. The standard response guarantees an `id`; compatible providers may additionally return a display-name field. |
| Anthropic Messages API | [Anthropic Models API](https://platform.claude.com/docs/en/api/models/list) | Query the configured `GET /models` endpoint using the existing Anthropic auth and version headers. Anthropic documents this endpoint specifically to determine currently available models. |
| Google Gemini API | [Gemini Models API](https://ai.google.dev/api/models) | Query `GET /models`. Gemini model resources provide `name`, `displayName`, and supported generation methods; only `generateContent` models belong in the chat picker. |
| OpenRouter | [OpenRouter model listing API](https://openrouter.ai/docs/api/api-reference/models/list-all-models-and-their-properties) | Query its configured `GET /models` endpoint. Its response includes model metadata and changes with the live catalog. |
| xAI | [xAI Models documentation](https://docs.x.ai/developers/models) | Treat the configured account's available model list as authoritative. The public documentation is useful for capabilities but is not a substitute for account availability. |
| DeepSeek | [DeepSeek model documentation and pricing](https://api-docs.deepseek.com/quick_start/pricing/) | Query the configured `GET /models` endpoint. DeepSeek's documented model aliases can change independently of a client release. |
| Alibaba Model Studio / Qwen | [Model Studio model documentation](https://help.aliyun.com/zh/model-studio/models) | Query the configured compatible-mode `GET /models` endpoint. Availability can vary by region and account. |

## Implementation Policy

- Retain every model ID returned by the live provider for requests and configuration.
- Persist a provider-returned `display_name`, `displayName`, or `name` alongside the ID when available.
- Prefer that official returned name in the picker and settings. Use the local ID formatter only when a provider returns no name.
- Do not add a hard-coded catalog of supposedly current model IDs. It would become stale and can hide models actually enabled for a user.
- Gateways and aggregators, including RikkaHub, AiHubMix, SiliconFlow, Vercel AI Gateway, TokenPony, 302.AI, and AckAI, must always use their live account-scoped `/models` response rather than upstream vendor documentation.
