<template>
<!-- 后台管理表格占位框架面 -->
<!-- 后台各项业务管理的整合看板组件 -->
  <section ref="root" class="admin-module">
    <header class="module-hero" data-reveal>
      <div>
        <h2>{{ moduleConfig.title }}</h2>
        <p>{{ moduleConfig.description }}</p>
      </div>
      <button class="button button-filled" type="button" @click="handlePrimaryAction">
        <Plus :size="16" />
        {{ moduleConfig.primaryAction }}
      </button>
    </header>

    <section v-if="activeSection === 'articles'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveArticle">
        <div class="panel-title">
          <h2>{{ editingArticleId ? '编辑文章' : '新建文章' }}</h2>
          <span>{{ privacyLabel(articleForm.privacyType) }}</span>
        </div>
        <div class="form-line">
          <label>
            标题
            <input v-model="articleForm.title" maxlength="200" />
          </label>
          <label>
            URL 标识
            <input v-model="articleForm.slug" maxlength="220" />
          </label>
        </div>
        <div class="form-line">
          <label>
            分类
            <select v-model="articleForm.categoryId">
              <option :value="null">不绑定分类</option>
              <option v-for="category in articleCategories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
          </label>
          <label>
            可见性
            <select v-model="articleForm.privacyType">
              <option v-for="privacy in articlePrivacies" :key="privacy" :value="privacy">
                {{ privacyLabel(privacy) }}
              </option>
            </select>
          </label>
        </div>
        <label>
          摘要
          <textarea v-model="articleForm.summary" rows="3" maxlength="1200" />
        </label>
        <label>
          封面地址
          <input v-model="articleForm.coverUrl" placeholder="/uploads/article/cover.png" />
        </label>
        <label>
          Markdown 正文
          <textarea v-model="articleForm.contentMarkdown" rows="8" />
        </label>
        <div class="tag-picker">
          <label v-for="tag in tags" :key="tag.id" class="check-line">
            <input v-model="articleForm.tagIds" type="checkbox" :value="tag.id" />
            {{ tag.name }}
          </label>
        </div>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingArticleId ? '保存文章' : '创建草稿' }}</button>
          <button v-if="editingArticleId" class="button button-tonal" type="button" @click="resetArticleForm">取消编辑</button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>文章队列</h2>
          <span>共 {{ articles.length }} 篇</span>
        </div>
        <div class="filter-bar">
          <input v-model="articleKeyword" placeholder="搜索文章" @keyup.enter="loadArticles" />
          <select v-model="articleStatus" @change="loadArticles">
            <option value="ALL">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="PENDING_REVIEW">待审核</option>
            <option value="PUBLISHED">公开</option>
            <option value="PRIVATE">私密</option>
            <option value="REJECTED">已驳回</option>
            <option value="ARCHIVED">归档</option>
          </select>
          <button class="icon-text-button" type="button" @click="loadArticles">刷新</button>
        </div>
        <article v-for="article in articles" :key="article.id" class="table-row table-row--rich">
          <div>
            <strong>{{ article.title }}</strong>
            <span>
              {{ article.category?.name ?? '未分类' }} · {{ privacyLabel(article.privacyType) }} · {{ article.slug }}
            </span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ contentStatusLabel(article.status) }}</span>
            <button class="icon-text-button" type="button" @click="editArticle(article.id)">编辑</button>
            <button
              v-if="article.status === 'PENDING_REVIEW'"
              class="icon-text-button"
              type="button"
              @click="approveArticleReview(article)"
            >
              通过
            </button>
            <button
              v-if="article.status === 'PENDING_REVIEW'"
              class="icon-text-button danger"
              type="button"
              @click="rejectArticleReview(article)"
            >
              驳回
            </button>
            <button class="icon-text-button" type="button" @click="toggleArticlePublish(article)">
              {{ articlePublishActionLabel(article) }}
            </button>
            <button class="icon-text-button" type="button" @click="toggleArticleTop(article)">
              {{ article.top ? '取消置顶' : '置顶' }}
            </button>
            <button class="icon-text-button" type="button" @click="toggleArticleRecommend(article)">
              {{ article.recommended ? '取消推荐' : '推荐' }}
            </button>
            <button class="icon-text-button danger" type="button" @click="removeArticle(article.id)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'projects'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveProject">
        <div class="panel-title">
          <h2>{{ editingProjectId ? '编辑作品' : '新增作品' }}</h2>
          <span>{{ projectTypeLabel(projectForm.projectType) }}</span>
        </div>
        <div class="form-line">
          <label>
            标题
            <input v-model="projectForm.title" maxlength="200" />
          </label>
          <label>
            URL 标识
            <input v-model="projectForm.slug" maxlength="220" />
          </label>
        </div>
        <div class="form-line">
          <label>
            类型
            <input v-model="projectForm.projectType" maxlength="60" placeholder="例如：Web 应用" />
          </label>
          <label>
            技术栈
            <input v-model="projectTechStack" placeholder="Vue 3, Spring Boot" />
          </label>
        </div>
        <label>
          描述
          <textarea v-model="projectForm.description" rows="3" maxlength="2000" />
        </label>
        <label>
          封面地址
          <input v-model="projectForm.coverUrl" placeholder="/uploads/project/cover.png" />
        </label>
        <div class="form-line">
          <label>
            GitHub
            <input v-model="projectForm.githubUrl" placeholder="https://github.com/..." />
          </label>
          <label>
            Demo
            <input v-model="projectForm.demoUrl" placeholder="https://demo.example.com" />
          </label>
        </div>
        <label>
          Markdown 详情
          <textarea v-model="projectForm.contentMarkdown" rows="8" />
        </label>
        <div class="tag-picker">
          <label v-for="tag in tags" :key="tag.id" class="check-line">
            <input v-model="projectForm.tagIds" type="checkbox" :value="tag.id" />
            {{ tag.name }}
          </label>
        </div>
        <label class="check-line">
          <input v-model="projectForm.recommended" type="checkbox" />
          推荐展示
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingProjectId ? '保存作品' : '创建作品' }}</button>
          <button v-if="editingProjectId" class="button button-tonal" type="button" @click="resetProjectForm">取消编辑</button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>作品队列</h2>
          <span>共 {{ projects.length }} 个</span>
        </div>
        <div class="filter-bar">
          <input v-model="projectKeyword" placeholder="搜索作品" @keyup.enter="loadProjects" />
          <select v-model="projectStatus" @change="loadProjects">
            <option value="ALL">全部状态</option>
            <option value="DRAFT">草稿</option>
            <option value="PENDING_REVIEW">待审核</option>
            <option value="VISIBLE">展示</option>
            <option value="HIDDEN">隐藏</option>
            <option value="REJECTED">已驳回</option>
            <option value="ARCHIVED">归档</option>
          </select>
          <button class="icon-text-button" type="button" @click="loadProjects">刷新</button>
        </div>
        <article v-for="project in projects" :key="project.id" class="table-row table-row--rich">
          <div>
            <strong>{{ project.title }}</strong>
            <span>{{ projectTypeLabel(project.projectType) }} · {{ project.techStack.join(' / ') || '未维护技术栈' }} · {{ project.slug }}</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ projectStatusLabel(project.status) }}</span>
            <button class="icon-text-button" type="button" @click="editProject(project.id)">编辑</button>
            <button
              v-if="project.status === 'PENDING_REVIEW'"
              class="icon-text-button"
              type="button"
              @click="approveProjectReview(project)"
            >
              通过
            </button>
            <button
              v-if="project.status === 'PENDING_REVIEW'"
              class="icon-text-button danger"
              type="button"
              @click="rejectProjectReview(project)"
            >
              驳回
            </button>
            <button class="icon-text-button" type="button" @click="toggleProjectVisible(project)">
              {{ project.status === 'VISIBLE' ? '隐藏' : '展示' }}
            </button>
            <button class="icon-text-button" type="button" @click="toggleProjectRecommend(project)">
              {{ project.recommended ? '取消推荐' : '推荐' }}
            </button>
            <button class="icon-text-button danger" type="button" @click="removeProject(project.id)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'inspirations'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveInspiration">
        <div class="panel-title">
          <h2>{{ editingInspirationId ? '编辑灵感卡片' : '添加灵感卡片' }}</h2>
          <span>{{ inspirationForm.isPublic ? '公开' : '私密' }}</span>
        </div>
        <label>
          标题
          <input v-model="inspirationForm.title" maxlength="200" />
        </label>
        <label>
          类型
          <select v-model="inspirationForm.cardType">
            <option v-for="type in inspirationTypes" :key="type" :value="type">
              {{ inspirationTypeLabel(type) }}
            </option>
          </select>
        </label>
        <label>
          内容
          <textarea v-model="inspirationForm.content" rows="5" maxlength="5000" />
        </label>
        <label>
          来源链接
          <input v-model="inspirationForm.sourceUrl" placeholder="https://example.com/reference" />
        </label>
        <div class="form-line">
          <label>
            色彩
            <input v-model="inspirationForm.color" placeholder="#6ea8ff" />
          </label>
          <label>
            排序
            <input v-model.number="inspirationForm.sortOrder" type="number" />
          </label>
        </div>
        <label class="check-line">
          <input v-model="inspirationForm.isPublic" type="checkbox" />
          公开展示
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">{{ editingInspirationId ? '保存修改' : '创建卡片' }}</button>
          <button v-if="editingInspirationId" class="button button-tonal" type="button" @click="resetInspirationForm">取消编辑</button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>灵感队列</h2>
          <span>共 {{ inspirations.length }} 条</span>
        </div>
        <article v-for="card in inspirations" :key="card.id" class="table-row table-row--rich">
          <div>
            <strong>{{ card.title }}</strong>
            <span>{{ inspirationTypeLabel(card.cardType) }} · {{ card.isPublic ? '公开' : '私密' }} · {{ formatDateTimeToSecond(card.createdAt) }}</span>
          </div>
          <div class="row-actions">
            <button class="icon-text-button" type="button" @click="editInspiration(card)">编辑</button>
            <button class="icon-text-button danger" type="button" @click="removeInspiration(card.id)">删除</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'comments'" class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>审核队列</h2>
          <span>共 {{ comments.length }} 条</span>
        </div>
        <div class="filter-bar">
          <select v-model="commentStatus" @change="loadComments">
            <option value="ALL">全部状态</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
            <option value="SPAM">垃圾评论</option>
          </select>
          <select v-model="commentTargetType" @change="loadComments">
            <option value="ALL">全部目标</option>
            <option value="ARTICLE">文章</option>
            <option value="PROJECT">作品</option>
            <option value="MESSAGE">留言</option>
          </select>
        </div>
        <article v-for="comment in comments" :key="comment.id" class="table-row table-row--rich">
          <div>
            <strong>{{ comment.username }}: {{ comment.content }}</strong>
            <span>{{ targetTypeLabel(comment.targetType) }} #{{ comment.targetId }} · {{ formatDateTimeToSecond(comment.createdAt) }}</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ reviewStatusLabel(comment.status) }}</span>
            <button class="icon-text-button" type="button" @click="review(comment.id, 'approve')">通过</button>
            <button class="icon-text-button danger" type="button" @click="review(comment.id, 'reject')">驳回</button>
          </div>
        </article>
      </div>

      <aside class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>审核原则</h2>
          <span>审核规范</span>
        </div>
        <ul class="rule-list">
          <li>公开评论只展示已通过状态</li>
          <li>命中敏感词会在提交时直接拒绝</li>
          <li>回复必须挂在已通过评论下，避免噪音扩散</li>
        </ul>
      </aside>
    </section>

    <section v-else-if="activeSection === 'guestbook'" class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>留言审核</h2>
          <span>共 {{ guestbookEntries.length }} 条</span>
        </div>
        <div class="filter-bar">
          <select v-model="guestbookStatus" @change="loadGuestbook">
            <option value="ALL">全部状态</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
          </select>
          <button class="icon-text-button" type="button" @click="loadGuestbook">刷新</button>
        </div>
        <article v-for="entry in guestbookEntries" :key="entry.id" class="table-row table-row--rich">
          <div>
            <strong>{{ entry.displayName }}: {{ entry.content }}</strong>
            <span>{{ formatDateTimeToSecond(entry.createdAt) }} · {{ entry.likeCount }} 赞</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ reviewStatusLabel(entry.status) }}</span>
            <button class="icon-text-button" type="button" @click="reviewGuestbookEntry(entry.id, 'approve')">通过</button>
            <button class="icon-text-button danger" type="button" @click="reviewGuestbookEntry(entry.id, 'reject')">驳回</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'files'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="uploadFile">
        <div class="panel-title">
          <h2>上传资源</h2>
          <span>本地存储</span>
        </div>
        <label>
          模块
          <select v-model="fileModule">
            <option v-for="module in fileModules" :key="module" :value="module">
              {{ fileModuleLabel(module) }}
            </option>
          </select>
        </label>
        <input type="file" @change="selectFile" />
        <button class="button button-filled" type="submit">上传文件</button>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>资源列表</h2>
          <span>共 {{ files.length }} 个</span>
        </div>
        <article v-for="file in files" :key="file.id" class="table-row table-row--rich">
          <div>
            <strong>{{ file.originalName }}</strong>
            <span>{{ fileModuleLabel(file.module) }} · {{ formatSize(file.fileSize) }} · {{ file.fileType }}</span>
          </div>
          <a class="icon-text-button" :href="file.publicUrl" target="_blank" rel="noreferrer">预览</a>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'themes'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveTheme">
        <div class="panel-title">
          <h2>{{ editingThemeId ? '编辑主题' : '选择主题' }}</h2>
          <span>{{ themeForm.primaryColor || '主题色' }}</span>
        </div>
        <div class="form-line">
          <label>
            主题标识
            <input v-model="themeForm.themeName" maxlength="80" />
          </label>
          <label>
            显示名称
            <input v-model="themeForm.displayName" maxlength="120" />
          </label>
        </div>
        <div class="form-line">
          <label>
            主色
            <input v-model="themeForm.primaryColor" type="color" />
          </label>
          <label>
            背景类型
            <select v-model="themeForm.backgroundType">
              <option value="color">纯色</option>
              <option value="solid">实色</option>
              <option value="image">图片</option>
              <option value="gradient">渐变</option>
              <option value="star">星空</option>
              <option value="webgl">动效</option>
            </select>
          </label>
        </div>
        <label>
          背景资源
          <input v-model="themeForm.backgroundImage" placeholder="/uploads/demo/theme.webp" />
        </label>
        <div class="form-line">
          <label>
            字体
            <input v-model="themeForm.fontFamily" maxlength="120" placeholder="Inter, system-ui, sans-serif" />
          </label>
          <label>
            卡片样式
            <input v-model="themeForm.cardStyle" maxlength="40" />
          </label>
        </div>
        <label>
          布局类型
          <input v-model="themeForm.layoutType" maxlength="40" />
        </label>
        <label>
          主题变量 JSON
          <textarea v-model="themeConfigText" rows="9" spellcheck="false" />
        </label>
        <div class="form-actions">
          <button class="button button-filled" type="submit">保存主题</button>
          <button v-if="editingThemeId" class="button button-tonal" type="button" @click="switchCurrentTheme()">
            设为当前主题
          </button>
        </div>
      </form>

      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>主题库</h2>
          <span>共 {{ themes.length }} 套</span>
        </div>
        <article v-for="theme in themes" :key="theme.id" class="table-row table-row--rich">
          <div>
            <strong>
              <span class="theme-dot" :style="{ backgroundColor: theme.primaryColor }" />
              {{ theme.displayName }}
            </strong>
            <span>{{ theme.themeName }} · {{ theme.layoutType }} · {{ theme.cardStyle }}</span>
          </div>
          <div class="row-actions">
            <span class="status-chip">{{ theme.active ? '当前启用' : '预设' }}</span>
            <button class="icon-text-button" type="button" @click="editTheme(theme)">编辑</button>
            <button class="icon-text-button" type="button" @click="switchCurrentTheme(theme.id)">启用</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeSection === 'settings'" class="workspace-grid">
      <form class="workspace-panel admin-form" data-reveal @submit.prevent="saveSiteSettings">
        <div class="panel-title">
          <h2>站点身份</h2>
          <span>{{ siteProfileForm.profileKey }}</span>
        </div>
        <div class="form-line">
          <label>
            配置标识
            <input v-model="siteProfileForm.profileKey" maxlength="80" />
          </label>
          <label>
            显示名称
            <input v-model="siteProfileForm.displayName" maxlength="120" />
          </label>
        </div>
        <label>
          标语
          <input v-model="siteProfileForm.headline" maxlength="180" />
        </label>
        <label>
          头像地址
          <input v-model="siteProfileForm.avatarUrl" placeholder="/uploads/demo/avatar.webp" />
        </label>
        <label>
          简介
          <textarea v-model="siteProfileForm.bio" rows="4" maxlength="5000" />
        </label>
        <div class="form-line">
          <label>
            联系邮箱
            <input v-model="siteProfileForm.contactEmail" maxlength="180" />
          </label>
          <label>
            位置
            <input v-model="siteProfileForm.location" maxlength="120" />
          </label>
        </div>
        <label>
          资料扩展 JSON
          <textarea v-model="profileJsonText" rows="5" spellcheck="false" />
        </label>
        <button class="button button-filled" type="submit">保存站点设置</button>
      </form>

      <div class="workspace-panel admin-form settings-builder" data-reveal>
        <div class="panel-title">
          <h2>导航 / 社交 / 页面</h2>
          <span>表单编辑</span>
        </div>
        <p class="settings-editor-note">已保存项按现有接口更新；新增项可移除，导航和社交可隐藏，页面可归档。</p>
        <section class="settings-section">
          <div class="settings-section-heading">
            <h3>导航项</h3>
            <button class="icon-text-button" type="button" @click="addNavigationItem">新增导航</button>
          </div>
          <article
            v-for="(item, index) in navigationForms"
            :key="item.id ?? `navigation-${index}`"
            class="settings-card"
          >
            <div class="settings-card-title">
              <strong>{{ item.label || `导航项 ${index + 1}` }}</strong>
              <button
                v-if="isDraftItem(item)"
                class="icon-text-button danger"
                type="button"
                @click="removeNavigationItem(index)"
              >
                移除
              </button>
            </div>
            <div class="form-line">
              <label>
                名称
                <input v-model="item.label" maxlength="80" />
              </label>
              <label>
                路径
                <input v-model="item.path" maxlength="180" placeholder="/articles" />
              </label>
            </div>
            <div class="form-line">
              <label>
                图标
                <input v-model="item.icon" maxlength="80" placeholder="book-open" />
              </label>
              <label>
                分组
                <input v-model="item.groupName" maxlength="80" />
              </label>
              <label>
                排序
                <input v-model.number="item.sortOrder" type="number" />
              </label>
            </div>
            <label class="check-line settings-check">
              <input v-model="item.visible" type="checkbox" />
              前台可见
            </label>
            <details class="json-fallback">
              <summary>扩展 JSON</summary>
              <textarea v-model="item.extraJsonText" rows="4" spellcheck="false" />
            </details>
          </article>
          <p v-if="navigationForms.length === 0" class="empty-editor">暂无导航项</p>
        </section>

        <section class="settings-section">
          <div class="settings-section-heading">
            <h3>社交链接</h3>
            <button class="icon-text-button" type="button" @click="addSocialLink">新增链接</button>
          </div>
          <article
            v-for="(link, index) in socialLinkForms"
            :key="link.id ?? `social-${index}`"
            class="settings-card"
          >
            <div class="settings-card-title">
              <strong>{{ link.label || `社交链接 ${index + 1}` }}</strong>
              <button
                v-if="isDraftItem(link)"
                class="icon-text-button danger"
                type="button"
                @click="removeSocialLink(index)"
              >
                移除
              </button>
            </div>
            <div class="form-line">
              <label>
                平台
                <input v-model="link.platform" maxlength="80" placeholder="github" />
              </label>
              <label>
                展示文本
                <input v-model="link.label" maxlength="120" />
              </label>
            </div>
            <label>
              链接
              <input v-model="link.url" maxlength="500" placeholder="https://example.com 或 mailto:name@example.com" />
            </label>
            <div class="form-line">
              <label>
                图标
                <input v-model="link.icon" maxlength="80" placeholder="github" />
              </label>
              <label>
                排序
                <input v-model.number="link.sortOrder" type="number" />
              </label>
            </div>
            <label class="check-line settings-check">
              <input v-model="link.visible" type="checkbox" />
              前台可见
            </label>
          </article>
          <p v-if="socialLinkForms.length === 0" class="empty-editor">暂无社交链接</p>
        </section>

        <section class="settings-section">
          <div class="settings-section-heading">
            <h3>页面配置</h3>
            <button class="icon-text-button" type="button" @click="addPageConfig">新增页面</button>
          </div>
          <article
            v-for="(page, index) in pageConfigForms"
            :key="page.id ?? `page-${index}`"
            class="settings-card"
          >
            <div class="settings-card-title">
              <strong>{{ page.title || `页面配置 ${index + 1}` }}</strong>
              <button
                v-if="isDraftItem(page)"
                class="icon-text-button danger"
                type="button"
                @click="removePageConfig(index)"
              >
                移除
              </button>
            </div>
            <div class="form-line">
              <label>
                配置键
                <input v-model="page.pageKey" maxlength="120" placeholder="about" />
              </label>
              <label>
                页面标题
                <input v-model="page.title" maxlength="180" />
              </label>
            </div>
            <div class="form-line">
              <label>
                URL 标识
                <input v-model="page.slug" maxlength="180" placeholder="about" />
              </label>
              <label>
                状态
                <select v-model="page.status">
                  <option v-for="status in pageStatuses" :key="status" :value="status">{{ status }}</option>
                </select>
              </label>
            </div>
            <label>
              SEO 标题
              <input v-model="page.seoTitle" maxlength="180" />
            </label>
            <label>
              SEO 描述
              <textarea v-model="page.seoDescription" rows="3" maxlength="1000" />
            </label>
            <details class="json-fallback" open>
              <summary>内容 JSON</summary>
              <textarea v-model="page.contentJsonText" rows="4" spellcheck="false" />
            </details>
            <details class="json-fallback">
              <summary>布局 JSON</summary>
              <textarea v-model="page.layoutJsonText" rows="4" spellcheck="false" />
            </details>
          </article>
          <p v-if="pageConfigForms.length === 0" class="empty-editor">暂无页面配置</p>
        </section>

        <section class="settings-section">
          <div class="settings-section-heading">
            <h3>站点配置</h3>
            <button class="icon-text-button" type="button" @click="addSiteConfigEntry">新增配置</button>
          </div>
          <article
            v-for="(config, index) in siteConfigForms"
            :key="config.id ?? `config-${index}`"
            class="settings-card"
          >
            <div class="settings-card-title">
              <strong>{{ config.configKey || `站点配置 ${index + 1}` }}</strong>
              <button
                v-if="isDraftItem(config)"
                class="icon-text-button danger"
                type="button"
                @click="removeSiteConfigEntry(index)"
              >
                移除
              </button>
            </div>
            <label>
              配置键
              <input
                v-model="config.configKey"
                :disabled="!isDraftItem(config)"
                maxlength="120"
                placeholder="site.identity"
              />
            </label>
            <label>
              配置说明
              <input v-model="config.description" maxlength="500" />
            </label>
            <details class="json-fallback" open>
              <summary>配置值 JSON</summary>
              <textarea v-model="config.configValueText" rows="5" spellcheck="false" />
            </details>
          </article>
          <p v-if="siteConfigForms.length === 0" class="empty-editor">暂无站点配置</p>
        </section>
        <div class="form-actions settings-actions">
          <button class="button button-filled" type="button" @click="saveSiteSettings">保存全部设置</button>
        </div>
      </div>
    </section>

    <section v-else class="workspace-grid">
      <div class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>内容队列</h2>
          <span>共 {{ moduleConfig.rows.length }} 条</span>
        </div>
        <article v-for="row in moduleConfig.rows" :key="row.title" class="table-row table-row--rich">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.meta }}</span>
          </div>
          <span class="status-chip">{{ generalStatusLabel(row.status) }}</span>
        </article>
      </div>

      <aside class="workspace-panel" data-reveal>
        <div class="panel-title">
          <h2>模块能力</h2>
          <span>待扩展</span>
        </div>
        <ul class="rule-list">
          <li v-for="capability in moduleConfig.capabilities" :key="capability">{{ capability }}</li>
        </ul>
      </aside>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
// 导入状态钩子, 路由组件和业务接口
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus } from '@lucide/vue'

import {
  approveArticle,
  approveProject,
  changeArticlePublishState,
  createArticle,
  createInspiration,
  createProject,
  deleteArticle,
  deleteInspiration,
  deleteProject,
  fetchAdminSiteSettings,
  fetchAdminThemes,
  fetchAdminArticle,
  fetchAdminArticles,
  fetchAdminComments,
  fetchAdminFiles,
  fetchAdminGuestbook,
  fetchAdminInspirations,
  fetchAdminProject,
  fetchAdminProjects,
  fetchCategories,
  fetchTags,
  reviewComment,
  reviewGuestbook,
  rejectArticle,
  rejectProject,
  setArticleRecommend,
  setArticleTop,
  setProjectRecommend,
  setProjectStatus,
  switchTheme,
  updateArticle,
  updateInspiration,
  updateProject,
  updateSiteSettings,
  updateTheme,
  uploadAdminFile,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import { usePageReveal } from '@/shared/composables/usePageReveal'
import { formatDateTimeToSecond } from '@/shared/datetime'
import type {
  AdminThemeConfig,
  ArticlePayload,
  ArticlePrivacy,
  ArticleSummary,
  CategorySummary,
  CommentSummary,
  ContentStatus,
  FileResource,
  InspirationCard,
  InspirationPayload,
  InspirationType,
  NavigationItem,
  PageConfig,
  ProjectPayload,
  ProjectSummary,
  SiteConfigEntry,
  SiteProfile,
  SiteSettings,
  SocialLink,
  TagSummary,
  ThemePayload,
} from '@/shared/domain'
import { syncSiteIdentityFromSettings, useSiteIdentity } from '@/shared/siteIdentity'

interface ModuleConfig {
  title: string
  description: string
  primaryAction: string
  capabilities: string[]
  rows: Array<{ title: string; meta: string; status: string }>
}

type NavigationItemForm = Omit<NavigationItem, 'extraJson' | 'icon'> & {
  icon: string
  extraJsonText: string
}

type SocialLinkForm = Omit<SocialLink, 'icon'> & {
  icon: string
}

type PageConfigForm = Omit<PageConfig, 'contentJson' | 'layoutJson' | 'seoTitle' | 'seoDescription'> & {
  seoTitle: string
  seoDescription: string
  contentJsonText: string
  layoutJsonText: string
}

type SiteConfigEntryForm = Omit<SiteConfigEntry, 'configValue' | 'description'> & {
  description: string
  configValueText: string
}

const route = useRoute()
const root = ref<HTMLElement | null>(null)
const { identity } = useSiteIdentity({ load: false })
const notice = ref('')
const articles = ref<ArticleSummary[]>([])
const projects = ref<ProjectSummary[]>([])
const articleCategories = ref<CategorySummary[]>([])
const tags = ref<TagSummary[]>([])
const inspirations = ref<InspirationCard[]>([])
const comments = ref<CommentSummary[]>([])
const files = ref<FileResource[]>([])
const themes = ref<AdminThemeConfig[]>([])
const articleKeyword = ref('')
const projectKeyword = ref('')
const articleStatus = ref<ContentStatus | 'ALL'>('ALL')
const projectStatus = ref<ProjectSummary['status'] | 'ALL'>('ALL')
const commentStatus = ref<CommentSummary['status'] | 'ALL'>('PENDING')
const commentTargetType = ref<CommentSummary['targetType'] | 'ALL'>('ALL')
const guestbookEntries = ref<Array<{ id: number; userId?: number | null; displayName: string; content: string; status: string; likeCount: number; createdAt?: string | null }>>([])
const guestbookStatus = ref('ALL')
const fileModule = ref('OTHER')
const selectedFile = ref<File | null>(null)
const editingArticleId = ref<number | null>(null)
const editingProjectId = ref<number | null>(null)
const editingInspirationId = ref<number | null>(null)
const editingThemeId = ref<number | null>(null)
const projectTechStack = ref('')
const themeConfigText = ref('{}')
const profileJsonText = ref('{}')
const navigationForms = ref<NavigationItemForm[]>([])
const socialLinkForms = ref<SocialLinkForm[]>([])
const pageConfigForms = ref<PageConfigForm[]>([])
const siteConfigForms = ref<SiteConfigEntryForm[]>([])

const articlePrivacies: ArticlePrivacy[] = ['PUBLIC', 'SELF', 'FRIENDS', 'SELECTED_FRIENDS', 'EXCLUDED_FRIENDS']
const inspirationTypes: InspirationType[] = ['TEXT', 'PROMPT', 'IMAGE', 'CODE', 'LINK', 'SKETCH', 'REFERENCE']
const fileModules = ['AVATAR', 'COVER', 'ARTICLE', 'PROJECT', 'INSPIRATION', 'OTHER']
const pageStatuses: PageConfig['status'][] = ['DRAFT', 'PUBLISHED', 'ARCHIVED']
const settingsKeyPattern = /^[a-zA-Z0-9._-]{2,120}$/
const pageSlugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/
const articleForm = reactive<ArticlePayload>({
  title: '',
  slug: '',
  summary: '',
  contentMarkdown: '',
  coverUrl: '',
  categoryId: null,
  tagIds: [],
  privacyType: 'PUBLIC',
})
const projectForm = reactive<ProjectPayload>({
  title: '',
  slug: '',
  description: '',
  coverUrl: '',
  projectType: 'WEB_APP',
  techStack: [],
  githubUrl: '',
  demoUrl: '',
  videoUrl: '',
  contentMarkdown: '',
  tagIds: [],
  recommended: false,
})
const inspirationForm = reactive<InspirationPayload>({
  title: '',
  content: '',
  imageUrl: '',
  cardType: 'TEXT',
  sourceUrl: '',
  color: '#6ea8ff',
  isPublic: true,
  sortOrder: 0,
  tagIds: [],
})
const themeForm = reactive<ThemePayload>({
  themeName: '',
  displayName: '',
  primaryColor: '#1a73e8',
  backgroundType: 'color',
  backgroundImage: '',
  fontFamily: 'Inter, system-ui, sans-serif',
  cardStyle: 'material',
  layoutType: 'editorial',
  config: {},
})
const siteProfileForm = reactive<SiteProfile>({
  profileKey: 'default',
  displayName: '',
  headline: '',
  avatarUrl: '',
  bio: '',
  contactEmail: '',
  location: '',
  profileJson: {},
})

usePageReveal(root)

const activeSection = computed(() => route.params.section?.toString() || 'articles')
const siteIdentityMeta = computed(() => (
  identity.value.slogan ? `${identity.value.name} · ${identity.value.slogan}` : identity.value.name
))
const moduleConfig = computed<ModuleConfig>(() => {
  const config = (configs[activeSection.value] ?? configs.articles) as ModuleConfig
  if (activeSection.value !== 'settings') {
    return config
  }
  return {
    ...config,
    rows: config.rows.map((row) => (
      row.title === '站点身份' ? { ...row, meta: siteIdentityMeta.value } : row
    )),
  }
})

function valueLabel(value: string, labels: Record<string, string>) {
  return labels[value] ?? value
}

function privacyLabel(value: string) {
  return valueLabel(value, {
    PUBLIC: '公开',
    SELF: '仅自己',
    FRIENDS: '好友可见',
    SELECTED_FRIENDS: '指定好友',
    EXCLUDED_FRIENDS: '排除好友',
    PRIVATE: '私密',
  })
}

function contentStatusLabel(value: string) {
  return valueLabel(value, {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    PUBLISHED: '已公开',
    PRIVATE: '私密',
    REJECTED: '已驳回',
    SCHEDULED: '定时发布',
    ARCHIVED: '已归档',
  })
}

function projectStatusLabel(value: string) {
  return valueLabel(value, {
    DRAFT: '草稿',
    PENDING_REVIEW: '待审核',
    VISIBLE: '展示中',
    HIDDEN: '已隐藏',
    REJECTED: '已驳回',
    ARCHIVED: '已归档',
  })
}

function reviewStatusLabel(value: string) {
  return valueLabel(value, {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    SPAM: '垃圾内容',
  })
}

function inspirationTypeLabel(value: string) {
  return valueLabel(value, {
    TEXT: '文本',
    PROMPT: '提示词',
    IMAGE: '图片',
    CODE: '代码',
    LINK: '链接',
    SKETCH: '草图',
    REFERENCE: '参考资料',
  })
}

function targetTypeLabel(value: string) {
  return valueLabel(value, {
    ARTICLE: '文章',
    PROJECT: '作品',
    MESSAGE: '留言',
  })
}

function fileModuleLabel(value: string) {
  return valueLabel(value, {
    AVATAR: '头像',
    COVER: '封面',
    ARTICLE: '文章',
    PROJECT: '作品',
    INSPIRATION: '灵感',
    OTHER: '其他',
  })
}

function projectTypeLabel(value: string | null | undefined) {
  const cleaned = cleanText(value)
  return valueLabel(cleaned || 'PROJECT', {
    WEB_APP: 'Web 应用',
    PROJECT: '作品',
    TOOL: '工具',
    DESIGN: '设计',
    ARTICLE: '文章',
  })
}

function generalStatusLabel(value: string) {
  return valueLabel(value, {
    ACTIVE: '当前启用',
    PRESET: '预设',
    READY: '已就绪',
    VISIBLE: '展示中',
    DRAFT: '草稿',
    PUBLISHED: '已公开',
    PRIVATE: '私密',
  })
}

watch(activeSection, () => {
  notice.value = ''
  loadActiveModule()
})

// 组件挂载时自动加载当前页签对应的子管理模块
onMounted(loadActiveModule)

// 依据侧边栏激活的子页签模块类型, 动态分流调用对应的子模块列表和过滤器加载方法
async function loadActiveModule() {
  if (activeSection.value === 'articles') {
    await loadArticles()
  } else if (activeSection.value === 'projects') {
    await loadProjects()
  } else if (activeSection.value === 'inspirations') {
    await loadInspirations()
  } else if (activeSection.value === 'comments') {
    await loadComments()
  } else if (activeSection.value === 'guestbook') {
    await loadGuestbook()
  } else if (activeSection.value === 'files') {
    await loadFiles()
  } else if (activeSection.value === 'themes') {
    await loadThemes()
  } else if (activeSection.value === 'settings') {
    await loadSiteSettings()
  }
}

async function ensureTaxonomies() {
  if (articleCategories.value.length === 0) {
    articleCategories.value = await fetchCategories('ARTICLE')
  }
  if (tags.value.length === 0) {
    tags.value = await fetchTags()
  }
}

// 异步拉取后台文章管理列表, 支持按状态筛选和关键词匹配
async function loadArticles() {
  try {
    await ensureTaxonomies()
    const page = await fetchAdminArticles({
      keyword: articleKeyword.value,
      status: articleStatus.value,
      pageSize: 50,
    })
    articles.value = page.records
  } catch (error) {
    notice.value = readError(error, '文章队列加载失败')
  }
}

// 提交或更新文章草稿, 根据是否存在编辑中 ID 执行 PUT 或 POST 接口更新并刷新文章队列
async function saveArticle() {
  if (!articleForm.title.trim() || !articleForm.slug.trim() || !articleForm.contentMarkdown.trim()) {
    notice.value = '请填写文章标题、URL 标识和正文'
    return
  }
  try {
    if (editingArticleId.value) {
      await updateArticle(editingArticleId.value, { ...articleForm })
      notice.value = '文章已更新'
    } else {
      await createArticle({ ...articleForm })
      notice.value = '文章草稿已创建'
    }
    resetArticleForm()
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章保存失败')
  }
}

async function editArticle(id: number) {
  try {
    const article = await fetchAdminArticle(id)
    editingArticleId.value = article.id
    articleForm.title = article.title
    articleForm.slug = article.slug
    articleForm.summary = article.summary ?? ''
    articleForm.contentMarkdown = article.contentMarkdown ?? ''
    articleForm.coverUrl = article.coverUrl ?? ''
    articleForm.categoryId = article.category?.id ?? null
    articleForm.tagIds = article.tags.map((tag) => tag.id)
    articleForm.privacyType = article.privacyType
  } catch (error) {
    notice.value = readError(error, '文章读取失败')
  }
}

async function toggleArticlePublish(article: ArticleSummary) {
  try {
    const action = article.status === 'PUBLISHED' || article.status === 'PRIVATE' ? 'unpublish' : 'publish'
    await changeArticlePublishState(article.id, action)
    notice.value = action === 'publish' ? '文章已发布' : '文章已撤回'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章状态更新失败')
  }
}

async function approveArticleReview(article: ArticleSummary) {
  try {
    await approveArticle(article.id)
    notice.value = '文章审核通过'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章审核失败')
  }
}

async function rejectArticleReview(article: ArticleSummary) {
  const reviewNote = window.prompt('请输入驳回原因', article.reviewNote ?? '请补充来源、说明或作品归属信息。')?.trim()
  if (!reviewNote) {
    return
  }
  try {
    await rejectArticle(article.id, reviewNote)
    notice.value = '文章已驳回'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章驳回失败')
  }
}

function articlePublishActionLabel(article: ArticleSummary) {
  return article.status === 'PUBLISHED' || article.status === 'PRIVATE' ? '撤回' : '发布'
}

async function toggleArticleTop(article: ArticleSummary) {
  try {
    await setArticleTop(article.id, !article.top)
    notice.value = article.top ? '已取消置顶' : '文章已置顶'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '置顶状态更新失败')
  }
}

async function toggleArticleRecommend(article: ArticleSummary) {
  try {
    await setArticleRecommend(article.id, !article.recommended)
    notice.value = article.recommended ? '已取消推荐' : '文章已推荐'
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '推荐状态更新失败')
  }
}

async function removeArticle(id: number) {
  try {
    await deleteArticle(id)
    notice.value = '文章已删除'
    if (editingArticleId.value === id) {
      resetArticleForm()
    }
    await loadArticles()
  } catch (error) {
    notice.value = readError(error, '文章删除失败')
  }
}

function resetArticleForm() {
  editingArticleId.value = null
  articleForm.title = ''
  articleForm.slug = ''
  articleForm.summary = ''
  articleForm.contentMarkdown = ''
  articleForm.coverUrl = ''
  articleForm.categoryId = null
  articleForm.tagIds = []
  articleForm.privacyType = 'PUBLIC'
}

async function loadProjects() {
  try {
    await ensureTaxonomies()
    const page = await fetchAdminProjects({
      keyword: projectKeyword.value,
      status: projectStatus.value,
      pageSize: 50,
    })
    projects.value = page.records
  } catch (error) {
    notice.value = readError(error, '作品队列加载失败')
  }
}

async function saveProject() {
  if (!projectForm.title.trim() || !projectForm.slug.trim() || !projectForm.projectType.trim()) {
    notice.value = '请填写作品标题、URL 标识和类型'
    return
  }
  const payload = { ...projectForm, techStack: splitTechStack(projectTechStack.value) }
  try {
    if (editingProjectId.value) {
      await updateProject(editingProjectId.value, payload)
      notice.value = '作品已更新'
    } else {
      await createProject(payload)
      notice.value = '作品已创建'
    }
    resetProjectForm()
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品保存失败')
  }
}

async function editProject(id: number) {
  try {
    const project = await fetchAdminProject(id)
    editingProjectId.value = project.id
    projectForm.title = project.title
    projectForm.slug = project.slug
    projectForm.description = project.description ?? ''
    projectForm.coverUrl = project.coverUrl ?? ''
    projectForm.projectType = project.projectType
    projectForm.techStack = project.techStack
    projectForm.githubUrl = project.githubUrl ?? ''
    projectForm.demoUrl = project.demoUrl ?? ''
    projectForm.videoUrl = project.videoUrl ?? ''
    projectForm.contentMarkdown = project.contentMarkdown ?? ''
    projectForm.tagIds = project.tags.map((tag) => tag.id)
    projectForm.recommended = project.recommended
    projectTechStack.value = project.techStack.join(', ')
  } catch (error) {
    notice.value = readError(error, '作品读取失败')
  }
}

async function toggleProjectVisible(project: ProjectSummary) {
  try {
    if (project.status === 'PENDING_REVIEW') {
      await approveProject(project.id)
    } else {
      await setProjectStatus(project.id, project.status === 'VISIBLE' ? 'HIDDEN' : 'VISIBLE')
    }
    notice.value = project.status === 'VISIBLE' ? '作品已隐藏' : '作品已展示'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品状态更新失败')
  }
}

async function approveProjectReview(project: ProjectSummary) {
  try {
    await approveProject(project.id)
    notice.value = '作品审核通过'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品审核失败')
  }
}

async function rejectProjectReview(project: ProjectSummary) {
  const reviewNote = window.prompt('请输入驳回原因', project.reviewNote ?? '请补充素材授权、创作说明或演示链接。')?.trim()
  if (!reviewNote) {
    return
  }
  try {
    await rejectProject(project.id, reviewNote)
    notice.value = '作品已驳回'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品驳回失败')
  }
}

async function toggleProjectRecommend(project: ProjectSummary) {
  try {
    await setProjectRecommend(project.id, !project.recommended)
    notice.value = project.recommended ? '已取消推荐' : '作品已推荐'
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品推荐状态更新失败')
  }
}

async function removeProject(id: number) {
  try {
    await deleteProject(id)
    notice.value = '作品已删除'
    if (editingProjectId.value === id) {
      resetProjectForm()
    }
    await loadProjects()
  } catch (error) {
    notice.value = readError(error, '作品删除失败')
  }
}

function resetProjectForm() {
  editingProjectId.value = null
  projectForm.title = ''
  projectForm.slug = ''
  projectForm.description = ''
  projectForm.coverUrl = ''
  projectForm.projectType = 'WEB_APP'
  projectForm.techStack = []
  projectForm.githubUrl = ''
  projectForm.demoUrl = ''
  projectForm.videoUrl = ''
  projectForm.contentMarkdown = ''
  projectForm.tagIds = []
  projectForm.recommended = false
  projectTechStack.value = ''
}

async function loadInspirations() {
  try {
    const page = await fetchAdminInspirations({ pageSize: 50 })
    inspirations.value = page.records
  } catch (error) {
    notice.value = readError(error, '灵感卡片加载失败')
  }
}

async function saveInspiration() {
  if (!inspirationForm.title.trim()) {
    notice.value = '请填写灵感标题'
    return
  }
  try {
    if (editingInspirationId.value) {
      await updateInspiration(editingInspirationId.value, { ...inspirationForm })
      notice.value = '灵感卡片已更新'
    } else {
      await createInspiration({ ...inspirationForm })
      notice.value = '灵感卡片已创建'
    }
    resetInspirationForm()
    await loadInspirations()
  } catch (error) {
    notice.value = readError(error, '灵感卡片保存失败')
  }
}

function editInspiration(card: InspirationCard) {
  editingInspirationId.value = card.id
  inspirationForm.title = card.title
  inspirationForm.content = card.content ?? ''
  inspirationForm.imageUrl = card.imageUrl ?? ''
  inspirationForm.cardType = card.cardType
  inspirationForm.sourceUrl = card.sourceUrl ?? ''
  inspirationForm.color = card.color ?? '#6ea8ff'
  inspirationForm.isPublic = card.isPublic ?? true
  inspirationForm.sortOrder = card.sortOrder
  inspirationForm.tagIds = card.tags.map((tag) => tag.id)
}

async function removeInspiration(id: number) {
  try {
    await deleteInspiration(id)
    notice.value = '灵感卡片已删除'
    await loadInspirations()
  } catch (error) {
    notice.value = readError(error, '灵感卡片删除失败')
  }
}

function resetInspirationForm() {
  editingInspirationId.value = null
  inspirationForm.title = ''
  inspirationForm.content = ''
  inspirationForm.imageUrl = ''
  inspirationForm.cardType = 'TEXT'
  inspirationForm.sourceUrl = ''
  inspirationForm.color = '#6ea8ff'
  inspirationForm.isPublic = true
  inspirationForm.sortOrder = 0
  inspirationForm.tagIds = []
}

async function loadComments() {
  try {
    const page = await fetchAdminComments({
      status: commentStatus.value,
      targetType: commentTargetType.value,
      pageSize: 50,
    })
    comments.value = page.records
  } catch (error) {
    notice.value = readError(error, '评论队列加载失败')
  }
}

async function review(id: number, action: 'approve' | 'reject') {
  try {
    await reviewComment(id, action)
    notice.value = action === 'approve' ? '评论已通过' : '评论已驳回'
    await loadComments()
  } catch (error) {
    notice.value = readError(error, '评论审核失败')
  }
}

async function loadGuestbook() {
  try {
    const page = await fetchAdminGuestbook({
      status: guestbookStatus.value,
      pageSize: 50,
    })
    guestbookEntries.value = page.records
  } catch (error) {
    notice.value = readError(error, '留言加载失败')
  }
}

async function reviewGuestbookEntry(id: number, action: 'approve' | 'reject') {
  try {
    await reviewGuestbook(id, action)
    notice.value = action === 'approve' ? '留言已通过' : '留言已驳回'
    await loadGuestbook()
  } catch (error) {
    notice.value = readError(error, '留言审核失败')
  }
}

async function loadFiles() {
  try {
    const page = await fetchAdminFiles({ pageSize: 50 })
    files.value = page.records
  } catch (error) {
    notice.value = readError(error, '文件资源加载失败')
  }
}

function selectFile(event: Event) {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function uploadFile() {
  if (!selectedFile.value) {
    notice.value = '请选择文件'
    return
  }
  try {
    await uploadAdminFile(selectedFile.value, fileModule.value)
    selectedFile.value = null
    notice.value = '文件已上传'
    await loadFiles()
  } catch (error) {
    notice.value = readError(error, '文件上传失败')
  }
}

async function loadThemes() {
  try {
    themes.value = await fetchAdminThemes()
    const selected = themes.value.find((theme) => theme.id === editingThemeId.value)
      ?? themes.value.find((theme) => theme.active)
      ?? themes.value[0]
    if (selected) {
      editTheme(selected)
    }
  } catch (error) {
    notice.value = readError(error, '主题配置加载失败')
  }
}

function editTheme(theme: AdminThemeConfig) {
  editingThemeId.value = theme.id
  themeForm.themeName = theme.themeName
  themeForm.displayName = theme.displayName
  themeForm.primaryColor = theme.primaryColor
  themeForm.backgroundType = theme.backgroundType
  themeForm.backgroundImage = theme.backgroundImage ?? ''
  themeForm.fontFamily = theme.fontFamily ?? ''
  themeForm.cardStyle = theme.cardStyle
  themeForm.layoutType = theme.layoutType
  themeForm.config = normalizeRecord(theme.config)
  themeConfigText.value = prettyJson(themeForm.config)
}

async function saveTheme() {
  if (!editingThemeId.value) {
    notice.value = '请先从主题库选择一个主题'
    return
  }
  const config = parseRecord(themeConfigText.value, '主题变量 JSON')
  if (!config) {
    return
  }
  try {
    const saved = await updateTheme(editingThemeId.value, {
      ...themeForm,
      backgroundImage: themeForm.backgroundImage || null,
      fontFamily: themeForm.fontFamily || null,
      config,
    })
    notice.value = '主题已保存'
    await loadThemes()
    editTheme(saved)
  } catch (error) {
    notice.value = readError(error, '主题保存失败')
  }
}

async function switchCurrentTheme(id = editingThemeId.value) {
  if (!id) {
    notice.value = '请先选择主题'
    return
  }
  try {
    const switched = await switchTheme(id)
    notice.value = '当前主题已切换'
    await loadThemes()
    editTheme(switched)
  } catch (error) {
    notice.value = readError(error, '主题切换失败')
  }
}

async function loadSiteSettings() {
  try {
    applySiteSettings(await fetchAdminSiteSettings())
  } catch (error) {
    notice.value = readError(error, '站点设置加载失败')
  }
}

function applySiteSettings(settings: SiteSettings) {
  syncSiteIdentityFromSettings(settings)
  const profile = settings.profile
  siteProfileForm.profileKey = profile?.profileKey ?? 'default'
  siteProfileForm.displayName = profile?.displayName ?? ''
  siteProfileForm.headline = profile?.headline ?? ''
  siteProfileForm.avatarUrl = profile?.avatarUrl ?? ''
  siteProfileForm.bio = profile?.bio ?? ''
  siteProfileForm.contactEmail = profile?.contactEmail ?? ''
  siteProfileForm.location = profile?.location ?? ''
  siteProfileForm.profileJson = normalizeRecord(profile?.profileJson)
  profileJsonText.value = prettyJson(siteProfileForm.profileJson)
  navigationForms.value = settings.navigationItems.map(toNavigationForm)
  socialLinkForms.value = settings.socialLinks.map(toSocialLinkForm)
  pageConfigForms.value = settings.pages.map(toPageConfigForm)
  siteConfigForms.value = settings.configs.map(toSiteConfigForm)
}

async function saveSiteSettings() {
  const profileJson = parseRecord(profileJsonText.value, '资料扩展 JSON')
  const navigationItems = collectNavigationItems()
  const socialLinks = collectSocialLinks()
  const pages = collectPageConfigs()
  const configs = collectSiteConfigs()
  if (!profileJson || !navigationItems || !socialLinks || !pages || !configs) {
    return
  }
  try {
    const settings = await updateSiteSettings({
      profile: {
        ...siteProfileForm,
        avatarUrl: siteProfileForm.avatarUrl || null,
        headline: siteProfileForm.headline || null,
        bio: siteProfileForm.bio || null,
        contactEmail: siteProfileForm.contactEmail || null,
        location: siteProfileForm.location || null,
        profileJson,
      },
      navigationItems,
      socialLinks,
      pages,
      configs,
    })
    notice.value = '站点设置已保存'
    applySiteSettings(settings)
  } catch (error) {
    notice.value = readError(error, '站点设置保存失败')
  }
}

function toNavigationForm(item: NavigationItem): NavigationItemForm {
  return {
    id: item.id ?? null,
    label: item.label ?? '',
    path: item.path ?? '',
    icon: item.icon ?? '',
    groupName: item.groupName ?? 'primary',
    sortOrder: normalizeSortOrder(item.sortOrder),
    visible: item.visible,
    extraJsonText: prettyJson(normalizeRecord(item.extraJson)),
  }
}

function toSocialLinkForm(link: SocialLink): SocialLinkForm {
  return {
    id: link.id ?? null,
    platform: link.platform ?? '',
    label: link.label ?? '',
    url: link.url ?? '',
    icon: link.icon ?? '',
    sortOrder: normalizeSortOrder(link.sortOrder),
    visible: link.visible,
  }
}

function toPageConfigForm(page: PageConfig): PageConfigForm {
  return {
    id: page.id ?? null,
    pageKey: page.pageKey ?? '',
    title: page.title ?? '',
    slug: page.slug ?? '',
    seoTitle: page.seoTitle ?? '',
    seoDescription: page.seoDescription ?? '',
    contentJsonText: prettyJson(normalizeRecord(page.contentJson)),
    layoutJsonText: prettyJson(normalizeRecord(page.layoutJson)),
    status: page.status,
  }
}

function toSiteConfigForm(config: SiteConfigEntry): SiteConfigEntryForm {
  return {
    id: config.id ?? null,
    configKey: config.configKey ?? '',
    description: config.description ?? '',
    configValueText: prettyJson(normalizeRecord(config.configValue)),
  }
}

function addNavigationItem() {
  navigationForms.value.push(toNavigationForm({
    id: null,
    label: '',
    path: '',
    icon: '',
    groupName: 'primary',
    sortOrder: nextSortOrder(navigationForms.value),
    visible: true,
    extraJson: {},
  }))
}

function addSocialLink() {
  socialLinkForms.value.push(toSocialLinkForm({
    id: null,
    platform: '',
    label: '',
    url: '',
    icon: '',
    sortOrder: nextSortOrder(socialLinkForms.value),
    visible: true,
  }))
}

function addPageConfig() {
  pageConfigForms.value.push(toPageConfigForm({
    id: null,
    pageKey: '',
    title: '',
    slug: '',
    seoTitle: '',
    seoDescription: '',
    contentJson: {},
    layoutJson: {},
    status: 'DRAFT',
  }))
}

function addSiteConfigEntry() {
  siteConfigForms.value.push(toSiteConfigForm({
    id: null,
    configKey: '',
    description: '',
    configValue: {},
  }))
}

function removeNavigationItem(index: number) {
  removeDraftItem(navigationForms.value, index)
}

function removeSocialLink(index: number) {
  removeDraftItem(socialLinkForms.value, index)
}

function removePageConfig(index: number) {
  removeDraftItem(pageConfigForms.value, index)
}

function removeSiteConfigEntry(index: number) {
  removeDraftItem(siteConfigForms.value, index)
}

function isDraftItem(item: { id?: number | null }) {
  return item.id == null
}

function removeDraftItem<T extends { id?: number | null }>(items: T[], index: number) {
  const item = items[index]
  if (!item || !isDraftItem(item)) {
    return
  }
  items.splice(index, 1)
}

function collectNavigationItems(): NavigationItem[] | null {
  const items: NavigationItem[] = []
  for (const [index, form] of navigationForms.value.entries()) {
    const label = requireFormText(form.label, `导航项 ${index + 1} 名称`)
    const path = requireFormText(form.path, `导航项 ${index + 1} 路径`)
    const groupName = requireFormText(form.groupName, `导航项 ${index + 1} 分组`)
    const extraJson = parseRecordOrEmpty(form.extraJsonText, `导航项 ${index + 1} 扩展 JSON`)
    if (!label || !path || !groupName || !extraJson) {
      return null
    }
    if (!isValidNavigationPath(path)) {
      notice.value = `导航项 ${index + 1} 路径只允许站内路径或 http/https 地址`
      return null
    }
    items.push({
      id: form.id ?? null,
      label,
      path,
      icon: optionalText(form.icon),
      groupName,
      sortOrder: normalizeSortOrder(form.sortOrder),
      visible: Boolean(form.visible),
      extraJson,
    })
  }
  return items
}

function collectSocialLinks(): SocialLink[] | null {
  const links: SocialLink[] = []
  for (const [index, form] of socialLinkForms.value.entries()) {
    const platform = requireFormText(form.platform, `社交链接 ${index + 1} 平台`)
    const label = requireFormText(form.label, `社交链接 ${index + 1} 展示文本`)
    const url = requireFormText(form.url, `社交链接 ${index + 1} 链接`)
    if (!platform || !label || !url) {
      return null
    }
    if (!isValidSocialUrl(url)) {
      notice.value = `社交链接 ${index + 1} 只允许 http、https 或 mailto 地址`
      return null
    }
    links.push({
      id: form.id ?? null,
      platform,
      label,
      url,
      icon: optionalText(form.icon),
      sortOrder: normalizeSortOrder(form.sortOrder),
      visible: Boolean(form.visible),
    })
  }
  return links
}

function collectPageConfigs(): PageConfig[] | null {
  const pages: PageConfig[] = []
  for (const [index, form] of pageConfigForms.value.entries()) {
    const pageKey = requireFormText(form.pageKey, `页面配置 ${index + 1} 配置键`)
    const title = requireFormText(form.title, `页面配置 ${index + 1} 标题`)
    const slug = requireFormText(form.slug, `页面配置 ${index + 1} URL 标识`)
    const contentJson = parseRecordOrEmpty(form.contentJsonText, `页面配置 ${index + 1} 内容 JSON`)
    const layoutJson = parseRecordOrEmpty(form.layoutJsonText, `页面配置 ${index + 1} 布局 JSON`)
    if (!pageKey || !title || !slug || !contentJson || !layoutJson) {
      return null
    }
    if (!settingsKeyPattern.test(pageKey)) {
      notice.value = `页面配置 ${index + 1} 配置键只能使用字母、数字、点、下划线或连字符`
      return null
    }
    if (!pageSlugPattern.test(slug)) {
      notice.value = `页面配置 ${index + 1} URL 标识只能使用小写字母、数字和连字符`
      return null
    }
    pages.push({
      id: form.id ?? null,
      pageKey,
      title,
      slug,
      seoTitle: optionalText(form.seoTitle),
      seoDescription: optionalText(form.seoDescription),
      contentJson,
      layoutJson,
      status: form.status,
    })
  }
  return pages
}

function collectSiteConfigs(): SiteConfigEntry[] | null {
  const configs: SiteConfigEntry[] = []
  for (const [index, form] of siteConfigForms.value.entries()) {
    const configKey = requireFormText(form.configKey, `站点配置 ${index + 1} 配置键`)
    const configValue = parseRecordOrEmpty(form.configValueText, `站点配置 ${index + 1} 配置值 JSON`)
    if (!configKey || !configValue) {
      return null
    }
    if (!settingsKeyPattern.test(configKey)) {
      notice.value = `站点配置 ${index + 1} 配置键只能使用字母、数字、点、下划线或连字符`
      return null
    }
    configs.push({
      id: form.id ?? null,
      configKey,
      configValue,
      description: optionalText(form.description),
    })
  }
  return configs
}

function handlePrimaryAction() {
  if (activeSection.value === 'articles') {
    resetArticleForm()
  } else if (activeSection.value === 'projects') {
    resetProjectForm()
  } else if (activeSection.value === 'inspirations') {
    resetInspirationForm()
  } else if (activeSection.value === 'comments') {
    commentStatus.value = 'PENDING'
    loadComments()
  } else if (activeSection.value === 'files') {
    loadFiles()
  } else if (activeSection.value === 'themes') {
    saveTheme()
  } else if (activeSection.value === 'settings') {
    saveSiteSettings()
  }
}

function formatSize(value: number) {
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function splitTechStack(value: string) {
  return value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function normalizeSortOrder(value: unknown): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? Math.trunc(parsed) : 0
}

function nextSortOrder(items: Array<{ sortOrder: unknown }>) {
  return items.reduce((max, item) => Math.max(max, normalizeSortOrder(item.sortOrder)), 0) + 10
}

function prettyJson(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2)
}

function parseRecordOrEmpty(value: string, label: string): Record<string, unknown> | null {
  return parseRecord(value.trim() || '{}', label)
}

function parseRecord(value: string, label: string): Record<string, unknown> | null {
  try {
    const parsed: unknown = JSON.parse(value)
    if (isRecord(parsed)) {
      return parsed
    }
    notice.value = `${label} 必须是 JSON 对象`
    return null
  } catch {
    notice.value = `${label} 不是合法 JSON`
    return null
  }
}

function requireFormText(value: string, label: string): string | null {
  const cleaned = cleanText(value)
  if (cleaned) {
    return cleaned
  }
  notice.value = `${label}不能为空`
  return null
}

function optionalText(value: string | null | undefined): string | null {
  const cleaned = cleanText(value)
  return cleaned || null
}

function cleanText(value: string | null | undefined): string {
  return typeof value === 'string' ? value.trim() : ''
}

function isValidNavigationPath(value: string): boolean {
  if (value.startsWith('// ')) {
    return false
  }
  return value.startsWith('/') || isHttpUrl(value)
}

function isValidSocialUrl(value: string): boolean {
  if (value.toLowerCase().startsWith('mailto:')) {
    return value.length > 'mailto:'.length
  }
  return isHttpUrl(value)
}

function isHttpUrl(value: string): boolean {
  const lowerValue = value.toLowerCase()
  if (!lowerValue.startsWith('http://') && !lowerValue.startsWith('https://')) {
    return false
  }
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

function normalizeRecord(value: unknown): Record<string, unknown> {
  return isRecord(value) ? value : {}
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function readError(error: unknown, fallback: string) {
  return `${fallback}: ${toUserMessage(error, '请稍后再试')}`
}

const DEFAULT_SETTINGS_IDENTITY_META = '站点身份配置'

const configs: Record<string, ModuleConfig> = {
  articles: {
    title: '文章管理',
    description: '草稿、发布、私密可见性、分类、标签和版本记录都在这里完成。',
    primaryAction: '新建文章',
    capabilities: ['Markdown 编辑', '封面上传', '推荐/置顶', '私密可见名单', '版本回滚预留'],
    rows: [
      { title: '把个人站点做成主题档案馆', meta: '产品设计 · 公开 · 2 天前', status: 'PUBLISHED' },
      { title: '朋友可见：季度复盘片段', meta: '个人知识库 · 选中好友', status: 'PRIVATE' },
      { title: '演示草稿：作品上传流程', meta: '幕后日志 · 未发布', status: 'DRAFT' },
    ],
  },
  projects: {
    title: '作品管理',
    description: '维护作品封面、截图、过程、时间线、技术栈和外部链接。',
    primaryAction: '新增作品',
    capabilities: ['作品截图', 'Demo/GitHub/视频链接', '过程记录', '里程碑时间线', '推荐展示'],
    rows: [
      { title: '内容整理后台', meta: '管理控制台 · Vue 3 / Spring Boot', status: 'VISIBLE' },
      { title: '主题博客前台', meta: '主题前台 · WebGL / GSAP', status: 'VISIBLE' },
      { title: '阅读动效实验', meta: '动效研究 · anime.js', status: 'DRAFT' },
    ],
  },
  inspirations: {
    title: '灵感墙管理',
    description: '摘句、图片、链接、Prompt、草图和参考资料可以快速沉淀。',
    primaryAction: '重置表单',
    capabilities: [],
    rows: [],
  },
  comments: {
    title: '评论审核',
    description: '评论、回复、点赞和敏感词审核进入统一互动工作流。',
    primaryAction: '查看待审',
    capabilities: [],
    rows: [],
  },
  guestbook: {
    title: '留言板管理',
    description: '审核用户留言，管理公开留言列表。',
    primaryAction: '查看待审',
    capabilities: [],
    rows: [],
  },
  files: {
    title: '文件资源',
    description: '本地文件、封面、截图、附件和引用关系集中管理。',
    primaryAction: '刷新列表',
    capabilities: [],
    rows: [],
  },
  themes: {
    title: '主题配置',
    description: '主题不仅是换色，还包含字体、密度、卡片、动效和首页模块顺序。',
    primaryAction: '保存主题',
    capabilities: ['主题变量 JSON', '主题版本', '资源绑定', '预览模式', '当前主题切换'],
    rows: [
      { title: '玻璃空间', meta: '玻璃星空风 · 当前启用', status: 'ACTIVE' },
      { title: '赛博夜色', meta: '暗色科技风 · 预设', status: 'PRESET' },
      { title: '极简白', meta: '极简阅读风 · 预设', status: 'PRESET' },
    ],
  },
  settings: {
    title: '站点设置',
    description: '站点身份、导航、社交链接、SEO、首页编排和关于页内容从后台配置。',
    primaryAction: '保存配置',
    capabilities: ['导航配置', 'SEO 描述', '社交链接', '首页模块排序', '关于页内容块'],
    rows: [
      { title: '站点身份', meta: DEFAULT_SETTINGS_IDENTITY_META, status: 'READY' },
      { title: '首页推荐', meta: '文章 / 作品 / 灵感', status: 'READY' },
    ],
  },
}
</script>

<style scoped>
.module-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  min-height: 152px;
  padding: 24px 28px;
  border: 1px solid var(--tone-line);
  border-radius: var(--app-radius-sm);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.58)),
    radial-gradient(circle at 16% 20%, rgba(49, 91, 255, 0.14), transparent 34%),
    radial-gradient(circle at 88% 18%, rgba(0, 124, 114, 0.16), transparent 28%),
    linear-gradient(120deg, rgba(49, 91, 255, 0.08), rgba(194, 95, 58, 0.08), rgba(0, 124, 114, 0.1));
  box-shadow: var(--tone-shadow);
}

.module-hero h2 {
  max-width: 830px;
  margin: 0;
  color: var(--admin-ink);
  font-size: 32px;
  font-weight: 860;
  line-height: 1.16;
}

.module-hero p {
  max-width: 680px;
  margin: 10px 0 0;
  color: var(--tone-muted);
  font-size: 14px;
  line-height: 1.65;
}

.admin-dashboard,
.admin-module {
  display: grid;
  gap: 18px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 136px;
  padding: 20px;
}

.metric-card strong {
  display: block;
  margin-top: 18px;
  font-size: 36px;
  line-height: 1;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(300px, 0.75fr);
  gap: 12px;
  margin-top: 12px;
}

.workspace-grid--even {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-line > * {
  flex: 1;
  min-width: 160px;
}

.settings-builder {
  gap: 18px;
}

.settings-section {
  display: grid;
  gap: 12px;
  padding-top: 4px;
}

.settings-editor-note {
  margin: -4px 0 2px;
  color: var(--tone-muted);
  font-size: 13px;
  line-height: 1.6;
}

.settings-section + .settings-section {
  padding-top: 18px;
  border-top: 1px solid var(--tone-line);
}

.settings-section-heading,
.settings-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings-section-heading h3 {
  margin: 0;
  color: var(--admin-ink);
  font-size: 15px;
  font-weight: 860;
}

.settings-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
}

.settings-card-title strong {
  min-width: 0;
  overflow: hidden;
  color: var(--tone-strong);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-check {
  justify-content: flex-start;
  color: #344154;
  font-size: 13px;
  font-weight: 760;
}

.json-fallback {
  display: grid;
  gap: 8px;
}

.json-fallback summary {
  color: var(--tone-muted);
  font-size: 13px;
  font-weight: 820;
  cursor: pointer;
}

.json-fallback textarea {
  min-height: 112px;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
}

.empty-editor {
  margin: 0;
  color: var(--tone-muted);
  font-size: 13px;
}

.settings-actions {
  justify-content: flex-end;
  padding-top: 2px;
}

.settings-card input:disabled {
  background: rgba(17, 24, 39, 0.06);
  color: var(--tone-muted);
  cursor: not-allowed;
}

.tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-picker .check-line {
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid var(--tone-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #344154;
  font-size: 12px;
  font-weight: 760;
}

.check-line {
  display: flex !important;
  grid-template-columns: none;
  align-items: center;
  gap: 10px;
}

.check-line input {
  width: 18px;
  min-height: 18px;
}

.panel-title h2 {
  font-size: 18px;
}

.theme-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 8px;
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 50%;
  vertical-align: -1px;
}

@media (min-width: 761px) {
  .module-hero h2 {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 1020px) {
  .module-hero,
  .workspace-grid,
  .workspace-grid--even {
    grid-template-columns: 1fr;
  }

  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .module-hero {
    align-items: flex-start;
    padding: 18px;
  }

  .module-hero h2 {
    font-size: 28px;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .panel-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-row,
  .row-actions,
  .filter-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .row-actions,
  .filter-bar > * {
    width: 100%;
  }
}

.rule-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
}
</style>
