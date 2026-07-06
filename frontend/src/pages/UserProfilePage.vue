<template>
  <section
    ref="root"
    class="profile-page"
    :style="profileThemeStyle"
    :data-blog-canvas="activeBlogTheme.canvasType"
    :data-blog-layout="activeBlogTheme.layoutStyle"
    :data-blog-block="activeBlogTheme.blockStyle"
  >
    <div v-if="isLoading" class="empty-state profile-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在加载用户信息</h2>
    </div>

    <template v-else-if="profile">
      <div class="profile-layout" data-reveal>
        
        <!-- Left Sidebar: Identity -->
        <aside class="profile-sidebar">
          <div class="profile-identity">
            <div class="profile-avatar-wrapper">
              <img
                v-if="profile.avatarUrl"
                :src="profile.avatarUrl"
                alt=""
                class="profile-avatar-img"
                loading="lazy"
              />
              <UserRound v-else :size="48" class="profile-avatar-placeholder" />
            </div>

            <template v-if="editing">
              <div class="edit-form">
                <div class="avatar-edit-wrapper" @click="triggerAvatarUpload">
                  <img v-if="editForm.avatarUrl" :src="editForm.avatarUrl" alt="" class="avatar-edit-img" />
                  <UserRound v-else :size="48" class="avatar-edit-placeholder" />
                  <div class="avatar-edit-overlay">
                    <LoaderCircle v-if="avatarUploading" class="spin" :size="24" />
                    <Camera v-else :size="24" />
                  </div>
                  <input type="file" ref="avatarInput" class="hidden-input" accept="image/*" @change="handleAvatarSelect" />
                </div>
                <input v-model="editForm.nickname" class="edit-input edit-input--title" placeholder="昵称" />
                <textarea v-model="editForm.bio" class="edit-input edit-input--textarea" placeholder="个人简介" rows="3" />
                <div class="setting-group">
                  <label class="setting-label">私信权限设置</label>
                  <select v-model="editForm.privateMessageSetting" class="edit-input edit-select">
                    <option value="ALL">所有人都可以私信我</option>
                    <option value="FOLLOW">关注我才可私信我（只能发送一条）</option>
                    <option value="MUTUAL">仅互相关注可私信</option>
                    <option value="NONE">关闭私信功能</option>
                  </select>
                </div>
                <div class="password-section">
                  <input v-model="passwordForm.oldPassword" type="password" class="edit-input" placeholder="原密码 (留空不改)" />
                  <input v-model="passwordForm.newPassword" type="password" class="edit-input" placeholder="新密码" />
                </div>
                <div class="edit-actions">
                  <button class="btn-solid" type="button" :disabled="saving" @click="saveProfile">
                    {{ saving ? '保存中...' : '保存' }}
                  </button>
                  <button class="btn-outline" type="button" :disabled="saving" @click="cancelEditing">取消</button>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="profile-titles">
                <h1 class="profile-name">{{ profile.nickname || profile.username }}</h1>
              </div>
              
              <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
              
              <div class="profile-raw-stats">
                <div class="stat-item">
                  <strong>{{ profile.articleCount }}</strong>
                  <span>文章</span>
                </div>
                <div class="stat-item">
                  <strong>{{ profile.followerCount }}</strong>
                  <span>粉丝</span>
                </div>
                <div class="stat-item">
                  <strong>{{ profile.followingCount }}</strong>
                  <span>关注</span>
                </div>
              </div>

              <div class="profile-actions">
                <button v-if="isOwnProfile" class="btn-outline w-full" type="button" @click="startEditing">
                  编辑资料
                </button>
                <button
                  v-else-if="canFollow"
                  class="w-full"
                  :class="following ? 'btn-outline' : 'btn-solid'"
                  type="button"
                  @click="toggleFollow(profile.id)"
                >
                  {{ isFriend ? '互相关注' : following ? '已关注' : '关注' }}
                </button>
                <RouterLink
                  v-if="!isOwnProfile && session.isAuthenticated"
                  class="btn-outline w-full profile-message-link"
                  :to="{ name: 'my-messages', query: { to: profile.id } }"
                >
                  <MessageCircle :size="16" />
                  私信
                </RouterLink>
              </div>
            </template>
          </div>
        </aside>

        <!-- Right Content: Navigation & Data -->
        <main class="profile-main">
          <nav class="profile-nav">
            <button :class="{ 'is-active': activeTab === 'articles' }" @click="activeTab = 'articles'">文章</button>
            <button :class="{ 'is-active': activeTab === 'friends' }" @click="activeTab = 'friends'">好友 <span>{{ profile.friendCount }}</span></button>
            <button :class="{ 'is-active': activeTab === 'following' }" @click="activeTab = 'following'">关注 <span>{{ profile.followingCount }}</span></button>
            <button :class="{ 'is-active': activeTab === 'followers' }" @click="activeTab = 'followers'">粉丝 <span>{{ profile.followerCount }}</span></button>
            <template v-if="isOwnProfile">
              <button :class="{ 'is-active': activeTab === 'favorites' }" @click="activeTab = 'favorites'">收藏 <span>{{ favoriteRecords.length }}</span></button>
              <button :class="{ 'is-active': activeTab === 'likes' }" @click="activeTab = 'likes'">喜欢 <span>{{ likeRecords.length }}</span></button>
              <button :class="{ 'is-active': activeTab === 'appearance' }" @click="activeTab = 'appearance'">外观</button>
            </template>
          </nav>

          <div class="profile-content-area">
            
            <!-- 文章列表 -->
            <div v-if="activeTab === 'articles'" class="content-fade-in">
              <div v-if="articlesLoading" class="empty-state">
                <LoaderCircle class="spin" :size="20" />
              </div>
              <div v-else-if="articles.length === 0" class="empty-state">
                <h3>还没有公开文章</h3>
              </div>
              <div v-else class="journal-grid">
                <RouterLink
                  v-for="(article, index) in articles"
                  :key="article.id"
                  class="journal-card"
                  :to="{ name: 'article-detail', params: { slug: article.slug } }"
                  :style="coverStyle(index)"
                >
                  <div class="journal-card__visual" aria-hidden="true">
                    <img v-if="article.coverUrl" :src="article.coverUrl" alt="" loading="lazy" />
                    <span v-else>{{ (article.title || '文章').slice(0, 2) }}</span>
                  </div>
                  <div class="journal-card__content">
                    <div class="article-meta-row">
                      <span v-if="article.privacyType !== 'PUBLIC'" class="privacy-badge">{{ privacyLabel(article.privacyType) }}</span>
                      <span class="article-date">{{ formatDate(article.publishTime) }}</span>
                    </div>
                    <h2>{{ article.title }}</h2>
                    <p class="article-summary">{{ article.summary }}</p>
                    <div class="tag-row" v-if="article.tags?.length">
                      <span v-for="tag in article.tags.slice(0, 3)" :key="tag.id">#{{ tag.name }}</span>
                    </div>
                  </div>
                </RouterLink>
              </div>
            </div>

            <div v-if="activeTab === 'appearance' && isOwnProfile" class="content-fade-in appearance-studio">
              <section class="appearance-library">
                <div class="appearance-heading">
                  <p class="appearance-eyebrow">Blog appearance</p>
                  <h2>选择一套完整的博客气质</h2>
                  <span>主题会同时影响个人主页、文章详情和发布预览。</span>
                </div>
                <div class="appearance-presets">
                  <button
                    v-for="preset in BLOG_THEME_PRESETS"
                    :key="preset.displayName"
                    type="button"
                    class="theme-preset"
                    :class="{ 'is-active': isPresetActive(preset) }"
                    :style="blogThemeToStyle(preset)"
                    @click="applyThemePreset(preset)"
                  >
                    <span class="theme-preset__swatch">
                      <i></i>
                      <b></b>
                    </span>
                    <span class="theme-preset__body">
                      <strong>{{ preset.displayName }}</strong>
                      <small>{{ presetMeta(preset).description }}</small>
                    </span>
                    <em>{{ presetMeta(preset).rhythm }}</em>
                  </button>
                </div>
              </section>

              <section class="appearance-controls">
                <div class="appearance-heading appearance-heading--compact">
                  <p class="appearance-eyebrow">Fine tune</p>
                  <h2>少量高影响调节</h2>
                  <span>保留主题整体审美，只微调识别色和内容气质。</span>
                </div>
                <div class="theme-form-grid">
                  <label>
                    主题名称
                    <input v-model="blogThemeForm.displayName" class="edit-input" maxlength="80" />
                  </label>
                  <label>
                    字体
                    <BaseSelect v-model="blogThemeForm.fontPreset" :options="BLOG_FONT_OPTIONS" />
                  </label>
                  <label>
                    画布
                    <BaseSelect v-model="blogThemeForm.canvasType" :options="BLOG_CANVAS_OPTIONS" />
                  </label>
                  <label>
                    版式
                    <BaseSelect v-model="blogThemeForm.layoutStyle" :options="BLOG_LAYOUT_OPTIONS" />
                  </label>
                  <label>
                    文章块
                    <BaseSelect v-model="blogThemeForm.blockStyle" :options="BLOG_BLOCK_OPTIONS" />
                  </label>
                  <label class="color-field">
                    强调色
                    <input v-model="blogThemeForm.accentColor" type="color" />
                  </label>
                  <label class="color-field">
                    标题色
                    <input v-model="blogThemeForm.titleColor" type="color" />
                  </label>
                  <label class="color-field">
                    正文色
                    <input v-model="blogThemeForm.bodyColor" type="color" />
                  </label>
                  <label class="color-field">
                    画布色
                    <input v-model="blogThemeForm.canvasColor" type="color" />
                  </label>
                  <label class="color-field">
                    纸张色
                    <input v-model="blogThemeForm.paperColor" type="color" />
                  </label>
                </div>

                <label v-if="blogThemeForm.canvasType === 'image'" class="canvas-upload">
                  画布图片
                  <FileUpload v-model="blogThemeForm.canvasImage" module="OTHER" accept="image/*" hint="最大 10MB" @error="handleThemeUploadError" />
                </label>

                <div class="edit-actions appearance-actions">
                  <button class="btn-solid" type="button" :disabled="themeSaving" @click="saveBlogTheme">
                    {{ themeSaving ? '保存中...' : '保存外观' }}
                  </button>
                  <button class="btn-outline" type="button" :disabled="themeSaving" @click="resetBlogThemeForm">还原</button>
                </div>
                <p v-if="themeNotice" class="inline-notice">{{ themeNotice }}</p>
              </section>

              <section
                class="appearance-preview"
                :style="previewThemeStyle"
                :data-blog-canvas="blogThemeForm.canvasType"
                :data-blog-layout="blogThemeForm.layoutStyle"
                :data-blog-block="blogThemeForm.blockStyle"
              >
                <div class="preview-browser-bar">
                  <span></span>
                  <span></span>
                  <span></span>
                  <strong>{{ blogThemeForm.displayName }}</strong>
                </div>
                <div class="preview-author-card">
                  <div class="preview-avatar">{{ (profile.nickname || profile.username || 'C').slice(0, 1) }}</div>
                  <div>
                    <p class="preview-kicker">{{ presetMeta(blogThemeForm).bestFor }}</p>
                    <h2>{{ profile.nickname || profile.username }} 的主题博客</h2>
                  </div>
                </div>
                <article class="preview-article-card">
                  <div class="preview-cover"></div>
                  <div class="preview-article-body">
                    <p class="preview-kicker">Featured essay</p>
                    <h3>把一段想法整理成可以被记住的文章</h3>
                    <p>第一屏先传达作者气质，再让卡片、标题、引用和代码块保持同一套节奏。</p>
                  </div>
                </article>
                <div class="preview-markdown-sample">
                  <h3>主题正文示例</h3>
                  <p>这里展示的是文章详情页会继承的字体、行距、纸张色和阅读宽度。</p>
                  <blockquote>好的主题不是装饰，而是内容的语气。</blockquote>
                  <pre><code>const theme = 'consistent tokens'</code></pre>
                </div>
                <div class="preview-tags">
                  <span>主页</span>
                  <span>文章页</span>
                  <span>发布预览</span>
                </div>
              </section>
            </div>

            <!-- 收藏列表（仅自己可见） -->
            <div v-if="activeTab === 'favorites'" class="content-fade-in">
              <div v-if="favoritesLoading" class="empty-state">
                <LoaderCircle class="spin" :size="20" />
              </div>
              <div v-else-if="favoriteRecords.length === 0" class="empty-state">
                <h3>还没有收藏</h3>
              </div>
              <div v-else class="journal-grid">
                <RouterLink
                  v-for="(fav, index) in favoriteRecords"
                  :key="fav.id"
                  :to="favoriteRoute(fav)"
                  class="journal-card"
                  :style="coverStyle(index)"
                >
                  <div class="journal-card__visual" aria-hidden="true">
                    <img v-if="fav.coverUrl" :src="fav.coverUrl" alt="" loading="lazy" />
                    <span v-else>{{ coverFallback(fav) }}</span>
                  </div>
                  <div class="journal-card__content">
                    <div class="article-meta-row">
                      <span class="favorite-type-badge">{{ fav.targetType === 'ARTICLE' ? '文章' : '作品' }}</span>
                      <span class="article-date">{{ formatDate(fav.createdAt) }}</span>
                    </div>
                    <h2>{{ fav.title || '未命名' }}</h2>
                  </div>
                </RouterLink>
              </div>
            </div>

            <!-- 喜欢列表（仅自己可见） -->
            <div v-if="activeTab === 'likes'" class="content-fade-in">
              <div v-if="likesLoading" class="empty-state">
                <LoaderCircle class="spin" :size="20" />
              </div>
              <div v-else-if="likeRecords.length === 0" class="empty-state">
                <h3>还没有喜欢</h3>
              </div>
              <div v-else class="journal-grid">
                <RouterLink
                  v-for="(like, index) in likeRecords"
                  :key="like.id"
                  :to="likeRoute(like)"
                  class="journal-card"
                  :style="coverStyle(index)"
                >
                  <div class="journal-card__visual" aria-hidden="true">
                    <img v-if="like.coverUrl" :src="like.coverUrl" alt="" loading="lazy" />
                    <span v-else>{{ (like.title || '喜欢').slice(0, 2) }}</span>
                  </div>
                  <div class="journal-card__content">
                    <div class="article-meta-row">
                      <span class="favorite-type-badge">{{ like.targetType === 'ARTICLE' ? '文章' : like.targetType }}</span>
                      <span class="article-date">{{ formatDate(like.createdAt) }}</span>
                    </div>
                    <h2>{{ like.title || `${like.targetType} #${like.targetId}` }}</h2>
                  </div>
                </RouterLink>
              </div>
            </div>

            <!-- 好友 / 关注 / 粉丝列表 -->
            <div v-if="['friends', 'following', 'followers'].includes(activeTab)" class="content-fade-in">
              <div v-if="relationLoading" class="empty-state">
                <LoaderCircle class="spin" :size="20" />
              </div>
              <div v-else-if="relationList.length === 0" class="empty-state">
                <h3>{{ emptyRelationText }}</h3>
              </div>
              <div v-else class="profile-user-list">
                <RouterLink
                  v-for="user in relationList"
                  :key="user.id"
                  class="profile-user-card"
                  :to="{ name: 'user-profile', params: { userId: user.id } }"
                >
                  <div class="profile-user-card__avatar">
                    <img
                      v-if="user.avatarUrl"
                      :src="user.avatarUrl"
                      alt=""
                      loading="lazy"
                    />
                    <UserRound v-else :size="20" />
                  </div>
                  <div class="profile-user-card__info">
                    <strong>{{ user.nickname || user.username }}</strong>
                    <span>@{{ user.username }}</span>
                  </div>
                </RouterLink>
              </div>
            </div>
          </div>
        </main>
      </div>
    </template>
    
    <div v-else class="empty-state profile-state" data-reveal>
      <h2>用户不存在</h2>
      <p>{{ notice }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { LoaderCircle, UserRound, Camera, MessageCircle } from '@lucide/vue'
import { uploadFile } from '../services/file'
import FileUpload from '../components/common/FileUpload.vue'
import BaseSelect from '../shared/components/BaseSelect.vue'
import {
  fetchUserProfile,
  fetchUserArticles,
  fetchUserFollowers,
  fetchUserFollowing,
  fetchUserFriends,
  fetchMyFavorites,
  fetchMyLikes,
  updateMyProfile,
  updateMyPassword,
  fetchMyBlogTheme,
  updateMyBlogTheme,
} from '../services/content'
import { toUserMessage } from '../services/http'
import {
  BLOG_BLOCK_OPTIONS,
  BLOG_CANVAS_OPTIONS,
  BLOG_FONT_OPTIONS,
  BLOG_LAYOUT_OPTIONS,
  BLOG_THEME_PRESET_META,
  BLOG_THEME_PRESETS,
  DEFAULT_BLOG_THEME,
  blogThemeToStyle,
  normalizeBlogTheme,
} from '../shared/blogTheme'
import { useFollow } from '../shared/composables/useFollow'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { formatDateToDay } from '../shared/datetime'
import { useSessionStore } from '../shared/sessionStore'
import type { ArticleSummary, BlogThemeConfig, BlogThemePayload, FavoriteRecord, FollowUser, InteractionRecord, UserProfile } from '../shared/domain'

const route = useRoute()
const root = ref<HTMLElement | null>(null)
const session = useSessionStore()

const profile = ref<UserProfile | null>(null)
const articles = ref<ArticleSummary[]>([])
const relationList = ref<FollowUser[]>([])
const isLoading = ref(true)
const articlesLoading = ref(false)
const relationLoading = ref(false)
const favoritesLoading = ref(false)
const likesLoading = ref(false)
const favoriteRecords = ref<FavoriteRecord[]>([])
const likeRecords = ref<InteractionRecord[]>([])
const notice = ref('')
type ProfileTab = 'articles' | 'friends' | 'following' | 'followers' | 'favorites' | 'likes' | 'appearance'
const profileTabs: ProfileTab[] = ['articles', 'friends', 'following', 'followers', 'favorites', 'likes', 'appearance']
const ownerOnlyTabs: ProfileTab[] = ['favorites', 'likes', 'appearance']
const activeTab = ref<ProfileTab>('articles')

const { following, isFriend, loadStatus: loadFollowStatus, toggleFollow } = useFollow()
const userId = computed(() => Number(route.params.userId))

const canFollow = computed(() => {
  if (!session.accessToken || !profile.value) return false
  return session.currentUser?.id !== profile.value.id
})

const isOwnProfile = computed(() => {
  return Boolean(session.currentUser && profile.value && session.currentUser.id === profile.value.id)
})

function routeTab(): ProfileTab {
  const tab = route.query.tab
  return typeof tab === 'string' && profileTabs.includes(tab as ProfileTab) ? (tab as ProfileTab) : 'articles'
}

function syncTabFromRoute(ownProfile = isOwnProfile.value) {
  const nextTab = routeTab()
  activeTab.value = !ownProfile && ownerOnlyTabs.includes(nextTab) ? 'articles' : nextTab
}

const editing = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const editForm = ref({ nickname: '', avatarUrl: '', bio: '', privateMessageSetting: 'ALL' })
const passwordForm = ref({ oldPassword: '', newPassword: '' })
const themeSaving = ref(false)
const themeNotice = ref('')
const blogThemeForm = ref<BlogThemePayload>({ ...DEFAULT_BLOG_THEME })

const activeBlogTheme = computed(() => normalizeBlogTheme(profile.value?.blogTheme))
const profileThemeStyle = computed(() => blogThemeToStyle(activeBlogTheme.value))
const previewThemeStyle = computed(() => blogThemeToStyle(blogThemeForm.value))

function startEditing() {
  if (!profile.value) return
  editForm.value = {
    nickname: profile.value.nickname ?? '',
    avatarUrl: profile.value.avatarUrl ?? '',
    bio: profile.value.bio ?? '',
    privateMessageSetting: profile.value.privateMessageSetting ?? 'ALL',
  }
  passwordForm.value = { oldPassword: '', newPassword: '' }
  editing.value = true
}

function cancelEditing() {
  editing.value = false
}

function triggerAvatarUpload() {
  avatarInput.value?.click()
}

async function handleAvatarSelect(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return
  
  const file = target.files[0]
  if (!file) return
  
  avatarUploading.value = true
  try {
    const res = await uploadFile(file, 'AVATAR', session.isAdmin)
    editForm.value.avatarUrl = res.publicUrl
  } catch (error) {
    alert(toUserMessage(error, '头像上传失败'))
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}

async function saveProfile() {
  if (!profile.value) return
  saving.value = true
  try {
    const updated = await updateMyProfile({
      nickname: editForm.value.nickname || null,
      avatarUrl: editForm.value.avatarUrl || null,
      bio: editForm.value.bio || null,
      privateMessageSetting: editForm.value.privateMessageSetting || 'ALL',
    })
    
    if (passwordForm.value.oldPassword && passwordForm.value.newPassword) {
      await updateMyPassword({
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword,
      })
      alert('密码修改成功，请使用新密码重新登录')
    }
    
    profile.value = updated
    editing.value = false
  } catch (e) {
    notice.value = toUserMessage(e, '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

function setBlogThemeForm(theme?: BlogThemeConfig | null) {
  const normalized = normalizeBlogTheme(theme)
  blogThemeForm.value = { ...normalized }
}

function resetBlogThemeForm() {
  setBlogThemeForm(profile.value?.blogTheme)
  themeNotice.value = ''
}

function applyThemePreset(preset: BlogThemeConfig) {
  setBlogThemeForm({
    ...preset,
    displayName: preset.displayName,
  })
}

function presetMeta(theme: Pick<BlogThemeConfig, 'displayName'>) {
  return BLOG_THEME_PRESET_META[theme.displayName] ?? {
    description: '自定义主题, 会继承当前选择的字体、画布和区块规则。',
    bestFor: '自定义博客',
    rhythm: '自由组合',
  }
}

function isPresetActive(preset: BlogThemeConfig) {
  return blogThemeForm.value.displayName === preset.displayName
}

function handleThemeUploadError(message: string) {
  themeNotice.value = message
}

async function saveBlogTheme() {
  if (!profile.value) return
  if (blogThemeForm.value.canvasType === 'image' && !blogThemeForm.value.canvasImage) {
    themeNotice.value = '请先上传画布图片'
    return
  }
  themeSaving.value = true
  themeNotice.value = ''
  try {
    const updated = await updateMyBlogTheme({
      ...blogThemeForm.value,
      canvasImage: blogThemeForm.value.canvasType === 'image' ? blogThemeForm.value.canvasImage : null,
    })
    profile.value = {
      ...profile.value,
      blogTheme: updated,
    }
    setBlogThemeForm(updated)
    themeNotice.value = '外观已保存'
  } catch (error) {
    themeNotice.value = toUserMessage(error, '外观保存失败')
  } finally {
    themeSaving.value = false
  }
}

const emptyRelationText = computed(() => {
  switch (activeTab.value) {
    case 'friends': return '还没有好友'
    case 'following': return '还没有关注'
    case 'followers': return '还没有粉丝'
    case 'favorites': return '还没有收藏'
    case 'likes': return '还没有喜欢'
    default: return ''
  }
})

usePageReveal(root)

const coverPalettes = [
  ['#18181b', '#3b82f6', '#f8fafc'],
  ['#18181b', '#10b981', '#f8fafc'],
  ['#18181b', '#f59e0b', '#f8fafc'],
  ['#18181b', '#8b5cf6', '#f8fafc'],
  ['#18181b', '#ef4444', '#f8fafc'],
]

function favoriteRoute(item: FavoriteRecord) {
  if (item.targetType === 'ARTICLE') {
    return { name: 'article-detail', params: { slug: item.slug } }
  }
  return { name: 'project-detail', params: { slug: item.slug } }
}

function coverStyle(index: number) {
  const palette = coverPalettes[index % coverPalettes.length] ?? coverPalettes[0]
  return {
    '--cover-from': palette[0],
    '--cover-accent': palette[1],
    '--cover-ink': palette[2],
  }
}

function coverFallback(item: FavoriteRecord): string {
  return (item.title || '收藏').slice(0, 2)
}

function privacyLabel(value: string) {
  const labels: Record<string, string> = {
    PUBLIC: '公开',
    SELF: '仅自己',
    FRIENDS: '仅好友可见',
    SELECTED_FRIENDS: '指定好友',
    EXCLUDED_FRIENDS: '排除好友',
  }
  return labels[value] || value
}

function likeRoute(like: InteractionRecord) {
  if (like.targetType === 'ARTICLE' && like.slug) {
    return { name: 'article-detail', params: { slug: like.slug } }
  }
  return ''
}

async function loadProfile() {
  if (!userId.value || !Number.isFinite(userId.value)) {
    profile.value = null
    notice.value = '用户标识无效'
    isLoading.value = false
    return
  }
  isLoading.value = true
  notice.value = ''
  try {
    const [data] = await Promise.all([
      fetchUserProfile(userId.value),
      loadFollowStatus(userId.value),
    ])
    const ownProfile = session.currentUser?.id === data.id
    let blogTheme = data.blogTheme
    if (ownProfile) {
      try {
        blogTheme = await fetchMyBlogTheme()
      } catch {
        blogTheme = data.blogTheme
      }
    }
    profile.value = {
      ...data,
      blogTheme,
    }
    syncTabFromRoute(ownProfile)
    setBlogThemeForm(blogTheme)
    if (ownProfile) {
      await Promise.all([loadArticles(), loadFavorites(), loadLikes()])
    } else {
      favoriteRecords.value = []
      likeRecords.value = []
      await loadArticles()
    }
  } catch (error) {
    profile.value = null
    notice.value = toUserMessage(error, '用户不存在')
  } finally {
    isLoading.value = false
  }
}

async function loadArticles() {
  if (!profile.value) return
  articlesLoading.value = true
  try {
    const page = await fetchUserArticles(profile.value.id)
    articles.value = page.records
  } catch (e) {
    console.error('Failed to load articles:', e)
    articles.value = []
  } finally {
    articlesLoading.value = false
  }
}

async function loadRelations() {
  if (!profile.value) return
  relationLoading.value = true
  relationList.value = []
  try {
    let page
    switch (activeTab.value) {
      case 'friends':
        page = await fetchUserFriends(profile.value.id)
        break
      case 'following':
        page = await fetchUserFollowing(profile.value.id)
        break
      case 'followers':
        page = await fetchUserFollowers(profile.value.id)
        break
      default:
        return
    }
    relationList.value = page.records
  } catch {
    relationList.value = []
  } finally {
    relationLoading.value = false
  }
}

async function loadFavorites() {
  if (!profile.value) return
  favoritesLoading.value = true
  try {
    const page = await fetchMyFavorites()
    favoriteRecords.value = page.records
  } catch {
    favoriteRecords.value = []
  } finally {
    favoritesLoading.value = false
  }
}

async function loadLikes() {
  if (!profile.value) return
  likesLoading.value = true
  try {
    const page = await fetchMyLikes()
    likeRecords.value = page.records
  } catch {
    likeRecords.value = []
  } finally {
    likesLoading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'articles') {
    loadArticles()
  } else if (tab === 'favorites') {
    loadFavorites()
  } else if (tab === 'likes') {
    loadLikes()
  } else if (tab === 'appearance') {
    resetBlogThemeForm()
  } else {
    loadRelations()
  }
})

watch(userId, () => {
  activeTab.value = 'articles'
  loadProfile()
})

watch(() => route.query.tab, () => {
  if (profile.value) {
    syncTabFromRoute()
  }
})

function formatDate(value?: string | null): string {
  return formatDateToDay(value)
}

onMounted(loadProfile)
</script>

<style scoped>
/* --- Core Layout --- */
.profile-page {
  min-height: 100dvh;
  background:
    radial-gradient(circle at 12% 8%, color-mix(in srgb, var(--blog-accent, #2563eb) 16%, transparent), transparent 28%),
    var(--blog-canvas, #fafafa);
  color: var(--blog-body, #09090b);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
  padding: 40px 24px 120px;
}

.profile-page[data-blog-canvas='linen'] {
  background:
    repeating-linear-gradient(90deg, rgba(17, 24, 39, 0.035) 0 1px, transparent 1px 18px),
    repeating-linear-gradient(0deg, rgba(17, 24, 39, 0.03) 0 1px, transparent 1px 18px),
    var(--blog-canvas, #fafafa);
}

.profile-page[data-blog-canvas='gradient'] {
  background:
    radial-gradient(circle at 84% 10%, color-mix(in srgb, var(--blog-accent, #2563eb) 26%, transparent), transparent 30%),
    linear-gradient(135deg, var(--blog-canvas, #fafafa), color-mix(in srgb, var(--blog-accent, #2563eb) 14%, #ffffff));
}

.profile-page[data-blog-canvas='image'] {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blog-canvas, #fafafa) 86%, transparent), var(--blog-canvas, #fafafa)),
    var(--blog-canvas-image),
    var(--blog-canvas, #fafafa);
  background-size: cover;
  background-attachment: fixed;
}

.profile-layout {
  max-width: 1440px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr;
  gap: 48px;
  align-items: start;
}

@media (min-width: 1024px) {
  .profile-layout {
    grid-template-columns: var(--blog-sidebar-width, 320px) 1fr;
    gap: 80px;
  }

  .profile-page[data-blog-layout='notebook'] .profile-layout {
    max-width: 1120px;
    grid-template-columns: 280px minmax(0, 760px);
    gap: 56px;
  }

  .profile-page[data-blog-layout='gallery'] .profile-layout {
    max-width: 1500px;
    grid-template-columns: 260px 1fr;
    gap: 52px;
  }
}

/* --- Left Sidebar (Identity) --- */
.profile-sidebar {
  position: sticky;
  top: 40px;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-avatar-wrapper {
  width: 120px;
  height: 120px;
  border-radius: var(--blog-radius-lg, 20px);
  overflow: hidden;
  background: var(--blog-chip, #e4e4e7);
  display: grid;
  place-items: center;
  box-shadow: var(--blog-shadow, 0 4px 20px rgba(0, 0, 0, 0.05));
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-placeholder {
  color: #a1a1aa;
}

.profile-titles {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-name {
  font-size: clamp(28px, 3vw, 36px);
  font-family: var(--blog-font-heading, inherit);
  font-weight: var(--blog-heading-weight, 800);
  letter-spacing: 0;
  line-height: 1.1;
  color: var(--blog-title, #09090b);
  margin: 0;
}

.profile-handle {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
  color: #71717a;
  margin: 0;
}

.profile-bio {
  font-size: 15px;
  line-height: 1.6;
  color: var(--blog-muted, var(--blog-body, #52525b));
  margin: 0;
  max-width: 90%;
}

.profile-raw-stats {
  display: flex;
  gap: 32px;
  padding-top: 16px;
  border-top: 1px solid var(--blog-line, #e4e4e7);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-item strong {
  font-size: 20px;
  font-weight: 700;
  color: var(--blog-title, #09090b);
  letter-spacing: 0;
}

.stat-item span {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--blog-muted, #71717a);
  font-weight: 600;
}

.profile-actions {
  display: grid;
  gap: 10px;
  padding-top: 16px;
}

.profile-message-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-decoration: none;
}

.w-full {
  width: 100%;
}

/* --- Edit Form --- */
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--blog-card-strong, #fff);
  padding: 24px;
  border-radius: var(--blog-radius-lg, 16px);
  border: 1px solid var(--blog-line, #e4e4e7);
  box-shadow: var(--blog-shadow, 0 10px 30px rgba(0, 0, 0, 0.03));
}

.avatar-edit-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  background: #e4e4e7;
  display: grid;
  place-items: center;
  cursor: pointer;
  margin: 0 auto 8px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.avatar-edit-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-edit-placeholder {
  color: #a1a1aa;
}

.avatar-edit-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-edit-wrapper:hover .avatar-edit-overlay {
  opacity: 1;
}

.hidden-input {
  display: none;
}

.edit-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--blog-line, #d4d4d8);
  border-radius: var(--blog-radius, 8px);
  font: inherit;
  font-size: 14px;
  color: var(--blog-title, #09090b);
  background: var(--blog-card, #fafafa);
  transition: all 0.2s;
}

.edit-input:focus {
  outline: none;
  border-color: var(--blog-accent, #09090b);
  background: var(--blog-card-strong, #fff);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--blog-accent, #09090b) 14%, transparent);
}

.edit-input--title {
  font-weight: 600;
}

.edit-input--textarea {
  resize: vertical;
  min-height: 80px;
}

.password-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.edit-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.edit-actions button {
  flex: 1;
}

/* --- Buttons --- */
.btn-solid {
  background: var(--blog-title, #09090b);
  color: #fff;
  border: 1px solid var(--blog-title, #09090b);
  padding: 12px 24px;
  border-radius: var(--blog-radius, 8px);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-solid:hover {
  background: var(--blog-accent, #27272a);
  border-color: var(--blog-accent, #27272a);
}

.btn-solid:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  color: var(--blog-title, #09090b);
  border: 1px solid var(--blog-line, #d4d4d8);
  padding: 12px 24px;
  border-radius: var(--blog-radius, 8px);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline:hover {
  border-color: var(--blog-accent, #09090b);
  background: var(--blog-chip, #f4f4f5);
}

.btn-outline:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* --- Right Content (Nav & Grid) --- */
.profile-nav {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid var(--blog-line, #e4e4e7);
  margin-bottom: 32px;
  overflow-x: auto;
  scrollbar-width: none; /* Firefox */
}

.profile-nav::-webkit-scrollbar {
  display: none; /* Safari and Chrome */
}

.profile-nav button {
  background: none;
  border: none;
  padding: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--blog-muted, #71717a);
  cursor: pointer;
  position: relative;
  white-space: nowrap;
  transition: color 0.2s;
}

.profile-nav button span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--blog-chip, #f4f4f5);
  color: var(--blog-muted, #52525b);
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 999px;
  margin-left: 6px;
}

.profile-nav button:hover {
  color: var(--blog-title, #09090b);
}

.profile-nav button.is-active {
  color: var(--blog-title, #09090b);
}

.profile-nav button.is-active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--blog-accent, #09090b);
  border-radius: 2px;
}

/* --- Content Area --- */
.content-fade-in {
  animation: fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  color: var(--blog-muted, #71717a);
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--blog-title, #09090b);
}

.journal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--blog-card-gap, 24px);
}

.profile-page[data-blog-layout='notebook'] .journal-grid {
  grid-template-columns: 1fr;
  gap: 18px;
}

.profile-page[data-blog-layout='gallery'] .journal-grid {
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 18px;
}

/* Redesigned Journal Card */
.journal-card {
  display: flex;
  flex-direction: column;
  background: var(--blog-card-strong, var(--blog-paper, #fff));
  border: 1px solid var(--blog-line, #e4e4e7);
  border-radius: var(--blog-radius-lg, 12px);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.profile-page[data-blog-layout='notebook'] .journal-card {
  display: grid;
  grid-template-columns: minmax(150px, 0.34fr) 1fr;
  min-height: 188px;
  border-radius: 6px;
}

.profile-page[data-blog-layout='gallery'] .journal-card {
  border-radius: 6px;
}

.journal-card:hover {
  transform: translateY(-4px);
  border-color: color-mix(in srgb, var(--blog-accent, #2563eb) 38%, var(--blog-line, #d4d4d8));
  box-shadow: var(--blog-shadow, 0 12px 32px rgba(0, 0, 0, 0.06));
}

.journal-card__visual {
  width: 100%;
  height: 160px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--cover-from, #18181b) 72%, transparent), color-mix(in srgb, var(--blog-accent, #2563eb) 34%, transparent)),
    var(--blog-cover-gradient, var(--cover-from, #18181b));
  display: grid;
  place-items: center;
  font-size: 32px;
  font-weight: 800;
  color: var(--cover-accent, #fff);
  position: relative;
  overflow: hidden;
}

.profile-page[data-blog-layout='notebook'] .journal-card__visual {
  height: 100%;
  min-height: 188px;
}

.profile-page[data-blog-layout='gallery'] .journal-card__visual {
  height: 220px;
}

.journal-card__visual img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.journal-card:hover .journal-card__visual img {
  transform: scale(1.05);
}

.journal-card__content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-page[data-blog-layout='gallery'] .journal-card__content {
  padding: 16px;
}

@media (max-width: 699px) {
  .profile-page[data-blog-layout='notebook'] .journal-card {
    display: flex;
    min-height: 0;
  }

  .profile-page[data-blog-layout='notebook'] .journal-card__visual {
    height: 160px;
    min-height: 0;
  }
}

.article-meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  font-family: var(--blog-font-mono, ui-monospace, SFMono-Regular, monospace);
}

.privacy-badge, .favorite-type-badge {
  background: var(--blog-chip, #f4f4f5);
  color: var(--blog-muted, #52525b);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.article-date {
  color: var(--blog-muted, #a1a1aa);
}

.journal-card h2 {
  font-size: 18px;
  font-family: var(--blog-font-heading, inherit);
  font-weight: var(--blog-heading-weight, 700);
  line-height: 1.3;
  margin: 0;
  color: var(--blog-title, #09090b);
}

.article-summary {
  font-size: 14px;
  color: var(--blog-muted, var(--blog-body, #52525b));
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.tag-row span {
  font-size: 12px;
  color: var(--blog-accent, #71717a);
  background: var(--blog-chip, #f4f4f5);
  padding: 2px 8px;
  border-radius: 999px;
}

/* Redesigned User List */
.profile-user-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.profile-user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--blog-card-strong, #fff);
  border: 1px solid var(--blog-line, #e4e4e7);
  border-radius: var(--blog-radius-lg, 12px);
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
}

.profile-user-card:hover {
  border-color: var(--blog-accent, #09090b);
  background: var(--blog-card, #fafafa);
}

.profile-user-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--blog-radius, 12px);
  background: var(--blog-chip, #e4e4e7);
  overflow: hidden;
  display: grid;
  place-items: center;
  color: #71717a;
}

.profile-user-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-user-card__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.profile-user-card__info strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--blog-title, #09090b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profile-user-card__info span {
  font-family: var(--blog-font-mono, ui-monospace, SFMono-Regular, monospace);
  font-size: 12px;
  color: var(--blog-muted, #71717a);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.appearance-studio {
  display: grid;
  gap: 18px;
}

@media (min-width: 1120px) {
  .appearance-studio {
    grid-template-columns: minmax(0, 0.98fr) minmax(400px, 0.72fr);
    align-items: start;
  }

  .appearance-preview {
    grid-column: 2;
    grid-row: 1 / span 2;
  }
}

.appearance-library,
.appearance-controls,
.appearance-preview {
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 20%, var(--blog-line, #e4e4e7));
  border-radius: var(--blog-radius-lg, 16px);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blog-card-strong, #ffffff) 96%, transparent), color-mix(in srgb, var(--blog-card, #ffffff) 90%, transparent));
  box-shadow: var(--blog-shadow, 0 18px 48px rgba(17, 24, 39, 0.06));
}

.appearance-library {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.appearance-controls {
  display: grid;
  gap: 18px;
  padding: 18px;
}

.appearance-heading {
  display: grid;
  gap: 6px;
}

.appearance-heading h2 {
  margin: 0;
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-heading, inherit);
  font-size: 22px;
  font-weight: var(--blog-heading-weight, 780);
  line-height: 1.2;
}

.appearance-heading span {
  color: var(--blog-muted, #71717a);
  font-size: 13px;
  line-height: 1.6;
}

.appearance-heading--compact h2 {
  font-size: 18px;
}

.appearance-eyebrow {
  margin: 0;
  color: var(--blog-accent, #2563eb);
  font-family: var(--blog-font-mono, ui-monospace, monospace);
  font-size: 11px;
  font-weight: 780;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.appearance-presets {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(178px, 1fr));
  gap: 12px;
}

.theme-preset {
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 10px;
  min-height: 178px;
  padding: 12px;
  border: 1px solid var(--blog-line, rgba(17, 24, 39, 0.08));
  border-radius: var(--blog-radius, 10px);
  background:
    radial-gradient(circle at 18% 0%, color-mix(in srgb, var(--blog-accent, #2563eb) 18%, transparent), transparent 38%),
    var(--blog-card-strong, var(--blog-paper, #ffffff));
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.theme-preset:hover,
.theme-preset.is-active {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--blog-accent, #2563eb) 54%, var(--blog-line, transparent));
  box-shadow: var(--blog-shadow, 0 18px 40px rgba(17, 24, 39, 0.08));
}

.theme-preset__swatch {
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 66px;
  border-radius: 8px;
  background:
    repeating-linear-gradient(90deg, color-mix(in srgb, var(--blog-title, #111827) 10%, transparent) 0 1px, transparent 1px 18px),
    var(--blog-cover-gradient, linear-gradient(135deg, var(--blog-canvas, #f8fafc), var(--blog-accent, #2563eb)));
}

.theme-preset__swatch i,
.theme-preset__swatch b {
  position: absolute;
  display: block;
  background: var(--blog-paper, #ffffff);
  box-shadow: 0 12px 26px rgba(17, 24, 39, 0.12);
}

.theme-preset__swatch i {
  left: 12px;
  bottom: 10px;
  width: 48%;
  height: 26px;
  border-radius: 6px;
}

.theme-preset__swatch b {
  right: 12px;
  bottom: 13px;
  width: 24%;
  height: 20px;
  border-radius: 999px;
}

.theme-preset__body {
  display: grid;
  gap: 4px;
}

.theme-preset__body strong {
  font-family: var(--blog-font-heading, inherit);
  font-size: 15px;
  font-weight: var(--blog-heading-weight, 760);
}

.theme-preset__body small {
  color: var(--blog-muted, #71717a);
  font-size: 12px;
  line-height: 1.5;
}

.theme-preset em {
  justify-self: start;
  padding: 5px 8px;
  border-radius: 999px;
  background: var(--blog-chip, #f4f4f5);
  color: var(--blog-accent, #2563eb);
  font-size: 11px;
  font-style: normal;
  font-weight: 760;
}

.theme-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.theme-form-grid label,
.canvas-upload {
  display: grid;
  gap: 8px;
  color: var(--blog-title, #111827);
  font-size: 13px;
  font-weight: 720;
}

.theme-form-grid label:first-child {
  grid-column: 1 / -1;
}

.color-field input[type='color'] {
  width: 100%;
  min-height: 42px;
  padding: 4px;
  border: 1px solid var(--blog-line, #d4d4d8);
  border-radius: var(--blog-radius, 8px);
  background: var(--blog-card-strong, #ffffff);
}

.appearance-actions {
  margin-top: 0;
}

.appearance-preview {
  position: sticky;
  top: 36px;
  display: grid;
  gap: 14px;
  padding: 14px;
  background:
    radial-gradient(circle at 92% 0%, color-mix(in srgb, var(--blog-accent, #2563eb) 22%, transparent), transparent 34%),
    linear-gradient(180deg, color-mix(in srgb, var(--blog-paper, #ffffff) 96%, transparent), color-mix(in srgb, var(--blog-canvas, #f8fafc) 92%, transparent)),
    var(--blog-canvas-image);
  color: var(--blog-body, #374151);
  font-family: var(--blog-font-body, var(--blog-font, inherit));
  backdrop-filter: blur(var(--blog-surface-blur, 14px));
}

.appearance-preview[data-blog-canvas='linen'] {
  background:
    repeating-linear-gradient(90deg, rgba(17, 24, 39, 0.035) 0 1px, transparent 1px 16px),
    repeating-linear-gradient(0deg, rgba(17, 24, 39, 0.03) 0 1px, transparent 1px 16px),
    var(--blog-paper, #ffffff);
}

.appearance-preview[data-blog-canvas='gradient'] {
  background:
    radial-gradient(circle at 82% 8%, color-mix(in srgb, var(--blog-accent, #2563eb) 22%, transparent), transparent 34%),
    linear-gradient(135deg, var(--blog-paper, #ffffff), color-mix(in srgb, var(--blog-accent, #2563eb) 14%, var(--blog-canvas, #f8fafc)));
}

.appearance-preview[data-blog-canvas='image'] {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blog-paper, #ffffff) 82%, transparent), var(--blog-paper, #ffffff)),
    var(--blog-canvas-image),
    var(--blog-canvas, #f8fafc);
  background-size: cover;
  background-position: center;
}

.appearance-preview h2 {
  margin: 0;
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-heading, inherit);
  font-size: clamp(24px, 3vw, 34px);
  font-weight: var(--blog-heading-weight, 820);
  line-height: 1.12;
}

.appearance-preview p {
  margin: 0;
  color: var(--blog-body, #374151);
  font-size: 16px;
  line-height: 1.78;
}

.appearance-preview[data-blog-layout='notebook'] {
  border-radius: var(--blog-radius, 6px);
  gap: 14px;
  justify-self: center;
}

.appearance-preview[data-blog-layout='gallery'] {
  min-height: 520px;
}

.preview-browser-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 10px;
  border-radius: var(--blog-radius, 8px);
  background: color-mix(in srgb, var(--blog-card-strong, #ffffff) 78%, transparent);
  border: 1px solid color-mix(in srgb, var(--blog-line, #e4e4e7) 84%, transparent);
}

.preview-browser-bar span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--blog-accent, #2563eb);
}

.preview-browser-bar span:nth-child(2) {
  opacity: 0.55;
}

.preview-browser-bar span:nth-child(3) {
  opacity: 0.28;
}

.preview-browser-bar strong {
  min-width: 0;
  margin-left: 4px;
  color: var(--blog-muted, #71717a);
  font-size: 11px;
  font-weight: 760;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-author-card {
  display: grid;
  grid-template-columns: 54px 1fr;
  gap: 12px;
  align-items: center;
  padding: 18px;
  border-radius: var(--blog-radius-lg, 16px);
  background: color-mix(in srgb, var(--blog-card-strong, #ffffff) 86%, transparent);
  border: 1px solid color-mix(in srgb, var(--blog-line, #e4e4e7) 88%, transparent);
}

.preview-avatar {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: var(--blog-radius, 10px);
  background: var(--blog-cover-gradient, var(--blog-accent, #2563eb));
  color: var(--blog-paper, #ffffff);
  font-family: var(--blog-font-heading, inherit);
  font-size: 22px;
  font-weight: 840;
}

.preview-article-card {
  display: grid;
  overflow: hidden;
  border-radius: var(--blog-radius-lg, 16px);
  border: 1px solid color-mix(in srgb, var(--blog-line, #e4e4e7) 88%, transparent);
  background: var(--blog-card-strong, #ffffff);
  box-shadow: var(--blog-block-shadow, 0 12px 30px rgba(17, 24, 39, 0.05));
}

.preview-cover {
  min-height: 138px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--blog-title, #111827) 40%, transparent), color-mix(in srgb, var(--blog-accent, #2563eb) 26%, transparent)),
    var(--blog-cover-gradient, var(--blog-accent, #2563eb));
}

.preview-article-body,
.preview-markdown-sample {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.preview-article-body h3,
.preview-markdown-sample h3 {
  margin: 0;
  color: var(--blog-title, #111827);
  font-family: var(--blog-font-heading, inherit);
  font-size: 20px;
  font-weight: var(--blog-heading-weight, 760);
  line-height: 1.25;
}

.preview-markdown-sample {
  border-radius: var(--blog-radius-lg, 16px);
  background: color-mix(in srgb, var(--blog-card-strong, #ffffff) 82%, transparent);
  border: 1px solid var(--blog-block-border, var(--blog-line, #e4e4e7));
}

.preview-markdown-sample pre {
  max-width: 100%;
  overflow: auto;
  margin: 0;
  padding: 12px;
  border-radius: var(--blog-radius, 8px);
  background: var(--blog-code-bg, #f4f4f5);
  color: var(--blog-code-text, #2563eb);
  font-family: var(--blog-font-mono, monospace);
  font-size: 12px;
}

.appearance-preview[data-blog-layout='gallery'] blockquote {
  align-self: stretch;
}

.preview-kicker {
  color: var(--blog-accent, #2563eb) !important;
  font-size: 12px !important;
  font-weight: 820;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.appearance-preview blockquote {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--blog-block-border, color-mix(in srgb, var(--blog-accent, #2563eb) 20%, transparent));
  border-left: 4px solid var(--blog-accent, #2563eb);
  border-radius: var(--blog-radius, 8px);
  background: var(--blog-block-bg, color-mix(in srgb, var(--blog-accent, #2563eb) 8%, transparent));
  color: var(--blog-title, #111827);
}

.appearance-preview[data-blog-block='ink'] blockquote {
  border: 1px solid color-mix(in srgb, var(--blog-title, #111827) 34%, transparent);
  border-left-width: 6px;
  background: transparent;
}

.appearance-preview[data-blog-block='carded'] blockquote {
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 22%, transparent);
  border-left-width: 1px;
  box-shadow: 0 14px 34px rgba(17, 24, 39, 0.08);
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 10%, transparent);
  color: var(--blog-accent, #2563eb);
  font-size: 12px;
  font-weight: 760;
}

.inline-notice {
  margin: 0;
  color: var(--blog-accent, #2563eb);
  font-size: 13px;
  font-weight: 720;
}

@media (max-width: 760px) {
  .appearance-presets,
  .theme-form-grid {
    grid-template-columns: 1fr;
  }

  .appearance-preview[data-blog-layout='gallery'] {
    grid-template-columns: 1fr;
    min-height: 0;
  }
}

.setting-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.setting-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--blog-muted, #71717a);
  text-align: left;
}

.edit-select {
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%2371717a' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'></polyline></svg>");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 16px;
  padding-right: 40px;
  cursor: pointer;
}
</style>
