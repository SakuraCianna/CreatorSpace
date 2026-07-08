<template>
  <div class="csdn-editor-page">
    <!-- 顶部工具栏 -->
    <header class="editor-header">
      <div class="header-left">
        <RouterLink :to="`/users/${session.currentUser?.id || ''}`" class="back-link">
          <ChevronLeft :size="18" />
          <span>个人中心</span>
        </RouterLink>
        <span class="divider"></span>
        <div class="header-title dropdown-trigger" @click="showTypeDropdown = !showTypeDropdown">
          {{ currentPostTypeLabel }}
          <ChevronDown :size="14" class="dropdown-icon" :class="{ 'rotated': showTypeDropdown }" />
          <div class="dropdown-menu" v-if="showTypeDropdown">
            <div class="dropdown-item" @click.stop="setPostType('article')">发布文章</div>
            <div class="dropdown-item" @click.stop="setPostType('project')">发布作品</div>
            <div class="dropdown-item" @click.stop="setPostType('idea')">发布灵感</div>
          </div>
        </div>
        <div
          v-if="currentPostType === 'article'"
          class="header-theme-chip"
          :style="blogThemeStyle"
          :title="`当前样式：${currentBlogTheme.displayName}（${blogThemeMeta}）`"
        >
          <span class="header-theme-dot" :style="{ background: currentBlogTheme.accentColor }"></span>
          <span class="header-theme-label">{{ currentBlogTheme.displayName }}</span>
        </div>
        <span class="header-status" v-if="editingArticleId">编辑草稿</span>
      </div>

      <div class="header-toolbar">
        <button class="tool-btn" title="撤销" :disabled="!canUndo" @click="undoEdit"><Undo :size="16" /><span>撤销</span></button>
        <button class="tool-btn" title="重做" :disabled="!canRedo" @click="redoEdit"><Redo :size="16" /><span>重做</span></button>
        <span class="tool-divider"></span>
        <button class="tool-btn" title="一级标题" @click="insertText('# ', '', '一级标题')"><Heading1 :size="16" /><span>一级标题</span></button>
        <button class="tool-btn" title="二级标题" @click="insertText('## ', '', '二级标题')"><Heading2 :size="16" /><span>二级标题</span></button>
        <button class="tool-btn" title="加粗" @click="insertText('**', '**', '粗体')"><Bold :size="16" /><span>加粗</span></button>
        <button class="tool-btn" title="斜体" @click="insertText('*', '*', '斜体')"><Italic :size="16" /><span>斜体</span></button>
        <button class="tool-btn" title="删除线" @click="insertText('~~', '~~', '删除线')"><Strikethrough :size="16" /><span>删除线</span></button>
        <span class="tool-divider"></span>
        <button class="tool-btn" title="无序列表" @click="insertText('- ', '', '列表项')"><List :size="16" /><span>列表</span></button>
        <button class="tool-btn" title="有序列表" @click="insertText('1. ', '', '列表项')"><ListOrdered :size="16" /><span>有序</span></button>
        <button class="tool-btn" title="表格" @click="showTableModal = true"><Table :size="16" /><span>表格</span></button>
        <span class="tool-divider"></span>
        <button class="tool-btn" title="代码块" @click="insertText('```\n', '\n```', 'code')"><Code :size="16" /><span>代码</span></button>
        <button class="tool-btn" title="引用" @click="insertText('> ', '', '引用内容')"><Quote :size="16" /><span>引用</span></button>
        <button class="tool-btn" title="分割线" @click="insertText('\n---\n', '', '')"><Minus :size="16" /><span>分割线</span></button>
        <span class="tool-divider"></span>
        <button class="tool-btn" title="图片" @click="insertImageMarkdown"><Image :size="16" /><span>图片</span></button>
        <button class="tool-btn" title="链接" @click="insertLinkMarkdown"><Link :size="16" /><span>链接</span></button>
        <button class="tool-btn" title="AI助手" @click="showAIAssistant = !showAIAssistant" :class="{ 'active': showAIAssistant }"><Sparkles :size="16" /><span>AI助手</span></button>
        <span class="tool-divider"></span>
        <button class="tool-btn" title="分栏编辑" :class="{ active: editorMode === 'split' }" @click="editorMode = 'split'"><Columns2 :size="16" /><span>分栏</span></button>
        <button class="tool-btn" title="只写作" :class="{ active: editorMode === 'write' }" @click="editorMode = 'write'"><PencilLine :size="16" /><span>写作</span></button>
        <button class="tool-btn" title="只预览" :class="{ active: editorMode === 'preview' }" @click="editorMode = 'preview'"><Eye :size="16" /><span>预览</span></button>
      </div>

      <div class="header-right">
        <span class="word-count">共 {{ wordCount }} 字</span>
        <button class="btn-draft" @click="saveCurrentDraft">保存草稿</button>
        <button class="btn-publish" @click="publishCurrent">发布{{ currentPostType === 'article' ? '文章' : (currentPostType === 'project' ? '作品' : '灵感') }}</button>
        <div class="avatar-wrap">
          <img :src="session.currentUser?.avatarUrl || 'https://api.dicebear.com/7.x/notionists/svg?seed=creator'" alt="avatar" class="avatar" />
        </div>
      </div>
    </header>

    <main class="editor-main">
      <!-- 左侧大纲 -->
      <aside class="editor-sidebar-left" v-if="currentPostType === 'article'">
        <div class="sidebar-header">
          <span>目录</span>
          <ChevronsLeftRight :size="16" class="collapse-icon" />
        </div>
        <div class="toc-container">
          <ul class="toc-list" v-if="toc.length > 0">
            <li v-for="item in toc" :key="item.id" :style="{ paddingLeft: `${(item.level - 1) * 12}px` }" :class="`toc-level-${item.level}`">
              <a :href="`#${item.id}`" class="toc-link" @click.prevent="scrollToHeading(item.id)">{{ item.text }}</a>
            </li>
          </ul>
          <div class="toc-empty" v-else>
            添加标题即可在此生成目录
          </div>
        </div>
      </aside>

      <!-- 中间编辑器区域 -->
      <section class="editor-content" v-if="currentPostType === 'article'">
        <div class="markdown-workspace" :class="`markdown-workspace--${editorMode}`">
          <!-- 左侧输入栏 -->
          <div v-show="editorMode !== 'preview'" class="markdown-column">
            <div class="title-input-wrapper">
              <input
                type="text"
                class="title-input"
                v-model="articleForm.title"
                placeholder="请输入文章标题"
                maxlength="100"
              />
            </div>
            <textarea
              ref="textareaRef"
              class="markdown-input"
              v-model="articleForm.contentMarkdown"
              placeholder="在这里开始您的专业创作...&#10;支持 Markdown 语法，左侧编写，右侧实时无缝预览。"
              @input="updateWordCount"
              @keydown="handleEditorKeydown"
              @scroll="syncScroll"
            ></textarea>
          </div>
          <!-- 右侧预览栏 -->
          <div
            v-show="editorMode !== 'write'"
            class="markdown-column preview-column article-preview-surface"
            :style="blogThemeStyle"
            :data-blog-canvas="currentBlogTheme.canvasType"
            :data-blog-layout="currentBlogTheme.layoutStyle"
            :data-blog-block="currentBlogTheme.blockStyle"
          >
            <div class="preview-title-wrapper">
              <h1 class="preview-title" v-if="articleForm.title">{{ articleForm.title }}</h1>
              <p class="preview-summary" v-if="articleForm.summary">{{ articleForm.summary }}</p>
            </div>
            <div class="markdown-preview" ref="previewRef" v-html="renderedHtml"></div>
          </div>
        </div>
      </section>

      <!-- 发布作品表单 -->
      <section class="editor-content form-layout" v-else-if="currentPostType === 'project'">
        <div class="project-form scroll-container">
          <h2 class="form-page-title">发布您的作品</h2>
          
          <div class="form-grid">
            <div class="form-group-large">
              <label>作品名称 <span class="required">*</span></label>
              <input type="text" class="form-input-large" v-model="projectForm.title" placeholder="如：CreatorSpace - 下一代创作平台" />
            </div>
            <div class="form-group-large">
              <label>作品类型 <span class="required">*</span></label>
              <BaseSelect v-model="projectForm.projectType" :options="projectTypeOptions" placeholder="请选择类型" />
            </div>
          </div>
          
          <div class="form-group-large" style="margin-top: 24px;">
            <label>技术栈 (用逗号分隔)</label>
            <input type="text" class="form-input-large" v-model="projectForm.techStack" placeholder="Vue3, Spring Boot, PostgreSQL" />
          </div>
          
          <div class="form-grid" style="margin-top: 24px;">
            <div class="form-group-large">
              <label>作品封面图</label>
              <FileUpload v-model="projectForm.coverUrl" module="OTHER" accept="image/*" hint="建议尺寸 16:9，不超过 5MB" />
            </div>
            <div class="form-group-large">
              <label>Demo URL</label>
              <input type="text" class="form-input-large" v-model="projectForm.demoUrl" placeholder="https://demo.example.com" />
            </div>
          </div>
          
          <div class="form-grid" style="margin-top: 24px;">
            <div class="form-group-large">
              <label>开源代码仓库 URL</label>
              <input type="text" class="form-input-large" v-model="projectForm.githubUrl" placeholder="https://github.com/..." />
            </div>
            <div class="form-group-large">
              <label>视频 URL (可选)</label>
              <input type="text" class="form-input-large" v-model="projectForm.videoUrl" placeholder="https://..." />
            </div>
          </div>
          
          <div class="form-group-large" style="margin-top: 24px;">
            <label>作品描述详情</label>
            <textarea class="form-textarea-large" v-model="projectForm.contentMarkdown" rows="12" placeholder="详细介绍这个作品的背景、技术栈、核心功能...支持 Markdown 语法"></textarea>
          </div>
        </div>
      </section>

      <!-- 发布灵感表单 -->
      <section class="editor-content idea-layout" v-else-if="currentPostType === 'idea'">
        <div class="idea-form scroll-container">
          <h2 class="form-page-title">分享您的灵感</h2>
          
          <div class="idea-tabs">
            <button class="idea-tab" :class="{'active': ideaForm.cardType === 'TEXT'}" @click="ideaForm.cardType = 'TEXT'">纯文字</button>
            <button class="idea-tab" :class="{'active': ideaForm.cardType === 'IMAGE'}" @click="ideaForm.cardType = 'IMAGE'">带配图</button>
            <button class="idea-tab" :class="{'active': ideaForm.cardType === 'LINK'}" @click="ideaForm.cardType = 'LINK'">外链接</button>
            <button class="idea-tab" :class="{'active': ideaForm.cardType === 'PROMPT'}" @click="ideaForm.cardType = 'PROMPT'">提示词</button>
            <button class="idea-tab" :class="{'active': ideaForm.cardType === 'CODE'}" @click="ideaForm.cardType = 'CODE'">代码段</button>
          </div>
          
          <div class="form-group-large" style="margin-top: 24px;">
            <label>灵感标题 <span class="required">*</span></label>
            <input type="text" class="form-input-large" v-model="ideaForm.title" placeholder="一句话概括你的灵感" />
          </div>
          
          <div class="form-group-large" style="margin-top: 24px;">
            <label>详细内容</label>
            <textarea class="idea-textarea form-textarea-large" v-model="ideaForm.content" rows="6" placeholder="今天有什么新的奇思妙想？可以直接在这里记录..."></textarea>
          </div>
          
          <div class="form-group-large" v-if="ideaForm.cardType === 'IMAGE'" style="margin-top: 24px;">
            <label>配图上传</label>
            <FileUpload v-model="ideaForm.imageUrl" module="OTHER" accept="image/*" hint="上传一张代表灵感的配图" />
          </div>
          
          <div class="form-group-large" v-if="ideaForm.cardType === 'LINK'" style="margin-top: 24px;">
            <label>来源链接</label>
            <input type="text" class="form-input-large" v-model="ideaForm.sourceUrl" placeholder="https://..." />
          </div>
          
          <div class="form-group-large" style="margin-top: 24px;">
            <label>高光色配置 (可选)</label>
            <div class="color-picker-wrap">
              <input type="color" v-model="ideaForm.color" class="color-input" />
              <span class="color-value">{{ ideaForm.color || '未设置' }}</span>
              <button class="btn-clear-color" v-if="ideaForm.color" @click="ideaForm.color = ''">清除</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 右侧 AI 助手 -->
      <aside class="editor-sidebar-right" v-if="showAIAssistant && currentPostType === 'article'">
        <div class="ai-assistant">
          <div class="ai-header">
            <div>
              <div class="ai-title">
                <Sparkles :size="18" color="#315bff" /> AI 创作助手
              </div>
            </div>
            <X :size="16" class="close-icon" @click="showAIAssistant = false" style="cursor: pointer;" />
          </div>

          <div class="ai-body" :class="{ 'ai-body--chat': aiChatActive }">
            <template v-if="!aiChatActive">
            <div class="ai-section">
              <div class="ai-section-title">
                <span>灵感话题</span>
                <button class="btn-refresh" @click="fetchHotTopics" :disabled="isLoadingTopics"><RefreshCw :size="12" style="margin-right: 4px;" :class="{ 'spin': isLoadingTopics }" /> 换一换</button>
              </div>
              <div class="ai-tags">
                <span class="ai-tag" v-for="(topic, index) in hotTopics" :key="index" @click="insertTopic(topic)">{{ topic }}</span>
                <span v-if="hotTopics.length === 0 && isLoadingTopics" class="ai-loading">加载中...</span>
              </div>
            </div>

            <div class="ai-section">
              <div class="ai-section-title"><span>快捷生成</span></div>
              <div class="ai-quick-actions">
                <button
                  v-for="action in aiQuickActions"
                  :key="action.mode"
                  class="ai-action-btn"
                  :class="{ active: activeAiMode === action.mode }"
                  type="button"
                  :disabled="isGeneratingAiText"
                  @click="runQuickAction(action.mode)"
                >
                  <component :is="action.icon" :size="14" />
                  <span>{{ action.label }}</span>
                </button>
              </div>
            </div>

            <div class="ai-result-panel" v-if="aiResult.text || aiResult.notice || isGeneratingAiText">
              <div class="ai-result-head">
                <span>{{ aiResultTitle }}</span>
                <span class="ai-result-state" v-if="isGeneratingAiText">生成中</span>
              </div>
              <div class="ai-result-notice" v-if="aiResult.notice">{{ aiResult.notice }}</div>
              <div class="ai-result-text ai-markdown" v-if="aiResult.text || isGeneratingAiText" v-html="renderAiMarkdown(aiResult.text || '正在整理你的草稿...')"></div>
              <div class="ai-result-actions" v-if="aiResult.text">
                <button type="button" @click="insertAiResult"><Check :size="13" />插入正文</button>
                <button type="button" @click="replaceSelectionWithAiResult" :disabled="!canReplaceSelection"><WandSparkles :size="13" />替换选区</button>
                <button type="button" @click="fillSummaryFromAiResult"><FileText :size="13" />填入摘要</button>
                <button type="button" v-if="aiResult.mode === 'TITLE'" @click="useAiResultAsTitle"><FileText :size="13" />设为标题</button>
                <button type="button" @click="copyAiResult"><Copy :size="13" />复制</button>
              </div>
            </div>
            </template>

            <div class="ai-chat-thread" v-else ref="aiThreadRef">
              <div class="ai-chat-topline">
                <span>对话中</span>
                <button type="button" @click="resetAiChat" :disabled="isGeneratingAiText">新对话</button>
              </div>
              <div
                v-for="message in aiChatMessages"
                :key="message.id"
                class="ai-message"
                :class="`ai-message--${message.role}`"
              >
                <div class="ai-message-label">{{ message.role === 'user' ? '我' : 'AI' }}</div>
                <div v-if="message.role === 'user'" class="ai-message-text">{{ message.content }}</div>
                <div v-else class="ai-message-content">
                  <div
                    class="ai-message-markdown ai-markdown"
                    v-html="renderAiMarkdown(message.content || (message.pending ? '正在思考...' : ''))"
                  ></div>
                  <div class="ai-result-notice" v-if="message.notice">{{ message.notice }}</div>
                </div>
              </div>
            </div>

            <div class="ai-chat-area">
              <div class="ai-disclaimer">内容由 AI 生成，采纳前请自行核对事实与来源</div>

              <div class="ai-input-box">
                <Sparkles :size="16" color="#71717a" class="ai-input-icon" />
                <input type="text" placeholder="输入创作要求，AI 帮你写" v-model="aiPrompt" @keydown.enter.prevent="generateAiText('CUSTOM')" />
                <button class="btn-send" @click="generateAiText('CUSTOM')" :disabled="isGeneratingAiText || !aiPrompt.trim()">
                  <RefreshCw :size="14" color="#fff" class="spin" v-if="isGeneratingAiText" />
                  <Send :size="14" color="#fff" v-else />
                </button>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <!-- 发布设置弹窗 -->
    <div class="modal-overlay" v-if="showPublishModal" @click.self="showPublishModal = false">
      <div class="publish-modal">
        <div class="modal-header">
          <h3>发布设置</h3>
          <X :size="20" class="close-icon" @click="showPublishModal = false" style="cursor: pointer;" />
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>文章分类</label>
            <BaseSelect v-model="articleForm.categoryId" :options="categoryOptions" placeholder="请选择分类" />
          </div>
          <div class="form-group">
            <label>可见性</label>
            <BaseSelect v-model="articleForm.privacyType" :options="privacyOptions" placeholder="请选择可见性" />
          </div>
          <div class="form-group" v-if="showFriendSelector">
            <label>
              {{ articleForm.privacyType === 'SELECTED_FRIENDS' ? '选择可见的好友' : '选择排除的好友' }}
              <span class="selected-count">已选 {{ selectedVisibilityUserIds.size }} 人</span>
            </label>
            <div class="friend-selector">
              <div class="friend-search">
                <input
                  type="text"
                  v-model="friendSearchQuery"
                  placeholder="搜索好友..."
                  class="friend-search-input"
                />
              </div>
              <div class="friend-list">
                <div v-if="isLoadingFriends" class="friend-loading">加载好友中...</div>
                <div v-else-if="myFriends.length === 0" class="friend-empty">暂无好友，请先添加好友</div>
                <label
                  v-for="friend in filteredFriends"
                  :key="friend.friendId"
                  class="friend-item"
                  :class="{ 'is-selected': selectedVisibilityUserIds.has(friend.friendId) }"
                >
                  <input
                    type="checkbox"
                    :checked="selectedVisibilityUserIds.has(friend.friendId)"
                    @change="toggleVisibilityUser(friend.friendId)"
                    class="friend-checkbox"
                  />
                  <img
                    :src="friend.avatarUrl || 'https://api.dicebear.com/7.x/notionists/svg?seed=' + friend.username"
                    class="friend-avatar"
                    alt=""
                  />
                  <span class="friend-name">{{ friend.nickname || friend.username }}</span>
                </label>
                <div v-if="filteredFriends.length === 0 && myFriends.length > 0" class="friend-empty">无匹配的好友</div>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>文章标签 (可多选)</label>
            <div class="tags-selector">
              <label v-for="tag in tagOptions" :key="tag.id" class="tag-checkbox">
                <input type="checkbox" :value="tag.id" v-model="articleForm.tagIds" />
                {{ tag.name }}
              </label>
              <span v-if="tagOptions.length === 0" class="form-empty-hint">暂无可用标签</span>
            </div>
          </div>
          <div class="form-group">
            <label>封面图片 (可选)</label>
            <FileUpload v-model="articleForm.coverUrl" module="COVER" accept="image/*" hint="建议 16:9，最大 10MB" @error="showNotice" />
            <img v-if="articleForm.coverUrl" :src="articleForm.coverUrl" class="cover-preview" />
          </div>
          <div class="form-group">
            <label>文章摘要 (可选)</label>
            <textarea v-model="articleForm.summary" placeholder="输入文章摘要..." class="form-textarea" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>博客外观</label>
            <div class="theme-inherit-card" :style="blogThemeStyle">
              <div>
                <strong>{{ currentBlogTheme.displayName }}</strong>
                <span>{{ blogThemeMeta }}</span>
              </div>
              <i :style="{ background: currentBlogTheme.accentColor }"></i>
              <RouterLink :to="`/users/${session.currentUser?.id || ''}`">调整外观</RouterLink>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showPublishModal = false">取消</button>
          <button class="btn-publish-confirm" @click="confirmPublish">确认发布</button>
        </div>
      </div>
    </div>

    <!-- 表格生成弹窗 -->
    <div class="modal-overlay" v-if="showTableModal" @click.self="showTableModal = false">
      <div class="publish-modal" style="width: 320px;">
        <div class="modal-header">
          <h3>插入表格</h3>
          <X :size="20" class="close-icon" @click="showTableModal = false" style="cursor: pointer;" />
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>行数 (包含表头)</label>
            <input type="number" v-model.number="tableConfig.rows" class="form-input" min="2" max="20" />
          </div>
          <div class="form-group">
            <label>列数</label>
            <input type="number" v-model.number="tableConfig.cols" class="form-input" min="1" max="10" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showTableModal = false">取消</button>
          <button class="btn-publish-confirm" @click="insertTable">插入</button>
        </div>
      </div>
    </div>

    <!-- 弹窗提示 -->
    <div v-if="notice" class="global-toast">{{ notice }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '../shared/sessionStore'
import type { ArticlePrivacy, BlogThemeConfig, FriendVO, InspirationPayload, InspirationType, ProjectPayload, TagSummary, VisibilityUserVO } from '../shared/domain'
import {
  createCreatorArticle,
  createCreatorInspiration,
  createCreatorProject,
  publishCreatorInspiration,
  updateCreatorArticle,
  updateCreatorInspiration,
  updateCreatorProject,
  fetchCreatorArticle,
  fetchCategories,
  fetchTags,
  fetchMyBlogTheme,
  submitCreatorArticle,
  submitCreatorProject,
  fetchMyFriends,
  fetchArticleVisibilityUsers,
  addArticleVisibilityUser,
  removeArticleVisibilityUser
} from '../services/content'
import { streamCreatorAiText } from '../services/content'
import type { CreatorAiResponse } from '../services/content'
import { toUserMessage, requestJson } from '../services/http'
import { DEFAULT_BLOG_THEME, blogThemeToStyle, normalizeBlogTheme } from '../shared/blogTheme'
import { renderSafeMarkdown } from '../shared/markdown'
import {
  ChevronLeft, ChevronDown, ChevronsLeftRight, Undo, Redo, Bold, Italic, Strikethrough,
  List, ListOrdered, Code, Quote, Image, Link, Sparkles, X, RefreshCw,
  ListTree, Send, Table, Minus, Copy, Check, FileText, WandSparkles,
  Heading1, Heading2, Columns2, PencilLine, Eye
} from '@lucide/vue'
import MarkdownIt from 'markdown-it'
import FileUpload from '../components/common/FileUpload.vue'
import BaseSelect from '../shared/components/BaseSelect.vue'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const notice = ref('')
const editingArticleId = ref<number | null>(null)
const editingProjectId = ref<number | null>(null)
const editingIdeaId = ref<number | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const wordCount = ref(0)
const toc = ref<{id: string, text: string, level: number}[]>([])
const showAIAssistant = ref(true)
const editorMode = ref<'split' | 'write' | 'preview'>('split')
const tagOptions = ref<TagSummary[]>([])
const articleCategoryOptions = ref<{ label: string, value: number }[]>([])
const blogTheme = ref<BlogThemeConfig>({ ...DEFAULT_BLOG_THEME })
const currentBlogTheme = computed(() => normalizeBlogTheme(blogTheme.value))
const blogThemeStyle = computed(() => blogThemeToStyle(currentBlogTheme.value))
const blogThemeMeta = computed(() => {
  const font = blogOptionLabel(currentBlogTheme.value.fontPreset, {
    'literary-serif': '文学衬线',
    'neo-grotesk': '现代无衬线',
    'rounded-sans': '圆润日记',
    'mono-editor': '编辑器等宽',
  })
  const canvas = blogOptionLabel(currentBlogTheme.value.canvasType, {
    'soft-paper': '柔纸',
    linen: '织纹',
    gradient: '渐变',
    image: '图片画布',
  })
  const layout = blogOptionLabel(currentBlogTheme.value.layoutStyle, {
    editorial: '杂志版式',
    notebook: '手账版式',
    gallery: '画廊版式',
  })
  return `${font} / ${canvas} / ${layout}`
})

const md = new MarkdownIt({ html: false, breaks: true, linkify: true })
const renderedHtml = ref('')
const previewRef = ref<HTMLDivElement | null>(null)
let headingRenderIndex = 0

md.renderer.rules.heading_open = (tokens, idx, options, _env, self) => {
  const inlineToken = tokens[idx + 1]
  const text = inlineToken?.type === 'inline' ? inlineToken.content : 'section'
  tokens[idx].attrSet('id', headingId(text, headingRenderIndex++))
  return self.renderToken(tokens, idx, options)
}

const historyStack = ref<string[]>([''])
const redoStack = ref<string[]>([])
const canUndo = computed(() => historyStack.value.length > 1)
const canRedo = computed(() => redoStack.value.length > 0)
let historyTimer: ReturnType<typeof setTimeout> | null = null
let isApplyingHistory = false

const showTypeDropdown = ref(false)
const currentPostType = ref('article')
const currentPostTypeLabel = computed(() => {
  if (currentPostType.value === 'project') return '发布作品'
  if (currentPostType.value === 'idea') return '发布灵感'
  return '发布文章'
})

function setPostType(type: string) {
  currentPostType.value = type
  showTypeDropdown.value = false
}

const showPublishModal = ref(false)
const privacyOptions: Array<{ label: string, value: ArticlePrivacy }> = [
  { label: '公开', value: 'PUBLIC' },
  { label: '仅自己', value: 'SELF' },
  { label: '仅好友', value: 'FRIENDS' },
  { label: '指定好友', value: 'SELECTED_FRIENDS' },
  { label: '排除指定好友', value: 'EXCLUDED_FRIENDS' },
]

function openPublishModal() {
  if (!articleForm.title.trim() || !articleForm.contentMarkdown.trim()) {
    showNotice('请填写标题和正文')
    return
  }
  showPublishModal.value = true
}

async function confirmPublish() {
  showPublishModal.value = false
  const saved = await publishArticle()
  return saved
}

async function saveCurrentDraft() {
  if (currentPostType.value === 'project') {
    await saveProjectDraft()
    return
  }
  if (currentPostType.value === 'idea') {
    await saveIdeaDraft()
    return
  }
  await saveArticle()
}

async function publishCurrent() {
  if (currentPostType.value === 'project') {
    await publishProject()
    return
  }
  if (currentPostType.value === 'idea') {
    await publishIdea()
    return
  }
  openPublishModal()
}

const articleForm = reactive({
  title: '',
  slug: '',
  summary: '',
  contentMarkdown: '',
  coverUrl: '',
  categoryId: null as number | null,
  tagIds: [] as number[],
  privacyType: 'PUBLIC' as ArticlePrivacy,
})

const myFriends = ref<FriendVO[]>([])
const visibilityUsers = ref<VisibilityUserVO[]>([])
const selectedVisibilityUserIds = ref<Set<number>>(new Set())
const friendSearchQuery = ref('')
const isLoadingFriends = ref(false)

const showFriendSelector = computed(() =>
  articleForm.privacyType === 'SELECTED_FRIENDS' || articleForm.privacyType === 'EXCLUDED_FRIENDS'
)

const filteredFriends = computed(() => {
  const query = friendSearchQuery.value.trim().toLowerCase()
  if (!query) return myFriends.value
  return myFriends.value.filter(f => {
    const name = (f.nickname || f.username).toLowerCase()
    return name.includes(query)
  })
})

async function loadFriends() {
  if (myFriends.value.length > 0) return
  isLoadingFriends.value = true
  try {
    const res = await fetchMyFriends({ pageSize: 100 })
    myFriends.value = res.records
  } catch (error) {
    showNotice(readError(error, '加载好友列表失败'))
  } finally {
    isLoadingFriends.value = false
  }
}

async function loadVisibilityUsers(articleId: number) {
  try {
    const users = await fetchArticleVisibilityUsers(articleId)
    visibilityUsers.value = users
    selectedVisibilityUserIds.value = new Set(users.map(u => u.userId))
  } catch (error) {
    showNotice(readError(error, '加载可见性规则失败'))
  }
}

function toggleVisibilityUser(userId: number) {
  const set = new Set(selectedVisibilityUserIds.value)
  if (set.has(userId)) {
    set.delete(userId)
  } else {
    set.add(userId)
  }
  selectedVisibilityUserIds.value = set
}

async function syncVisibilityUsers(articleId: number) {
  const ruleType = articleForm.privacyType === 'SELECTED_FRIENDS' ? 'ALLOW' : 'DENY'
  const currentIds = new Set(visibilityUsers.value.map(u => u.userId))
  const desiredIds = selectedVisibilityUserIds.value

  const toRemove = [...currentIds].filter(id => !desiredIds.has(id))
  const toAdd = [...desiredIds].filter(id => !currentIds.has(id))

  for (const userId of toRemove) {
    try {
      await removeArticleVisibilityUser(articleId, userId)
    } catch { /* ignore */ }
  }

  for (const userId of toAdd) {
    try {
      await addArticleVisibilityUser(articleId, userId, ruleType)
    } catch (error) {
      showNotice(readError(error, `添加用户可见性规则失败`))
    }
  }
}

const projectForm = reactive({
  title: '',
  slug: '',
  description: '',
  projectType: '',
  techStack: '',
  coverUrl: '',
  demoUrl: '',
  githubUrl: '',
  videoUrl: '',
  contentMarkdown: ''
})

const ideaForm = reactive({
  title: '',
  content: '',
  imageUrl: '',
  cardType: 'TEXT' as InspirationType,
  sourceUrl: '',
  color: ''
})

const projectTypeOptions = [
  { label: 'Web 应用', value: 'WEB' },
  { label: '移动端 App', value: 'MOBILE' },
  { label: 'AI 模型 / Agent', value: 'AI' },
  { label: '设计作品', value: 'DESIGN' },
  { label: '其他', value: 'OTHER' }
]

const categoryOptions = computed(() => articleCategoryOptions.value)

const showTableModal = ref(false)
const tableConfig = reactive({
  rows: 3,
  cols: 3
})

function insertTable() {
  let { rows, cols } = tableConfig
  rows = Math.max(2, Math.min(20, rows))
  cols = Math.max(1, Math.min(10, cols))

  let tableMd = '\n'
  // Header row
  tableMd += '|'
  for (let c = 0; c < cols; c++) {
    tableMd += ` 列${c + 1} |`
  }
  tableMd += '\n|'
  // Divider row
  for (let c = 0; c < cols; c++) {
    tableMd += ` --- |`
  }
  tableMd += '\n'
  // Data rows
  for (let r = 0; r < rows - 1; r++) {
    tableMd += '|'
    for (let c = 0; c < cols; c++) {
      tableMd += ` 内容 |`
    }
    tableMd += '\n'
  }
  tableMd += '\n'

  insertText(tableMd, '', '')
  showTableModal.value = false
}

type AiMode = 'OUTLINE' | 'CONTINUE' | 'POLISH' | 'SUMMARY' | 'TITLE' | 'TAGS' | 'CODE' | 'RESEARCH' | 'CUSTOM'
const AI_MODES = new Set<AiMode>(['OUTLINE', 'CONTINUE', 'POLISH', 'SUMMARY', 'TITLE', 'TAGS', 'CODE', 'RESEARCH', 'CUSTOM'])

interface AiChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  mode?: AiMode
  notice?: string
  pending?: boolean
}

const hotTopics = ref<string[]>([])
const isLoadingTopics = ref(false)
const aiPrompt = ref('')
const isGeneratingAiText = ref(false)
const activeAiMode = ref<AiMode | null>(null)
const aiResult = reactive<CreatorAiResponse>({ mode: 'CUSTOM', text: '', notice: '' })
const aiSelectionRange = reactive({ start: 0, end: 0 })
const aiChatMessages = ref<AiChatMessage[]>([])
const aiThreadRef = ref<HTMLDivElement | null>(null)
const aiChatActive = computed(() => aiChatMessages.value.length > 0)
let aiMessageId = 0

const aiQuickActions = [
  { mode: 'OUTLINE' as const, label: '大纲', icon: ListTree },
  { mode: 'CONTINUE' as const, label: '续写', icon: Sparkles },
  { mode: 'POLISH' as const, label: '润色', icon: WandSparkles },
  { mode: 'SUMMARY' as const, label: '摘要', icon: FileText },
]

const aiResultTitle = computed(() => {
  const labels: Record<AiMode, string> = {
    OUTLINE: '大纲候选',
    CONTINUE: '续写候选',
    POLISH: '润色候选',
    SUMMARY: '摘要候选',
    TITLE: '标题候选',
    TAGS: '标签候选',
    CODE: '代码候选',
    RESEARCH: '资料建议',
    CUSTOM: '自定义候选',
  }
  return labels[aiResult.mode] ?? 'AI 候选'
})

const canReplaceSelection = computed(() => aiSelectionRange.end > aiSelectionRange.start && Boolean(aiResult.text.trim()))

async function fetchHotTopics() {
  if (isLoadingTopics.value) return
  isLoadingTopics.value = true
  try {
    const topics = await requestJson<string[]>('/api/ai/hot-topics')
    hotTopics.value = (topics || []).slice(0, 3)
  } catch (error) {
    if (hotTopics.value.length === 0) {
      hotTopics.value = ['如何写出爆款文章', '技术进阶路线分享', '独立开发者的经验谈']
    }
    showNotice(readError(error, '灵感话题加载失败，已使用备用话题'))
  } finally {
    isLoadingTopics.value = false
  }
}

function insertTopic(topic: string) {
  articleForm.title = articleForm.title || topic
  insertMarkdown(`\n## ${topic}\n\n`)
}

function runQuickAction(mode: AiMode) {
  const prompt = aiPrompt.value.trim() || defaultPromptForMode(mode)
  generateAiText(mode, prompt)
}

function normalizeAiMode(value: unknown): AiMode {
  return typeof value === 'string' && AI_MODES.has(value as AiMode) ? value as AiMode : 'CUSTOM'
}

async function generateAiText(mode: AiMode = 'CUSTOM', promptOverride?: string) {
  const selectedMode = normalizeAiMode(mode)
  const prompt = (promptOverride ?? aiPrompt.value).trim()
  if (isGeneratingAiText.value) return
  if (selectedMode === 'CUSTOM' && !prompt) {
    showNotice('请输入 AI 创作要求')
    return
  }

  const selection = readEditorSelection()
  aiSelectionRange.start = selection.start
  aiSelectionRange.end = selection.end
  isGeneratingAiText.value = true
  activeAiMode.value = selectedMode
  aiResult.mode = selectedMode
  aiResult.text = ''
  aiResult.notice = ''

  try {
    if (selectedMode === 'CUSTOM') {
      await generateAiChatReply(prompt, selection.text)
      aiPrompt.value = ''
    } else {
      await generateQuickAiResult(selectedMode, prompt, selection.text)
    }
  } catch (error) {
    const message = readError(error, 'AI 生成失败')
    if (selectedMode === 'CUSTOM') {
      updateLastAssistantMessage({ notice: message, pending: false })
    } else {
      aiResult.notice = message
    }
    showNotice(message)
  } finally {
    isGeneratingAiText.value = false
    activeAiMode.value = null
  }
}

async function generateQuickAiResult(mode: AiMode, prompt: string, selection: string) {
  const response = await streamCreatorAiText(
    {
      mode,
      title: articleForm.title,
      prompt,
      context: articleForm.contentMarkdown,
      selection,
    },
    {
      onDelta: (delta) => {
        aiResult.text += delta
      },
      onDone: (result) => {
        aiResult.mode = result.mode
        aiResult.text = result.text
        aiResult.notice = result.notice ?? ''
      },
      onError: (message) => {
        aiResult.notice = message
      },
    }
  )
  aiResult.mode = response.mode
  aiResult.text = response.text
  aiResult.notice = response.notice ?? ''
}

async function generateAiChatReply(prompt: string, selection: string) {
  aiChatMessages.value.push({ id: ++aiMessageId, role: 'user', content: prompt })
  const assistantMessage: AiChatMessage = {
    id: ++aiMessageId,
    role: 'assistant',
    content: '',
    mode: 'CUSTOM',
    pending: true,
  }
  aiChatMessages.value.push(assistantMessage)
  scrollAiThreadToBottom()

  const response = await streamCreatorAiText(
    {
      mode: 'CUSTOM',
      title: articleForm.title,
      prompt,
      context: articleForm.contentMarkdown,
      selection,
    },
    {
      onDelta: (delta) => {
        assistantMessage.content += delta
        scrollAiThreadToBottom()
      },
      onDone: (result) => {
        assistantMessage.mode = result.mode
        assistantMessage.content = result.text
        assistantMessage.notice = result.notice ?? ''
        assistantMessage.pending = false
        scrollAiThreadToBottom()
      },
      onError: (message) => {
        assistantMessage.notice = message
        assistantMessage.pending = false
      },
    }
  )
  assistantMessage.mode = response.mode
  assistantMessage.content = response.text
  assistantMessage.notice = response.notice ?? ''
  assistantMessage.pending = false
  scrollAiThreadToBottom()
}

function updateLastAssistantMessage(patch: Partial<AiChatMessage>) {
  const message = [...aiChatMessages.value].reverse().find((item) => item.role === 'assistant')
  if (message) {
    Object.assign(message, patch)
  }
}

function resetAiChat() {
  if (isGeneratingAiText.value) return
  aiChatMessages.value = []
}

function scrollAiThreadToBottom() {
  nextTick(() => {
    const thread = aiThreadRef.value
    if (thread) {
      thread.scrollTop = thread.scrollHeight
    }
  })
}

function renderAiMarkdown(value: string) {
  return renderSafeMarkdown(value)
}

function defaultPromptForMode(mode: AiMode) {
  const title = articleForm.title.trim() || '当前草稿'
  const prompts: Record<AiMode, string> = {
    OUTLINE: `围绕《${title}》生成一份适合博客的结构化大纲`,
    CONTINUE: '根据当前草稿自然续写，保持上下文连贯',
    POLISH: '润色当前选中内容；如果没有选中内容，就润色当前草稿的关键段落',
    SUMMARY: '生成适合发布设置使用的文章摘要',
    TITLE: '基于当前草稿生成多个博客标题',
    TAGS: '基于当前草稿生成内容标签',
    CODE: '根据当前草稿生成一个相关的代码示例',
    RESEARCH: '整理这篇博客后续可以检索和核验的资料方向',
    CUSTOM: '',
  }
  return prompts[mode]
}

function readEditorSelection() {
  const textarea = textareaRef.value
  if (!textarea) {
    const end = articleForm.contentMarkdown.length
    return { start: end, end, text: '' }
  }
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  return {
    start,
    end,
    text: start === end ? '' : articleForm.contentMarkdown.slice(start, end),
  }
}

function insertAiResult() {
  if (!aiResult.text.trim()) return
  insertMarkdown(aiResult.text)
  showNotice('AI 内容已插入正文')
}

function replaceSelectionWithAiResult() {
  if (!canReplaceSelection.value) {
    showNotice('请先选中一段正文再替换')
    return
  }
  replaceMarkdownRange(aiSelectionRange.start, aiSelectionRange.end, aiResult.text.trim())
  showNotice('已用 AI 内容替换选区')
}

function fillSummaryFromAiResult() {
  if (!aiResult.text.trim()) return
  articleForm.summary = normalizePlainText(aiResult.text).slice(0, 180)
  showNotice('摘要已填入发布设置')
}

function useAiResultAsTitle() {
  const title = firstResultLine(aiResult.text)
  if (!title) return
  articleForm.title = title.slice(0, 100)
  showNotice('标题已更新')
}

async function copyAiResult() {
  if (!aiResult.text.trim()) return
  try {
    await navigator.clipboard.writeText(aiResult.text)
    showNotice('AI 内容已复制')
  } catch {
    showNotice('复制失败，请手动选中复制')
  }
}

function insertMarkdown(markdown: string) {
  const value = formatAiInsertion(markdown)
  const textarea = textareaRef.value
  const content = articleForm.contentMarkdown || ''
  if (!textarea) {
    articleForm.contentMarkdown = `${content.trim()}${value}`.trimStart()
    return
  }
  
  let position = textarea.selectionStart
  if (position === 0 && content.length > 0) {
    // If cursor is at the very beginning but there is text, likely lost focus or never focused. Append to end.
    position = content.length
  }
  replaceMarkdownRange(position, position, value)
}

function replaceMarkdownRange(start: number, end: number, value: string) {
  const content = articleForm.contentMarkdown || ''
  const safeStart = Math.max(0, Math.min(start, content.length))
  const safeEnd = Math.max(safeStart, Math.min(end, content.length))
  articleForm.contentMarkdown = content.slice(0, safeStart) + value + content.slice(safeEnd)
  setTimeout(() => {
    const textarea = textareaRef.value
    if (!textarea) return
    const cursor = safeStart + value.length
    textarea.focus()
    textarea.setSelectionRange(cursor, cursor)
  }, 0)
}

function formatAiInsertion(markdown: string) {
  const text = markdown.trim()
  if (!text) return ''
  const content = articleForm.contentMarkdown || ''
  return content.trim() ? `\n\n${text}\n` : text
}

function normalizePlainText(markdown: string) {
  return markdown
    .replace(/```[\s\S]*?```/g, '')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^[-*]\s+/gm, '')
    .replace(/^\d+[.、]\s+/gm, '')
    .replace(/[*_`>#]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function firstResultLine(markdown: string) {
  const line = markdown.split(/\r?\n/).map((item) => normalizePlainText(item)).find(Boolean)
  return line ?? ''
}

function blogOptionLabel<T extends string>(value: T, labels: Record<T, string>) {
  return labels[value] ?? value
}

async function loadEditorMeta() {
  const [categories, tags, theme] = await Promise.allSettled([
    fetchCategories('ARTICLE'),
    fetchTags(),
    fetchMyBlogTheme(),
  ])
  const failures: string[] = []

  if (categories.status === 'fulfilled') {
    articleCategoryOptions.value = categories.value
      .filter((category) => category.enabled !== false)
      .map((category) => ({ label: category.name, value: category.id }))
  } else {
    failures.push('分类')
  }
  if (tags.status === 'fulfilled') {
    tagOptions.value = tags.value
  } else {
    failures.push('标签')
  }
  if (theme.status === 'fulfilled') {
    blogTheme.value = normalizeBlogTheme(theme.value)
  } else {
    failures.push('博客外观')
  }
  if (failures.length > 0) {
    showNotice(`${failures.join('、')}加载失败，请刷新后再试`)
  }
}

// 初始加载
onMounted(async () => {
  fetchHotTopics()
  loadEditorMeta().catch((error) => showNotice(readError(error, '读取创作配置失败')))
  resetHistory(articleForm.contentMarkdown)
  renderedHtml.value = renderMarkdown(articleForm.contentMarkdown)
  // 如果 URL 带有 id 参数，则加载已有草稿进行编辑
  const id = Number(route.query.id)
  if (id) {
    await loadArticle(id)
  }
})

// 监听 Markdown 内容变化，生成 TOC
watch(() => articleForm.contentMarkdown, (newVal) => {
  updateWordCount()
  extractTOC(newVal)
  queueHistorySnapshot(newVal)
})

// 当可见性切换为指定好友/排除好友时，加载好友列表和现有规则
watch(() => articleForm.privacyType, async (privacy) => {
  if (privacy === 'SELECTED_FRIENDS' || privacy === 'EXCLUDED_FRIENDS') {
    await loadFriends()
    if (editingArticleId.value) {
      await loadVisibilityUsers(editingArticleId.value)
    }
  } else {
    selectedVisibilityUserIds.value = new Set()
    friendSearchQuery.value = ''
  }
})

let renderTimer: ReturnType<typeof setTimeout> | null = null
function updateWordCount() {
  wordCount.value = articleForm.contentMarkdown.trim().length
  if (renderTimer) clearTimeout(renderTimer)
  renderTimer = setTimeout(() => {
    renderedHtml.value = renderMarkdown(articleForm.contentMarkdown)
  }, 300)
}

function renderMarkdown(markdown: string) {
  headingRenderIndex = 0
  return md.render(markdown)
}

function extractTOC(markdown: string) {
  const lines = markdown.split('\n')
  const newToc = []
  let idCounter = 0
  for (const line of lines) {
    const match = line.match(/^(#{1,6})\s+(.+)/)
    if (match && match[1] && match[2]) {
      const level = match[1].length
      const text = match[2].trim()
      newToc.push({
        id: headingId(text, idCounter++),
        text,
        level
      })
    }
  }
  toc.value = newToc
}

function headingId(text: string, index: number) {
  const slug = text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}]+/gu, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 36)
  return `heading-${index}-${slug || 'section'}`
}

function scrollToHeading(id: string) {
  const target = previewRef.value?.querySelector(`[id="${id.replace(/"/g, '\\"')}"]`)
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function insertText(before: string, after: string, placeholder: string) {
  const textarea = textareaRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selected = articleForm.contentMarkdown.slice(start, end) || placeholder

  const newValue =
    articleForm.contentMarkdown.slice(0, start) +
    before + selected + after +
    articleForm.contentMarkdown.slice(end)

  articleForm.contentMarkdown = newValue

  // 恢复焦点并选中插入的文本
  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(start + before.length, start + before.length + selected.length)
  }, 0)
}

function insertImageMarkdown() {
  const url = window.prompt('图片 URL')
  if (!url) return
  insertText('![', `](${url.trim()})`, '图片描述')
}

function insertLinkMarkdown() {
  const url = window.prompt('链接 URL')
  if (!url) return
  insertText('[', `](${url.trim()})`, '链接文字')
}

function handleEditorKeydown(event: KeyboardEvent) {
  if (!(event.ctrlKey || event.metaKey)) return
  const key = event.key.toLowerCase()
  if (key === 's') {
    event.preventDefault()
    saveCurrentDraft()
  } else if (key === 'b') {
    event.preventDefault()
    insertText('**', '**', '粗体')
  } else if (key === 'i') {
    event.preventDefault()
    insertText('*', '*', '斜体')
  } else if (key === 'z' && !event.shiftKey) {
    event.preventDefault()
    undoEdit()
  } else if (key === 'y' || (key === 'z' && event.shiftKey)) {
    event.preventDefault()
    redoEdit()
  }
}

function queueHistorySnapshot(value: string) {
  if (isApplyingHistory) return
  if (historyTimer) clearTimeout(historyTimer)
  historyTimer = setTimeout(() => pushHistory(value), 350)
}

function pushHistory(value: string) {
  const last = historyStack.value[historyStack.value.length - 1]
  if (last === value) return
  historyStack.value.push(value)
  if (historyStack.value.length > 80) {
    historyStack.value.shift()
  }
  redoStack.value = []
}

function resetHistory(value: string) {
  historyStack.value = [value]
  redoStack.value = []
}

function undoEdit() {
  pushHistory(articleForm.contentMarkdown)
  if (!canUndo.value) return
  const current = historyStack.value.pop()
  if (current != null) redoStack.value.push(current)
  applyHistory(historyStack.value[historyStack.value.length - 1] ?? '')
}

function redoEdit() {
  const next = redoStack.value.pop()
  if (next == null) return
  historyStack.value.push(next)
  applyHistory(next)
}

function applyHistory(value: string) {
  isApplyingHistory = true
  articleForm.contentMarkdown = value
  renderedHtml.value = renderMarkdown(value)
  extractTOC(value)
  updateWordCount()
  requestAnimationFrame(() => {
    isApplyingHistory = false
  })
}

function syncScroll(e: Event) {
  const target = e.target as HTMLTextAreaElement
  const percentage = target.scrollTop / (target.scrollHeight - target.clientHeight)
  if (previewRef.value && previewRef.value.scrollHeight > previewRef.value.clientHeight) {
    previewRef.value.scrollTop = percentage * (previewRef.value.scrollHeight - previewRef.value.clientHeight)
  }
}

async function loadArticle(id: number) {
  try {
    const detail = await fetchCreatorArticle(id)
    editingArticleId.value = detail.id
    articleForm.title = detail.title
    articleForm.slug = detail.slug
    articleForm.summary = detail.summary ?? ''
    articleForm.contentMarkdown = detail.contentMarkdown ?? ''
    articleForm.coverUrl = detail.coverUrl ?? ''
    articleForm.categoryId = detail.category?.id ?? null
    articleForm.tagIds = detail.tags.map((t) => t.id)
    articleForm.privacyType = detail.privacyType
    renderedHtml.value = renderMarkdown(articleForm.contentMarkdown)
    extractTOC(articleForm.contentMarkdown)
    resetHistory(articleForm.contentMarkdown)
    if (detail.privacyType === 'SELECTED_FRIENDS' || detail.privacyType === 'EXCLUDED_FRIENDS') {
      await Promise.all([loadFriends(), loadVisibilityUsers(id)])
    }
  } catch (err) {
    showNotice(readError(err, '读取文章失败'))
  }
}

async function saveArticle(): Promise<boolean> {
  if (!articleForm.title.trim() || !articleForm.contentMarkdown.trim()) {
    showNotice('请填写标题和正文')
    return false
  }
  if (!articleForm.slug) {
    // 自动生成 slug
    articleForm.slug = slugFromTitle(articleForm.title, 'post')
  }

  try {
    if (editingArticleId.value) {
      await updateCreatorArticle(editingArticleId.value, { ...articleForm })
      showNotice('草稿已保存')
    } else {
      const res = await createCreatorArticle({ ...articleForm })
      editingArticleId.value = res.id // 保存后记录 ID
      // 更新 URL
      router.replace({ query: { id: res.id } })
      showNotice('草稿创建成功')
    }
    return true
  } catch (error) {
    showNotice(readError(error, '保存失败'))
    return false
  }
}

function slugFromTitle(title: string, prefix: string) {
  const normalized = title
    .trim()
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80)
  return normalized || `${prefix}-${Date.now()}`
}

function projectPayload(): ProjectPayload | null {
  if (!projectForm.title.trim() || !projectForm.projectType.trim()) {
    showNotice('请填写作品名称和作品类型')
    return null
  }
  if (!projectForm.slug) {
    projectForm.slug = slugFromTitle(projectForm.title, 'project')
  }
  const techStack = projectForm.techStack
    .split(/[，,]/)
    .map((item) => item.trim())
    .filter(Boolean)
  return {
    title: projectForm.title.trim(),
    slug: projectForm.slug,
    description: projectForm.description || projectForm.contentMarkdown.slice(0, 160),
    coverUrl: projectForm.coverUrl || null,
    projectType: projectForm.projectType,
    techStack,
    githubUrl: projectForm.githubUrl || null,
    demoUrl: projectForm.demoUrl || null,
    videoUrl: projectForm.videoUrl || null,
    contentMarkdown: projectForm.contentMarkdown || null,
    tagIds: [],
    recommended: false,
  }
}

async function saveProjectDraft(): Promise<boolean> {
  const payload = projectPayload()
  if (!payload) return false
  try {
    if (editingProjectId.value) {
      await updateCreatorProject(editingProjectId.value, payload)
      showNotice('作品草稿已保存')
    } else {
      const created = await createCreatorProject(payload)
      editingProjectId.value = created.id
      showNotice('作品草稿已创建')
    }
    return true
  } catch (error) {
    showNotice(readError(error, '保存作品失败'))
    return false
  }
}

async function publishProject() {
  const saved = await saveProjectDraft()
  if (!saved) return
  if (!editingProjectId.value) return
  try {
    await submitCreatorProject(editingProjectId.value)
    showNotice('作品已提交审核')
  } catch (error) {
    showNotice(readError(error, '发布作品失败'))
  }
}

function ideaPayload(isPublic: boolean): InspirationPayload | null {
  if (!ideaForm.title.trim()) {
    showNotice('请填写灵感标题')
    return null
  }
  if (ideaForm.cardType === 'IMAGE' && !ideaForm.imageUrl.trim()) {
    showNotice('请上传配图')
    return null
  }
  if (ideaForm.cardType === 'LINK' && !ideaForm.sourceUrl.trim()) {
    showNotice('请填写来源链接')
    return null
  }
  return {
    title: ideaForm.title.trim(),
    content: ideaForm.content.trim() || null,
    imageUrl: ideaForm.imageUrl || null,
    cardType: ideaForm.cardType,
    sourceUrl: ideaForm.sourceUrl || null,
    color: ideaForm.color || null,
    isPublic,
    sortOrder: 0,
    tagIds: [],
  }
}

async function saveIdeaDraft(): Promise<boolean> {
  const payload = ideaPayload(false)
  if (!payload) return false
  try {
    if (editingIdeaId.value) {
      await updateCreatorInspiration(editingIdeaId.value, payload)
      showNotice('灵感草稿已保存')
    } else {
      const created = await createCreatorInspiration(payload)
      editingIdeaId.value = created.id
      showNotice('灵感草稿已创建')
    }
    return true
  } catch (error) {
    showNotice(readError(error, '保存灵感失败'))
    return false
  }
}

async function publishIdea() {
  const payload = ideaPayload(true)
  if (!payload) return
  try {
    if (editingIdeaId.value) {
      await updateCreatorInspiration(editingIdeaId.value, payload)
      await publishCreatorInspiration(editingIdeaId.value)
    } else {
      const created = await createCreatorInspiration(payload)
      editingIdeaId.value = created.id
    }
    showNotice('灵感已发布')
  } catch (error) {
    showNotice(readError(error, '发布灵感失败'))
  }
}

async function publishArticle() {
  const saved = await saveArticle()
  if (!saved) return false
  if (!editingArticleId.value) return false

  try {
    if (showFriendSelector.value) {
      await syncVisibilityUsers(editingArticleId.value)
    }
    await submitCreatorArticle(editingArticleId.value)
    showNotice('文章已发布并提交审核！')
    setTimeout(() => {
      router.push(`/users/${session.currentUser?.id || ''}`)
    }, 1500)
    return true
  } catch (error) {
    showNotice(readError(error, '发布失败'))
    return false
  }
}

let noticeTimer: ReturnType<typeof setTimeout> | null = null
function showNotice(msg: string) {
  notice.value = msg
  if (noticeTimer) clearTimeout(noticeTimer)
  noticeTimer = setTimeout(() => { notice.value = '' }, 3000)
}

function readError(error: unknown, fallback: string) {
  return `${fallback}: ${toUserMessage(error, '请稍后再试')}`
}
</script>

<style scoped>
/* Reset & Base */
.csdn-editor-page {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100vw;
  height: calc(100vh - 73px); /* fits exactly below public-header */
  background-color: #fafafa;
  display: flex;
  flex-direction: column;
  z-index: 10;
  font-family: "Geist", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  overflow: hidden;
}

/* 顶部工具栏 (Premium Clean Toolbar) */
.editor-header {
  height: 52px;
  background: #ffffff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 1 360px;
  min-width: 0;
  min-height: 0;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
  color: #71717a;
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: color 0.2s;
}
.back-link:hover {
  color: #18181b;
}

.divider {
  width: 1px;
  height: 16px;
  background: rgba(0,0,0,0.08);
  flex: 0 0 auto;
}

.header-title {
  color: #18181b;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  flex: 0 0 auto;
  white-space: nowrap;
}

.header-theme-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 1 auto;
  max-width: 168px;
  min-height: 28px;
  padding: 5px 10px;
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 22%, transparent);
  border-radius: 999px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blog-paper, #ffffff) 92%, transparent), color-mix(in srgb, var(--blog-canvas, #f8fafc) 94%, transparent));
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
  font-size: 12px;
  font-weight: 750;
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(24, 24, 27, 0.04);
}

.header-theme-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  flex: 0 0 auto;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--blog-accent, #2563eb) 12%, transparent);
}

.header-theme-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-trigger {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.dropdown-icon {
  transition: transform 0.2s;
}
.dropdown-icon.rotated {
  transform: rotate(180deg);
}
.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  border: 1px solid rgba(0,0,0,0.06);
  min-width: 120px;
  z-index: 3000;
  overflow: hidden;
}
.dropdown-item {
  padding: 10px 16px;
  font-size: 13px;
  color: #3f3f46;
  cursor: pointer;
  transition: background 0.2s;
}
.dropdown-item:hover {
  background: #f4f4f5;
  color: #18181b;
}

.header-status {
  font-size: 11px;
  color: #71717a;
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.header-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1 1 auto;
  min-width: 0;
  justify-content: center;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: #52525b;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tool-btn span {
  display: none; /* Hide labels for a cleaner look */
}
.tool-btn:hover {
  background: #f4f4f5;
  color: #18181b;
}
.tool-btn.active {
  color: #6366f1;
  background: #eef2ff;
}
.tool-btn:disabled {
  color: #d4d4d8;
  cursor: not-allowed;
}
.tool-btn:disabled:hover {
  background: transparent;
  color: #d4d4d8;
}
.tool-btn:active {
  transform: scale(0.96);
}

.tool-divider {
  width: 1px;
  height: 16px;
  background: rgba(0,0,0,0.08);
  margin: 0 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 240px;
  justify-content: flex-end;
}

.word-count {
  font-size: 12px;
  color: #a1a1aa;
  font-variant-numeric: tabular-nums;
}

.btn-draft {
  background: transparent;
  border: 1px solid rgba(0,0,0,0.1);
  color: #52525b;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-draft:hover {
  background: #f4f4f5;
  color: #18181b;
}

.btn-publish {
  background: #18181b;
  border: none;
  color: #ffffff;
  padding: 7px 18px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.btn-publish:hover {
  background: #27272a;
}
.btn-publish:active {
  transform: translateY(1px);
}

/* 主体区域 */
.editor-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧目录 (Clean & Airy) */
.editor-sidebar-left {
  width: 260px;
  background: transparent;
  border-right: 1px solid rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px 24px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #a1a1aa;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.collapse-icon {
  color: #d4d4d8;
  cursor: pointer;
  transition: color 0.2s;
}
.collapse-icon:hover {
  color: #71717a;
}

.toc-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 24px;
}

.toc-empty {
  color: #a1a1aa;
  font-size: 13px;
  text-align: center;
  margin-top: 60px;
  padding: 0 20px;
  line-height: 1.6;
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.toc-list li {
  margin-bottom: 4px;
}
.toc-link {
  color: #52525b;
  text-decoration: none;
  font-size: 13px;
  display: block;
  padding: 6px 8px;
  border-radius: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.2s;
}
.toc-link:hover {
  background: rgba(0,0,0,0.04);
  color: #18181b;
}
.toc-level-1 .toc-link { font-size: 14px; font-weight: 600; }
.toc-level-2 .toc-link { font-size: 13px; font-weight: 500; }
.toc-level-3 .toc-link { font-size: 12px; }
.toc-level-4 .toc-link, .toc-level-5 .toc-link, .toc-level-6 .toc-link { font-size: 12px; color: #71717a; }

/* 中间编辑器 (Immersive) */
.editor-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  position: relative;
  min-height: 0;
}

.markdown-workspace {
  flex: 1;
  display: flex;
  padding: 32px 48px 48px;
  gap: 24px;
  min-height: 0;
}
.markdown-workspace--write,
.markdown-workspace--preview {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
}
.markdown-workspace--write .markdown-column,
.markdown-workspace--preview .markdown-column {
  flex-basis: 100%;
}

.markdown-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.preview-column {
  border-left: 1px solid rgba(0,0,0,0.06);
  padding-left: 24px;
}
.markdown-workspace--preview .preview-column {
  border-left: 0;
  padding-left: 0;
}

.title-input-wrapper {
  display: flex;
  align-items: center;
  padding-bottom: 16px;
  flex-shrink: 0;
}

.title-input {
  flex: 1;
  border: none;
  font-size: 36px;
  font-weight: 700;
  color: #18181b;
  outline: none;
  letter-spacing: 0;
}
.title-input::placeholder {
  color: #d4d4d8;
}

.title-counter {
  font-size: 12px;
  color: #a1a1aa;
  margin-left: 16px;
  opacity: 0;
  transition: opacity 0.3s;
  white-space: nowrap;
  flex-shrink: 0;
}
.title-input-wrapper:focus-within .title-counter {
  opacity: 1;
}

.markdown-input {
  flex: 1;
  border: none;
  resize: none;
  font-size: 15px;
  line-height: 1.8;
  color: #27272a;
  outline: none;
  font-family: "Geist Mono", Consolas, Monaco, monospace;
  overflow-y: auto;
}
.markdown-input::placeholder {
  color: #a1a1aa;
}
.markdown-input::-webkit-scrollbar {
  width: 8px;
}
.markdown-input::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.1);
  border-radius: 4px;
}

.preview-title-wrapper {
  padding-bottom: 16px;
  flex-shrink: 0;
}

.markdown-preview {
  flex: 1;
  overflow-y: auto;
  font-size: var(--blog-body-size, 15px);
  line-height: var(--blog-body-line, 1.8);
  color: var(--blog-body, #27272a);
}
.preview-title {
  font-size: 36px;
  font-weight: var(--blog-heading-weight, 700);
  color: var(--blog-title, #18181b);
  margin: 0;
  letter-spacing: 0;
}
.markdown-preview::-webkit-scrollbar {
  width: 7px;
}
.markdown-preview::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 24%, transparent);
  border-radius: 999px;
}
.markdown-preview :deep(h1), .markdown-preview :deep(h2), .markdown-preview :deep(h3) {
  margin-top: 1.2em;
  margin-bottom: 0.6em;
  color: var(--blog-title, #18181b);
  font-family: var(--blog-font-heading, inherit);
  font-weight: var(--blog-heading-weight, 760);
}
.markdown-preview :deep(> *:first-child) {
  margin-top: 0;
}
.markdown-preview :deep(p) {
  margin-bottom: 1em;
}
.markdown-preview :deep(pre) {
  background: var(--blog-code-bg, #f4f4f5);
  color: var(--blog-code-text, inherit);
  padding: 16px;
  border-radius: var(--blog-radius, 8px);
  overflow-x: auto;
}
.markdown-preview :deep(code) {
  font-family: var(--blog-font-mono, "Geist Mono", Consolas, monospace);
  background: var(--blog-code-bg, #f4f4f5);
  color: var(--blog-code-text, inherit);
  padding: 2px 4px;
  border-radius: 6px;
}
.markdown-preview :deep(blockquote) {
  border: 1px solid var(--blog-block-border, transparent);
  border-left: 4px solid var(--blog-accent, #e4e4e7);
  padding-left: 16px;
  color: var(--blog-muted, #71717a);
  background: var(--blog-block-bg, transparent);
  box-shadow: var(--blog-block-shadow, none);
  margin: 1em 0;
}
.markdown-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}
.markdown-preview :deep(table th), .markdown-preview :deep(table td) {
  border: 1px solid var(--blog-line, #e4e4e7);
  padding: 8px 12px;
  text-align: left;
}
.markdown-preview :deep(table th) {
  background: var(--blog-chip, #f4f4f5);
  font-weight: 600;
}
.markdown-preview :deep(img) {
  max-width: 100%;
  border-radius: var(--blog-radius, 8px);
}

.article-preview-surface {
  border-left-color: color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent);
  padding: 0 0 0 32px;
  border-radius: 0;
  background: #ffffff;
  color: var(--blog-body, #374151);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
  box-shadow: none;
}
.markdown-workspace--preview .article-preview-surface {
  padding-left: 0;
}
.article-preview-surface[data-blog-canvas='linen'] {
  background: #ffffff;
}
.article-preview-surface[data-blog-canvas='gradient'] {
  background: #ffffff;
}
.article-preview-surface[data-blog-canvas='image'] {
  background: #ffffff;
}
.article-preview-surface .preview-title {
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-heading, var(--blog-font, inherit));
  font-weight: var(--blog-heading-weight, 760);
}
.preview-summary {
  max-width: 62ch;
  margin: 10px 0 0;
  color: var(--blog-body, #374151);
  line-height: 1.7;
}
.article-preview-surface .markdown-preview {
  color: var(--blog-body, #374151);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
}
.article-preview-surface .markdown-preview :deep(h1),
.article-preview-surface .markdown-preview :deep(h2),
.article-preview-surface .markdown-preview :deep(h3) {
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-heading, inherit);
}
.article-preview-surface[data-blog-block='ink'] .markdown-preview :deep(blockquote) {
  border-left-color: var(--blog-accent, #2563eb);
}
.article-preview-surface[data-blog-block='carded'] .markdown-preview :deep(pre),
.article-preview-surface[data-blog-block='carded'] .markdown-preview :deep(blockquote) {
  border: 1px solid var(--blog-block-border, color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent));
  border-radius: var(--blog-radius, 8px);
}

/* 右侧 AI 助手 */
.editor-sidebar-right {
  width: 360px;
  background: #ffffff;
  border-left: 1px solid rgba(24, 24, 27, 0.08);
  display: flex;
  flex-direction: column;
}

.ai-assistant {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.ai-header {
  padding: 18px 20px 14px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  border-bottom: 1px solid rgba(24, 24, 27, 0.06);
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #18181b;
}

.ai-subtitle {
  margin-top: 4px;
  font-size: 11px;
  color: #71717a;
}

.ai-model {
  font-size: 11px;
  color: #71717a;
  background: #ffffff;
  border: 1px solid rgba(0,0,0,0.08);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.ai-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.ai-body--chat {
  overflow: hidden;
  gap: 12px;
  padding-bottom: 16px;
}

.ai-section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #52525b;
  margin-bottom: 10px;
}

.btn-refresh {
  background: none;
  border: none;
  color: #71717a;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.btn-refresh:hover:not(:disabled) {
  color: #18181b;
}
.btn-refresh:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ai-tag {
  max-width: 100%;
  background: #ffffff;
  border: 1px solid rgba(24, 24, 27, 0.08);
  padding: 8px 10px;
  border-radius: 7px;
  font-size: 12px;
  color: #3f3f46;
  cursor: pointer;
  transition: all 0.18s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  box-shadow: 0 1px 2px rgba(24, 24, 27, 0.03);
}
.ai-tag:hover {
  color: #0f172a;
  border-color: rgba(49, 91, 255, 0.28);
  transform: translateY(-1px);
}
.ai-loading {
  font-size: 12px;
  color: #a1a1aa;
  padding: 8px 0;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.spin {
  animation: spin 1s linear infinite;
}

.ai-quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.ai-action-btn {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 7px;
  min-width: 0;
  background: #ffffff;
  border: 1px solid rgba(24, 24, 27, 0.08);
  color: #3f3f46;
  padding: 10px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
  transition: all 0.18s ease;
}
.ai-action-btn span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ai-action-btn:hover:not(:disabled),
.ai-action-btn.active {
  color: #1436a8;
  border-color: rgba(49, 91, 255, 0.35);
  background: #f5f7ff;
}
.ai-action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.ai-result-panel {
  background: #ffffff;
  border: 1px solid rgba(24, 24, 27, 0.09);
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(24, 24, 27, 0.05);
  overflow: hidden;
}
.ai-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 13px;
  border-bottom: 1px solid rgba(24, 24, 27, 0.06);
  font-size: 12px;
  font-weight: 750;
  color: #18181b;
}
.ai-result-state {
  color: #315bff;
  font-size: 11px;
}
.ai-result-notice {
  margin: 10px 12px 0;
  color: #725900;
  background: #fff8db;
  border: 1px solid #f3df95;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 11px;
  line-height: 1.5;
}
.ai-result-text {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  padding: 12px 13px;
  font-size: 12px;
  line-height: 1.7;
  color: #27272a;
  background: #ffffff;
}
.ai-result-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid rgba(24, 24, 27, 0.06);
}
.ai-result-actions button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid rgba(24, 24, 27, 0.1);
  background: #f8f8f5;
  color: #3f3f46;
  border-radius: 6px;
  padding: 7px 9px;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
  transition: all 0.18s ease;
}
.ai-result-actions button:hover:not(:disabled) {
  background: #18181b;
  border-color: #18181b;
  color: #ffffff;
}
.ai-result-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.ai-chat-area {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 0 0 auto;
}

.ai-disclaimer {
  font-size: 11px;
  color: #71717a;
  line-height: 1.5;
}

.ai-chat-thread {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-chat-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #71717a;
  font-size: 11px;
  font-weight: 650;
}

.ai-chat-topline button {
  border: 1px solid rgba(24, 24, 27, 0.1);
  background: #ffffff;
  color: #3f3f46;
  border-radius: 999px;
  padding: 5px 9px;
  font-size: 11px;
  font-weight: 650;
  cursor: pointer;
}

.ai-chat-topline button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.ai-message {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.ai-message-label {
  color: #a1a1aa;
  font-size: 10px;
  font-weight: 750;
}

.ai-message--user {
  align-items: flex-end;
}

.ai-message--assistant {
  align-items: flex-start;
}

.ai-message-text,
.ai-message-content {
  max-width: 88%;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.7;
  word-break: break-word;
}

.ai-message-text {
  background: #18181b;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.ai-message-content {
  background: #ffffff;
  color: #27272a;
  border: 1px solid rgba(24, 24, 27, 0.08);
  border-bottom-left-radius: 4px;
  box-shadow: 0 6px 18px rgba(24, 24, 27, 0.04);
}

.ai-markdown {
  overflow-wrap: anywhere;
}

.ai-markdown :deep(p) {
  margin: 0 0 8px;
}

.ai-markdown :deep(p:last-child),
.ai-markdown :deep(ul:last-child),
.ai-markdown :deep(ol:last-child),
.ai-markdown :deep(pre:last-child),
.ai-markdown :deep(blockquote:last-child) {
  margin-bottom: 0;
}

.ai-markdown :deep(ul),
.ai-markdown :deep(ol) {
  margin: 0 0 8px;
  padding-left: 18px;
}

.ai-markdown :deep(li + li) {
  margin-top: 3px;
}

.ai-markdown :deep(code) {
  border-radius: 5px;
  background: #f4f4f5;
  padding: 1px 4px;
  font-size: 0.92em;
}

.ai-markdown :deep(pre) {
  margin: 8px 0;
  max-width: 100%;
  overflow-x: auto;
  border-radius: 8px;
  background: #111827;
  color: #f8fafc;
  padding: 10px;
  font-size: 11px;
  line-height: 1.6;
}

.ai-markdown :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.ai-markdown :deep(blockquote) {
  margin: 8px 0;
  padding-left: 10px;
  border-left: 3px solid #315bff;
  color: #52525b;
}

.ai-input-box {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 8px;
  padding: 7px 7px 7px 12px;
  border: 1px solid rgba(24, 24, 27, 0.1);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.ai-input-box:focus-within {
  border-color: rgba(49, 91, 255, 0.4);
  box-shadow: 0 0 0 3px rgba(49, 91, 255, 0.08);
}
.ai-input-box input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  padding: 0 8px;
  color: #18181b;
}
.ai-input-box input::placeholder {
  color: #a1a1aa;
}
.btn-send {
  background: #18181b;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
  flex: 0 0 auto;
}
.btn-send:hover:not(:disabled) {
  background: #315bff;
}
.btn-send:active:not(:disabled) {
  transform: scale(0.96);
}
.btn-send:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.publish-modal {
  background: white;
  width: 500px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
}
.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.modal-header h3 {
  margin: 0;
  font-size: 16px;
}
.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: #52525b;
}
.form-select, .form-input, .form-textarea {
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}
.form-select:focus, .form-input:focus, .form-textarea:focus {
  border-color: #18181b;
}
.tags-selector {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.tag-checkbox {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.cover-preview {
  margin-top: 8px;
  max-width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
}
.form-empty-hint {
  color: #a1a1aa;
  font-size: 13px;
}
.theme-inherit-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 18px auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent);
  border-radius: var(--blog-radius, 8px);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--blog-paper, #ffffff) 94%, transparent), color-mix(in srgb, var(--blog-canvas, #f8fafc) 86%, transparent)),
    #ffffff;
  font-family: var(--blog-font-body, var(--blog-font, inherit));
}
.theme-inherit-card strong,
.theme-inherit-card span {
  display: block;
  min-width: 0;
}
.theme-inherit-card strong {
  color: var(--blog-title, #111827);
  font-size: 13px;
}
.theme-inherit-card span {
  margin-top: 3px;
  color: var(--blog-body, #374151);
  font-size: 12px;
}
.theme-inherit-card i {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.55);
}
.theme-inherit-card a {
  color: var(--blog-accent, #2563eb);
  font-size: 12px;
  font-weight: 760;
  text-decoration: none;
  white-space: nowrap;
}
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(0,0,0,0.06);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.btn-cancel {
  background: white;
  border: 1px solid rgba(0,0,0,0.1);
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
}
.btn-publish-confirm {
  background: #18181b;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
}

/* 全局提示 */
.global-toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  background: #18181b;
  color: #fff;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  z-index: 2000;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  letter-spacing: 0.01em;
}

/* 额外表单样式 */
.form-layout, .idea-layout {
  padding: 0;
  align-items: stretch;
  overflow: hidden;
}
.project-form, .idea-form {
  width: 100%;
  max-width: 680px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.form-page-title {
  font-size: 24px;
  font-weight: 700;
  color: #18181b;
  margin-bottom: 16px;
  text-align: center;
}
.form-group-large {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.form-group-large label {
  font-size: 14px;
  font-weight: 600;
  color: #18181b;
}
.form-input-large, .form-textarea-large {
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 15px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
  background: #fafafa;
}
.form-input-large:focus, .form-textarea-large:focus {
  border-color: #18181b;
  background: #fff;
}
.form-textarea-large {
  resize: vertical;
}

.idea-input-box {
  background: #fff;
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.02);
}
.idea-textarea {
  width: 100%;
  border: none;
  padding: 20px;
  font-size: 15px;
  outline: none;
  font-family: inherit;
  resize: none;
}
.idea-actions {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-top: 1px solid rgba(0,0,0,0.06);
  background: #fafafa;
}/* Additional styles for Project and Idea layouts */
.scroll-container {
  overflow-y: auto;
  height: 100%;
  max-height: 100%;
  padding: 64px 40px 96px;
  width: 100%;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(17, 24, 39, 0.18) transparent;
}
.scroll-container::-webkit-scrollbar {
  width: 8px;
}
.scroll-container::-webkit-scrollbar-track {
  background: transparent;
}
.scroll-container::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.16);
  background-clip: padding-box;
}
.scroll-container::-webkit-scrollbar-thumb:hover {
  background: rgba(17, 24, 39, 0.28);
  background-clip: padding-box;
}

.form-layout, .idea-layout {
  display: flex;
  justify-content: center;
  background-color: #fafafa;
}

.project-form.scroll-container, .idea-form.scroll-container {
  width: 100%;
  max-width: none;
  margin: 0 auto;
  align-items: center;
}

.project-form.scroll-container > *, .idea-form.scroll-container > * {
  width: min(800px, 100%);
}

.form-page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 32px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.form-group-large label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}
.form-group-large .required {
  color: #ef4444;
}

.form-select-large,
.form-input-large,
.form-textarea-large {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  transition: border-color 0.2s;
  background-color: #fff;
}

.form-select-large:focus,
.form-input-large:focus,
.form-textarea-large:focus {
  outline: none;
  border-color: #6366f1;
}

.idea-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.idea-tab {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  color: #4b5563;
  transition: all 0.2s;
}

.idea-tab.active, .idea-tab:hover {
  background: #6366f1;
  color: #fff;
  border-color: #6366f1;
}

.color-picker-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-input {
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.color-value {
  font-size: 14px;
  color: #6b7280;
}

.btn-clear-color {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  font-size: 14px;
}

.selected-count {
  font-size: 12px;
  color: #71717a;
  font-weight: 400;
  margin-left: 8px;
}

.friend-selector {
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 8px;
  overflow: hidden;
}

.friend-search {
  padding: 8px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
}

.friend-search-input {
  width: 100%;
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.friend-search-input:focus {
  border-color: #18181b;
}

.friend-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 4px;
}

.friend-loading,
.friend-empty {
  padding: 24px 16px;
  text-align: center;
  color: #a1a1aa;
  font-size: 13px;
}

.friend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.friend-item:hover {
  background: #f4f4f5;
}

.friend-item.is-selected {
  background: #f0f4ff;
}

.friend-checkbox {
  width: 16px;
  height: 16px;
  accent-color: #315bff;
  cursor: pointer;
  flex-shrink: 0;
}

.friend-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #f4f4f5;
}

.friend-name {
  font-size: 13px;
  color: #3f3f46;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

</style>
