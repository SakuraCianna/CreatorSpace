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
              <section class="appearance-controls">
                <div class="appearance-presets">
                  <button
                    v-for="preset in BLOG_THEME_PRESETS"
                    :key="preset.displayName"
                    type="button"
                    class="theme-preset"
                    :style="blogThemeToStyle(preset)"
                    @click="applyThemePreset(preset)"
                  >
                    <span class="theme-preset__swatch" />
                    <strong>{{ preset.displayName }}</strong>
                  </button>
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
                <p class="preview-kicker">{{ blogThemeForm.displayName }}</p>
                <h2>{{ profile.nickname || profile.username }} 的主题博客</h2>
                <p>
                  今天把一段想法整理成文章。第一行写给风, 第二行留给夜里的灯。
                </p>
                <blockquote>这里有自己的呼吸, 也有慢慢成形的秩序。</blockquote>
                <div class="preview-tags">
                  <span>字体</span>
                  <span>画布</span>
                  <span>文章块</span>
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
const editForm = ref({ nickname: '', avatarUrl: '', bio: '' })
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
  } catch (err: any) {
    alert(err.message || '头像上传失败')
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
    radial-gradient(circle at 12% 8%, color-mix(in srgb, var(--blog-accent, #2563eb) 12%, transparent), transparent 28%),
    var(--blog-canvas, #fafafa);
  color: var(--blog-body, #09090b);
  font-family: var(--blog-font, inherit);
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
    grid-template-columns: 320px 1fr;
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
  border-radius: 20px; /* Squircle style */
  overflow: hidden;
  background: #e4e4e7;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
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
  font-weight: 800;
  letter-spacing: -0.03em;
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
  color: var(--blog-body, #52525b);
  margin: 0;
  max-width: 90%;
}

.profile-raw-stats {
  display: flex;
  gap: 32px;
  padding-top: 16px;
  border-top: 1px solid #e4e4e7;
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
  letter-spacing: -0.02em;
}

.stat-item span {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #71717a;
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
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #e4e4e7;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
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
  border: 1px solid #d4d4d8;
  border-radius: 8px;
  font: inherit;
  font-size: 14px;
  color: #09090b;
  background: #fafafa;
  transition: all 0.2s;
}

.edit-input:focus {
  outline: none;
  border-color: #09090b;
  background: #fff;
  box-shadow: 0 0 0 1px #09090b;
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
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-solid:hover {
  background: #27272a;
  border-color: #27272a;
}

.btn-solid:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  color: var(--blog-title, #09090b);
  border: 1px solid #d4d4d8;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline:hover {
  border-color: #09090b;
  background: #f4f4f5;
}

.btn-outline:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* --- Right Content (Nav & Grid) --- */
.profile-nav {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #e4e4e7;
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
  color: #71717a;
  cursor: pointer;
  position: relative;
  white-space: nowrap;
  transition: color 0.2s;
}

.profile-nav button span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f4f4f5;
  color: #52525b;
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
  color: #71717a;
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #09090b;
}

.journal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
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
  background: var(--blog-paper, #fff);
  border: 1px solid #e4e4e7;
  border-radius: 12px;
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
  border-color: #d4d4d8;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.06);
}

.journal-card__visual {
  width: 100%;
  height: 160px;
  background: var(--cover-from, #18181b);
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
  font-family: ui-monospace, SFMono-Regular, monospace;
}

.privacy-badge, .favorite-type-badge {
  background: #f4f4f5;
  color: #52525b;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.article-date {
  color: #a1a1aa;
}

.journal-card h2 {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0;
  color: var(--blog-title, #09090b);
}

.article-summary {
  font-size: 14px;
  color: var(--blog-body, #52525b);
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
  color: #71717a;
  background: #f4f4f5;
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
  background: #fff;
  border: 1px solid #e4e4e7;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
}

.profile-user-card:hover {
  border-color: #09090b;
  background: #fafafa;
}

.profile-user-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px; /* Matching the squircle aesthetic */
  background: #e4e4e7;
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
  color: #09090b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.profile-user-card__info span {
  font-family: ui-monospace, SFMono-Regular, monospace;
  font-size: 12px;
  color: #71717a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.appearance-studio {
  display: grid;
  gap: 24px;
}

@media (min-width: 1120px) {
  .appearance-studio {
    grid-template-columns: minmax(0, 0.95fr) minmax(360px, 0.72fr);
    align-items: start;
  }
}

.appearance-controls,
.appearance-preview {
  border: 1px solid color-mix(in srgb, var(--blog-accent, #2563eb) 18%, #e4e4e7);
  border-radius: 12px;
  background: color-mix(in srgb, var(--blog-paper, #ffffff) 92%, transparent);
  box-shadow: 0 18px 48px rgba(17, 24, 39, 0.06);
}

.appearance-controls {
  display: grid;
  gap: 18px;
  padding: 18px;
}

.appearance-presets {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.theme-preset {
  display: grid;
  gap: 8px;
  justify-items: start;
  min-height: 86px;
  padding: 12px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 10px;
  background: var(--blog-paper, #ffffff);
  color: var(--blog-title, #111827);
  font-family: var(--blog-font, inherit);
  text-align: left;
  cursor: pointer;
}

.theme-preset__swatch {
  width: 100%;
  height: 24px;
  border-radius: 7px;
  background:
    linear-gradient(90deg, var(--blog-accent, #2563eb) 0 35%, var(--blog-canvas, #f8fafc) 35% 70%, var(--blog-title, #111827) 70%);
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
  border: 1px solid #d4d4d8;
  border-radius: 8px;
  background: #ffffff;
}

.appearance-actions {
  margin-top: 0;
}

.appearance-preview {
  position: sticky;
  top: 36px;
  display: grid;
  gap: 16px;
  padding: clamp(22px, 4vw, 34px);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--blog-paper, #ffffff) 96%, transparent), var(--blog-paper, #ffffff)),
    var(--blog-canvas-image);
  color: var(--blog-body, #374151);
  font-family: var(--blog-font, inherit);
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
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.12;
}

.appearance-preview p {
  margin: 0;
  color: var(--blog-body, #374151);
  font-size: 16px;
  line-height: 1.78;
}

.appearance-preview[data-blog-layout='notebook'] {
  border-radius: 6px;
  gap: 14px;
  max-width: 520px;
  justify-self: center;
}

.appearance-preview[data-blog-layout='gallery'] {
  grid-template-columns: 1fr 1fr;
  align-items: end;
  min-height: 420px;
}

.appearance-preview[data-blog-layout='gallery'] h2 {
  grid-column: 1 / -1;
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
  border-left: 4px solid var(--blog-accent, #2563eb);
  border-radius: 8px;
  background: color-mix(in srgb, var(--blog-accent, #2563eb) 8%, transparent);
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
</style>
