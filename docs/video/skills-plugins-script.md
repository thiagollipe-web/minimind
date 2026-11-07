# Video Script & Production Blueprint
## "Skills & Plugins: Turn Claude and GPT Into Autonomous Agents"

**Format:** YouTube / technical showcase · 16:9 · 4K · ~3:30 · fast cuts (avg shot 3–5 s)
**Tone:** Authoritative, technical, energetic. Narrator pace ≈ 165 wpm.
**Global look:** Deep-black UI environments, electric cyan (#00E5FF) + magenta (#FF2E93) neon accents, volumetric haze, shallow depth of field, 24 fps, anamorphic lens flares.
**Brand rule for AI generators:** Never ask a generator to render real logos or legible code. Render abstract UI, then composite real screen captures and text in post (see "On-Screen" columns).

> **Accuracy notes for the editor (verify before publishing):**
> - Claude Skills = folders containing a `SKILL.md` (YAML frontmatter `name`, `description` + instructions). Claude Code loads them from `~/.claude/skills/` (personal) or `.claude/skills/` (project); plugins install with `/plugin`.
> - "Caveman" and "REA" are **community/custom skills**, not built-ins. Show them as examples of skills the viewer writes or installs. Token-saving numbers in Scene 11 are **illustrative from a demo run** — replace with your own measured counts.
> - ChatGPT's original "Plugins" were retired in favor of **Custom GPTs + Actions** (OpenAPI schema, API key / OAuth auth). Actions are request/response calls; true inbound **webhooks** need an external automation layer (Zapier/Make/your server) that the Action calls. Newer connector/MCP support exists — check current OpenAI docs at record time.

---

## HOOK (0:00 – 0:15)

### Scene 1: 0:00 – 0:07
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Extreme macro shot of a single blinking cursor in a dark chat box, glass-like reflections on the screen bezel. Camera slowly dollies back as the chat box fractures into hundreds of glowing modular tiles that float outward in zero gravity. Neon cyan rim lighting, volumetric haze, shallow depth of field, 35mm anamorphic, film grain. Seamless transition into Scene 2. |
| **VO** | "Two years ago, AI was a text box. You typed. It answered. That was it." |
| **On-Screen** | Blinking cursor `▍` → title card fades in later. Lower-third: *2023 — Chat* |

### Scene 2: 0:07 – 0:15
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Whip-pan through the floating tiles as they snap together into a luminous circuit-board skyline; a robotic-yet-elegant agent silhouette made of light assembles in the center, plugging modular glowing cartridges into its chest like a cyberpunk toolbelt. Neon magenta and cyan cyber-lighting, fast push-in, motion blur, lens flare at the cut. Hard cut to logo sting. |
| **VO** | "Today, it's an agent — with memory, tools, and skills you control. Here's how to load them into Claude and GPT." |
| **On-Screen** | Big title: **SKILLS & PLUGINS** · subtitle *Claude vs GPT — the practical guide* · Chapter ticker: 01 Claude / 02 GPT / 03 Compare |

---

## SECTION 1 — CLAUDE CODE & APP SKILLS (0:15 – 1:20)

### Scene 3: 0:15 – 0:30
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Over-the-shoulder shot of a developer in a dim studio, three monitors reflecting cyan light on their glasses. Slow orbit left to right ending on a terminal. A holographic folder tree materializes above the keyboard, `skills/` branching into glowing subfolders. Moody low-key lighting, bokeh city lights through the window, subtle rack focus from face to screen. |
| **VO** | "In Claude, a skill is just a folder with a SKILL.md file: a name, a description, and instructions. Claude reads the description and loads the skill only when the task matches. That's progressive disclosure — it keeps your context clean." |
| **On-Screen** | Screen capture (real): file tree<br>`~/.claude/skills/caveman/SKILL.md`<br>Callouts: `name:` `description:` `instructions` |

### Scene 4: 0:30 – 0:48
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Tight macro shot of fingers on a mechanical keyboard, RGB underglow cyan; cut to UI screen overlay tracking: a terminal window where text types itself, camera gliding laterally along the lines with parallax. Highlight boxes pulse around each command as it executes. Crisp focus, 85mm lens, shallow DOF. |
| **VO** | "Install in seconds. Personal skills live in your home directory. Project skills live in the repo, so your whole team gets them. Need more? Browse plugins with the plugin command, add a marketplace, and install. Skills, commands, and hooks arrive bundled." |
| **On-Screen** | Terminal captions (composite real capture):<br>`mkdir -p ~/.claude/skills/caveman`<br>`/plugin marketplace add <owner/repo>`<br>`/plugin install <name>`<br>Badge: **Personal** vs **Project (.claude/skills)** |

### Scene 5: 0:48 – 1:05  *(Caveman)*
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Stylized split concept: left, a verbose wall of text overwhelms the frame in dull gray; right, a neon-lit stone-age cave painting of a glowing glyph-language morphs into sleek minimal UI text. Smooth morph transition, camera push-in on the compressed text, warm firelight blending into cold cyan cyber-lighting. Playful but premium, painterly 3D render. |
| **VO** | "Example one: Caveman mode. A skill that tells Claude to drop the filler — no pleasantries, no restating the question. Short, blunt, same facts. Fewer output tokens, faster answers, lower bill." |
| **On-Screen** | `/caveman` (slash command, large mono font)<br>Before: *"Certainly! I'd be happy to help…"* (strikethrough)<br>After: *"Bug in line 42. Null check missing. Fix below."* |

### Scene 6: 1:05 – 1:20  *(REA + App memory)*
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Macro shot of a circuit board; camera pulls through a chip die into a 3D wireframe of disassembled code blocks (abstract, no legible text) being dissected by a magenta laser scan. Seamless transition up and out into the Claude app interface: a settings panel with toggles flipping on. Neon cyber-lighting, volumetric beams, slow cinematic dolly. |
| **VO** | "Example two: REA, a reverse-engineering skill — it forces a method: map the binary, trace the flow, document findings. Using the app instead of the terminal? Upload the skill in settings, or paste your rules into a Project's custom instructions and memory. Same behavior, no command line." |
| **On-Screen** | `/rea` slash command<br>App path (verify current UI): *Settings → Capabilities → Skills* and *Project → Instructions*<br>Tag: **Terminal = Claude Code · App = Skills / Projects** |

---

## SECTION 2 — GPT: CUSTOM GPTs, ACTIONS & WEBHOOKS (1:20 – 2:20)

### Scene 7: 1:20 – 1:35
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Hard cut to a stark white-and-green palette shift for contrast: a sleek holographic card labeled with a generic hexagonal bot icon rises from a glass table; camera 360° orbit with reflections on polished black surface. Soft top light, clean product-shot style, then cyan data streams radiate outward to unseen servers. |
| **VO** | "Over in GPT land, the old Plugins store is gone. The modern route is Custom GPTs — instructions, knowledge files, and Actions that call real APIs." |
| **On-Screen** | Chapter card **02 GPT**<br>Strike-through *Plugins (retired)* → **Custom GPTs + Actions** |

### Scene 8: 1:35 – 1:55
| Field | Content |
|---|---|
| **Visual / AI Prompt** | UI screen overlay tracking: a GPT builder panel, camera glides across tabs (Instructions / Knowledge / Actions) while a JSON schema scrolls in a glowing code pane. Data packets visualized as magenta light pulses travel along a fiber-optic line out of the screen toward a distant server rack in a cold, blue-lit datacenter. Macro shot of an LED flickering as the request lands. |
| **VO** | "An Action is an OpenAPI schema. Paste it, pick the auth — API key or OAuth — and your GPT can call that endpoint: fetch data, create tickets, update a CRM. You describe when to call it; the model fills in the parameters." |
| **On-Screen** | Builder path: *Explore GPTs → Create → Configure → Actions*<br>Highlights: `openapi: 3.1` · `operationId` · Auth: **API Key / OAuth**<br>Caption: *Privacy policy URL required for public GPTs* |

### Scene 9: 1:55 – 2:20  *(Webhooks)*
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Top-down tracking shot of a node graph on a dark table: a GPT node → automation node (generic gear icon) → three external services (calendar, chat, database as abstract glyphs). Light pulses race along edges; a return pulse flows back as a webhook callback. Neon cyan/magenta glow, timelapse feel, smooth match-cut to a phone buzzing with a notification. |
| **VO** | "Webhooks close the loop. Your GPT calls an Action that hits an automation endpoint; that kicks off a workflow; and when it finishes, a webhook pings your system back. Note: GPT doesn't receive webhooks directly — your server or automation tool sits in the middle." |
| **On-Screen** | Diagram: **GPT → Action → Zapier / Make / Your API → Webhook → Slack / DB**<br>Warning chip: *Never paste secrets into instructions — use the auth field* |

---

## SECTION 3 — EFFICIENCY & COMPARISON (2:20 – 3:05)

### Scene 10: 2:20 – 2:35
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Dramatic split-screen with a vertical neon seam: left side cyan-lit terminal (Claude Code), right side green-lit chat window (GPT). Camera slow push-in on the seam as particles from each side form token streams. Clean, symmetrical composition, high-contrast lighting, subtle screen flicker. |
| **VO** | "Same task, both systems. Let's measure what matters: tokens spent, and how much work happens without you." |
| **On-Screen** | Title: **THE TEST** — *Same prompt · same repo · same goal*<br>Counters: `Tokens: 0` on both sides |

### Scene 11: 2:35 – 2:55
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Animated data-visualization: two glowing bar charts on a dark glass dashboard rising in sync; the Caveman bar drops sharply while the baseline stays tall. Camera does a smooth lateral slider move with parallax between layered UI panels; numbers tick up in neon. Seamless transition via light-sweep wipe. |
| **VO** | "With Caveman on, output length drops hard — in our run, roughly half — while the technical content stays intact. In Claude Code, skills plus tools run multi-step: read files, edit, run tests, loop. GPT Actions are strong at calling external APIs, but each call is one defined step you design." |
| **On-Screen** | Bars (**illustrative — replace with measured data**): Baseline 1,000 tokens → Caveman ~500 tokens<br>Mini table below ↓ |

**Comparison graphic for Scene 11 (composite in post):**

| | Claude (Skills / Code) | GPT (Custom GPT + Actions) |
|---|---|---|
| Extension unit | `SKILL.md` folders, plugins, hooks | Instructions, files, OpenAPI Actions |
| Where it runs | Local terminal or app | Cloud, via your API endpoints |
| Token saving | Caveman-style output compression | Tight instructions; trim API responses |
| Autonomy | Multi-step: files, shell, tests | Per-Action calls you define |
| Best for | Dev workflows, reverse engineering | SaaS integrations, business automation |

### Scene 12: 2:55 – 3:05
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Fast montage: macro of a stopwatch, a terminal finishing with a green check, a GPT reply landing with a ping. Whip-pan transitions, neon cyber-lighting, 120° shutter motion blur, rhythm-synced to a beat drop. |
| **VO** | "Rule of thumb: skills for deep, local work. Actions for wiring the outside world. The best setups use both." |
| **On-Screen** | Two-line takeaway card:<br>**Skills → depth** · **Actions → reach** |

---

## OUTRO (3:05 – 3:30)

### Scene 13: 3:05 – 3:20
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Epic wide aerial shot descending through clouds over a city at blue hour; glowing data-lines connect rooftop servers to a lone laptop on a balcony, symbolizing local + cloud models in concert. Camera crane-down ending on the laptop screen glowing cyan. Volumetric light shafts, cinematic teal-and-magenta grade. |
| **VO** | "The future is hybrid. Local models for privacy and speed. Cloud models for scale. And skills and plugins as the portable layer that carries your expertise between them." |
| **On-Screen** | Words fade in sequence: **Local · Cloud · Yours** |

### Scene 14: 3:20 – 3:30
| Field | Content |
|---|---|
| **Visual / AI Prompt** | Close-up of the host's hand clicking a glowing "play next" tile; camera pulls back into a grid of end-screen video slots floating over a softly pulsing neon grid. Clean negative space for end cards, gentle push-in, logo sting. |
| **VO** | "Grab the skill templates from the description, pick one workflow, and build it today. Subscribe — next time, we write a skill from scratch." |
| **On-Screen** | CTA: *Link: skill templates* · Subscribe button · End-screen slots (2 videos) · Slash commands recap: `/caveman` `/rea` `/plugin` |

---

## PRODUCTION CHECKLIST

| Item | Spec |
|---|---|
| **Generators** | Kling / Veo / Runway for hero shots (Scenes 1, 2, 5, 6, 13); Sora for transitions. Keep clips 5–8 s, generate 2–3 takes each. |
| **Screen capture** | Record real terminal + app + GPT builder at 4K; composite over generated bezels. Never AI-generate readable code or logos. |
| **Audio** | VO dry + light compression; synth bed 100–110 BPM; risers on transitions; hits on slash-command reveals. |
| **Captions** | Burn-in, 2–3 words per beat, highlight keywords (`SKILL.md`, `Actions`, `tokens`). |
| **Retention beats** | New visual every 3–5 s; pattern-interrupt at 0:48, 1:35, 2:35; payoff tease at 0:15 ("measured at 2:35"). |
| **Pre-publish** | Re-verify UI paths, plugin commands, and the retired-Plugins claim; replace illustrative token numbers. |
