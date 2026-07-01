<template>
  <section ref="root" class="profile-page">
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
import { LoaderCircle, UserRound, Camera } from '@lucide/vue'
import { uploadFile } from '../services/file'
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
} from '../services/content'
import { toUserMessage } from '../services/http'
import { useFollow } from '../shared/composables/useFollow'
import { usePageReveal } from '../shared/composables/usePageReveal'
import { formatDateToDay } from '../shared/datetime'
import { useSessionStore } from '../shared/sessionStore'
import type { ArticleSummary, FavoriteRecord, FollowUser, InteractionRecord, UserProfile } from '../shared/domain'

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
const activeTab = ref('articles')

const { following, isFriend, loadStatus: loadFollowStatus, toggleFollow } = useFollow()
const userId = computed(() => Number(route.params.userId))

const canFollow = computed(() => {
  if (!session.accessToken || !profile.value) return false
  return session.currentUser?.id !== profile.value.id
})

const isOwnProfile = computed(() => {
  return Boolean(session.currentUser && profile.value && session.currentUser.id === profile.value.id)
})

const editing = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)
const editForm = ref({ nickname: '', avatarUrl: '', bio: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '' })

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
    profile.value = data
    await Promise.all([loadArticles(), loadFavorites(), loadLikes()])
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
  } else {
    loadRelations()
  }
})

watch(userId, () => {
  activeTab.value = 'articles'
  loadProfile()
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
  background: var(--tone-background, #fafafa);
  color: var(--tone-ink, #09090b);
  padding: 40px 24px 120px;
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
  color: #09090b;
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
  color: #52525b;
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
  color: #09090b;
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
  padding-top: 16px;
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
  background: #09090b;
  color: #fff;
  border: 1px solid #09090b;
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
  color: #09090b;
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
  color: #09090b;
}

.profile-nav button.is-active {
  color: #09090b;
}

.profile-nav button.is-active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: #09090b;
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

/* Redesigned Journal Card */
.journal-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e4e4e7;
  border-radius: 12px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
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
  color: #09090b;
}

.article-summary {
  font-size: 14px;
  color: #52525b;
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
</style>
