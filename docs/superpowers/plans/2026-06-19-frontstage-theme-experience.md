# Frontstage Theme Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the A module owned by the frontstage/theme developer: home page, article detail, project detail, inspiration wall, about page, and theme switching showcase.

**Architecture:** Keep the current Vue 3 + Vite frontstage routes and public API service layer. Improve only public-facing pages and shared theme/application helpers; do not take over CMS, auth/permission logic, backend migrations, or AI/data dashboard work owned by other groups.

**Tech Stack:** Vue 3, TypeScript, Vite 8, Vue Router, Pinia, GSAP, Lenis, Three.js, markdown-it, DOMPurify, highlight.js, @lucide/vue.

---

### Task 1: Scope Lock And Baseline Check

**Files:**
- Read: `工作分配.md`
- Read: `需求文档.md`
- Read: `README.md`
- Read: `frontend/package.json`
- Read: `frontend/src/app/router.ts`

- [ ] **Step 1: Confirm A module boundaries**

Use `工作分配.md` as the team split source. A module covers:

- 首页
- 文章详情
- 作品详情
- 创意墙
- 关于页
- 主题切换展示

Do not implement B module CMS forms, C module login/comment/permission ownership, or D module AI/search/statistics dashboard unless an A page only needs to display already available public data.

- [ ] **Step 2: Confirm frontstage routes**

Check `frontend/src/app/router.ts` and keep these route responsibilities:

- `/`: `HomePage.vue`
- `/articles/:slug`: `ArticleDetailPage.vue`
- `/projects/:slug`: `ProjectDetailPage.vue`
- `/inspirations`: `InspirationsPage.vue`
- `/about`: `AboutPage.vue`

- [ ] **Step 3: Run baseline build before editing**

Run:

```powershell
npm run build --prefix frontend
```

Expected: `vue-tsc --noEmit` and `vite build` both complete with exit code 0.

### Task 2: Home Page Real Content And Navigation

**Files:**
- Modify: `frontend/src/pages/HomePage.vue`
- Modify: `frontend/src/content/home.ts`
- Use existing APIs from: `frontend/src/services/content.ts`
- Optional style adjustments: `frontend/src/styles/home.css`

- [ ] **Step 1: Replace non-clickable demo anchors with real route links**

Make the featured article cards navigate to `/articles/{slug}` and project cards navigate to `/projects/{slug}`. If a card still uses static demo content, ensure its slug matches the fallback data in `frontend/src/content/studio.ts`.

- [ ] **Step 2: Prefer public API data where available**

Use existing service functions instead of adding new endpoints:

- `fetchArticles()` for featured writing
- `fetchProjects()` for portfolio gallery
- `fetchInspirations({ pageSize: 8 })` for creative wall preview
- `fetchCurrentTheme()` for active theme display if the section needs the current backend theme

Keep `frontend/src/content/home.ts` as fallback content only, so the home page still renders when the backend is unavailable.

- [ ] **Step 3: Keep the immersive experience accessible**

Preserve the existing reduced-motion behavior in `HeroWebGLScene`, `useLenis`, and GSAP sections. Do not make animation required for reading content.

- [ ] **Step 4: Verify home page behavior**

Run:

```powershell
npm run build --prefix frontend
```

Then manually check:

- 首页首屏能看到站点气质、文章入口、作品入口、登录/注册入口
- 首页文章卡点击进入文章详情
- 首页作品卡点击进入作品详情
- 后端不可用时仍显示本地样例和不阻断页面渲染

### Task 3: Article Detail Reading Experience

**Files:**
- Modify: `frontend/src/pages/ArticleDetailPage.vue`
- Modify only if needed: `frontend/src/styles/base.css`
- Reuse: `frontend/src/shared/markdown.ts`
- Reuse: `frontend/src/services/content.ts`

- [ ] **Step 1: Keep data loading on the existing API path**

Continue using `fetchArticleBySlug(slug)` from `frontend/src/services/content.ts`. Do not hardcode article detail data in the page except as fallback through `fallbackArticles`.

- [ ] **Step 2: Tighten reading presentation**

Ensure the detail page consistently shows:

- back link to article archive
- cover/gradient hero
- title, summary, publish date, category, tags
- safe Markdown rendering through `renderSafeMarkdown`
- generated second-level heading TOC
- loading, empty, and backend-unavailable states

- [ ] **Step 3: Keep comments as display-level integration**

It is acceptable for A module to display the comment form and public comments because they are part of the page experience. Do not implement review, sensitive-word filtering, multi-level reply ownership, or permission rules; those belong to C.

- [ ] **Step 4: Verify article detail**

Run:

```powershell
npm run build --prefix frontend
```

Manual checks:

- `/articles/demo-article-creator-hub` renders with fallback data if backend is unavailable
- Markdown code blocks are escaped/highlighted
- TOC scrolls to `##` headings
- unauthenticated comment button clearly indicates login requirement

### Task 4: Project Detail Exhibition Experience

**Files:**
- Modify: `frontend/src/pages/ProjectDetailPage.vue`
- Modify only if needed: `frontend/src/styles/base.css`
- Reuse: `frontend/src/shared/markdown.ts`
- Reuse: `frontend/src/services/content.ts`

- [ ] **Step 1: Keep detail data on the existing project API**

Continue using `fetchProjectBySlug(slug)` and fallback through `fallbackProjects`.

- [ ] **Step 2: Make the page feel like a work record**

Ensure the detail page clearly presents:

- cover image or cover gradient
- project type/status/recommended marker
- description
- tech stack
- safe GitHub/Demo external links through `safeExternalUrl`
- Markdown process/creation notes
- process timeline based on available project fields

- [ ] **Step 3: Avoid inventing missing backend fields**

If screenshots, gallery images, or rich timeline fields are not present in `ProjectSummary`, do not fake new API fields in the frontend. Use Markdown content and existing fields first; record richer gallery fields as a follow-up for B/backend coordination.

- [ ] **Step 4: Verify project detail**

Run:

```powershell
npm run build --prefix frontend
```

Manual checks:

- `/projects/demo-project-immersive-homepage` renders with fallback data if backend is unavailable
- unsafe external links are hidden
- mobile layout stacks without overlapping cover/title/actions

### Task 5: Inspiration Wall Frontstage

**Files:**
- Modify: `frontend/src/pages/InspirationsPage.vue`
- Modify only if needed: `frontend/src/styles/base.css`
- Reuse: `frontend/src/services/content.ts`
- Reuse fallback data from: `frontend/src/content/studio.ts`

- [ ] **Step 1: Keep filter behavior aligned with backend contract**

Use the existing `InspirationType | 'ALL'` filter values:

- `ALL`
- `TEXT`
- `PROMPT`
- `IMAGE`
- `CODE`
- `LINK`

Call `fetchInspirations({ keyword, type, pageSize: 30 })`.

- [ ] **Step 2: Keep source links safe**

Continue using `safeSource()` so only `http:` and `https:` source URLs render as external links.

- [ ] **Step 3: Improve masonry readability**

Keep the visual wall feel, but verify cards do not overlap, long code/text wraps safely, and empty/loading states remain visible on mobile.

- [ ] **Step 4: Verify inspiration wall**

Run:

```powershell
npm run build --prefix frontend
```

Manual checks:

- `/inspirations` loads cards
- type filters update the card list
- keyword search filters backend or fallback data
- one-column mobile masonry works at narrow width

### Task 6: About Page From Site Config

**Files:**
- Modify: `frontend/src/pages/AboutPage.vue`
- Modify only if needed: `frontend/src/styles/base.css`
- Reuse: `frontend/src/services/content.ts`

- [ ] **Step 1: Keep about page configurable**

Continue loading public site config through `fetchSiteConfig()`.

Expected config keys already used by the page:

- `site.profile.active`
- `site.socialLinks`

- [ ] **Step 2: Keep safe fallback copy**

If the backend config is unavailable, keep a polished local fallback. Avoid exposing private personal details in hardcoded copy.

- [ ] **Step 3: Validate contact links**

Keep `safeContactUrl()` behavior:

- allow `mailto:`
- allow `http:`
- allow `https:`
- reject other protocols

- [ ] **Step 4: Verify about page**

Run:

```powershell
npm run build --prefix frontend
```

Manual checks:

- `/about` renders with config or fallback
- social/contact links are safe
- skill cloud wraps on mobile

### Task 7: Theme Switching Showcase And Global Theme Application

**Files:**
- Modify: `frontend/src/pages/HomePage.vue`
- Modify: `frontend/src/styles/base.css`
- Modify: `frontend/src/app/App.vue`
- Create if needed: `frontend/src/shared/theme.ts`
- Reuse: `fetchCurrentTheme()` from `frontend/src/services/content.ts`

- [ ] **Step 1: Keep the homepage theme preview**

The existing `ThemeUniverse` section can remain a preview/展示 component. Its job is to let visitors see different theme moods without requiring admin permissions.

- [ ] **Step 2: Apply active backend theme to shared CSS variables**

Create a small helper only if needed, for example `frontend/src/shared/theme.ts`, that maps `ThemeConfig` fields to existing CSS variables:

- `primaryColor` -> `--tone-primary` and `--md-sys-color-primary`
- `fontFamily` -> root `font-family` or a dedicated variable
- `cardStyle` -> card radius/shadow variables
- `layoutType` -> optional body class or data attribute
- `config` -> optional supported keys only, with runtime guards

Do not blindly write unknown config keys into CSS.

- [ ] **Step 3: Load theme once near app startup**

Use `App.vue` or a small composable so the public shell and detail pages share the same active theme. If the API fails, keep the existing default variables from `base.css`.

- [ ] **Step 4: Verify theme behavior**

Run:

```powershell
npm run build --prefix frontend
```

Manual checks:

- active backend theme changes visible primary color/font/card mood
- home page preview still switches locally
- page remains readable when theme API fails
- no layout jump or unreadable contrast on detail/about/inspiration pages

### Task 8: Final Frontstage QA

**Files:**
- Review all A module files changed in this plan.

- [ ] **Step 1: Run final checks**

Run:

```powershell
git status --short --branch
npm run build --prefix frontend
```

Expected:

- working branch is `codex/CreatorSpace`
- only A module files and planned docs are changed
- build exits 0

- [ ] **Step 2: Manual route smoke test**

Start the frontend if needed:

```powershell
npm run dev --prefix frontend
```

Check these routes in browser:

- `/`
- `/articles/demo-article-creator-hub`
- `/projects/demo-project-immersive-homepage`
- `/inspirations`
- `/about`

- [ ] **Step 3: Independent review**

Dispatch an independent reviewer to check:

- A scope is satisfied
- B/C/D-owned work was not taken over
- theme helper does not write unsafe CSS from arbitrary config
- Markdown remains sanitized
- external URLs remain protocol-filtered
- responsive layout does not overlap on mobile
- TypeScript does not use unnecessary `any`

- [ ] **Step 4: Commit when clean**

Run:

```powershell
git add frontend/src/pages/HomePage.vue frontend/src/pages/ArticleDetailPage.vue frontend/src/pages/ProjectDetailPage.vue frontend/src/pages/InspirationsPage.vue frontend/src/pages/AboutPage.vue frontend/src/styles/base.css frontend/src/styles/home.css frontend/src/app/App.vue frontend/src/shared/theme.ts docs/superpowers/plans/2026-06-19-frontstage-theme-experience.md
git commit -m "完善前台展示与主题体验"
```

Only include files that actually changed. Do not stage `.env`, generated logs, unrelated backend files, or changes from other teammates.
