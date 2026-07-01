<template>
  <section ref="root" class="profile-page">
    <div v-if="isLoading" class="empty-state profile-state" data-reveal>
      <LoaderCircle class="spin" :size="24" />
      <h2>正在加载用户信息</h2>
    </div>
    <template v-else-if="profile">
      <header class="profile-hero" data-reveal>
        <div class="profile-hero__bg"></div>
        <div class="profile-hero__main">
          <div class="profile-avatar-frame">
            <img
              v-if="profile.avatarUrl"
              :src="profile.avatarUrl"
              alt=""
              class="profile-avatar-img"
              loading="lazy"
            />
            <UserRound v-else :size="64" class="profile-avatar-placeholder" />
          </div>
          <div class="profile-hero__info">
            <template v-if="editing">
              <div class="edit-mode-form">
                <input v-model="editForm.nickname" class="edit-input edit-input--title" placeholder="昵称" />
                <div class="avatar-upload-wrapper">
                  <FileUpload
                    v-model="editForm.avatarUrl"
                    module="AVATAR"
                    accept="image/*"
                    hint="建议 800x800，最大 100MB"
                  />
                </div>
                <textarea v-model="editForm.bio" class="edit-input edit-input--textarea" placeholder="个人简介" rows="3" />
                <div class="password-edit-section">
                  <input v-model="passwordForm.oldPassword" type="password" class="edit-input" placeholder="原密码 (留空不改)" />
                  <input v-model="passwordForm.newPassword" type="password" class="edit-input" placeholder="新密码" />
                </div>
                <div class="profile-actions edit-actions">
                  <button class="button button-filled" type="button" :disabled="saving" @click="saveProfile">
                    {{ saving ? '保存中...' : '保存修改' }}
                  </button>
                  <button class="button button-outline" type="button" :disabled="saving" @click="cancelEditing">取消</button>
                </div>
              </div>
            </template>
            <template v-else>
              <h1>{{ profile.nickname || profile.username }}</h1>
              <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
              <div v-if="isOwnProfile" class="profile-actions">
                <button class="button button-outline" type="button" @click="startEditing">
                  <PenLine :size="16" />
                  编辑资料
                </button>
              </div>
              <div v-else-if="canFollow" class="profile-actions">
                <button
                  class="button"
                  :class="following ? 'button-outline' : 'button-filled'"
                  type="button"
                  @click="toggleFollow(profile.id)"
                >
                  <UserPlus v-if="!following" :size="16" />
                  <UserCheck v-else :size="16" />
                  {{ isFriend ? '互相关注' : following ? '已关注' : '关注' }}
                </button>
              </div>
            </template>
          </div>
        </div>
        <div class="profile-stats-bar">
          <button :class="{ 'is-active': activeTab === 'articles' }" type="button" @click="activeTab = 'articles'">
            <strong>{{ profile.articleCount }}</strong>
            <span>文章</span>
          </button>
          <button :class="{ 'is-active': activeTab === 'friends' }" type="button" @click="activeTab = 'friends'">
            <strong>{{ profile.friendCount }}</strong>
            <span>好友</span>
          </button>
          <button :class="{ 'is-active': activeTab === 'following' }" type="button" @click="activeTab = 'following'">
            <strong>{{ profile.followingCount }}</strong>
            <span>关注</span>
          </button>
          <button :class="{ 'is-active': activeTab === 'followers' }" type="button" @click="activeTab = 'followers'">
            <strong>{{ profile.followerCount }}</strong>
            <span>粉丝</span>
          </button>
          <template v-if="isOwnProfile">
            <button :class="{ 'is-active': activeTab === 'favorites' }" type="button" @click="activeTab = 'favorites'">
              <strong>{{ favoriteRecords.length }}</strong>
              <span>收藏</span>
            </button>
            <button :class="{ 'is-active': activeTab === 'likes' }" type="button" @click="activeTab = 'likes'">
              <strong>{{ likeRecords.length }}</strong>
              <span>喜欢</span>
            </button>
          </template>
        </div>
      </header>

      <!-- 文章列表 -->
      <div v-if="activeTab === 'articles'" class="profile-section" data-reveal>
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
            <div>
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
      <div v-if="activeTab === 'favorites'" class="profile-section" data-reveal>
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
            <div>
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
      <div v-if="activeTab === 'likes'" class="profile-section" data-reveal>
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
            <div>
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
      <div v-if="['friends', 'following', 'followers'].includes(activeTab)" class="profile-section" data-reveal>
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
              <UserRound v-else :size="24" />
            </div>
            <div class="profile-user-card__info">
              <strong>{{ user.nickname || user.username }}</strong>
              <span>@{{ user.username }}</span>
            </div>
          </RouterLink>
        </div>
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
import { ArrowLeft, ArrowRight, LoaderCircle, PenLine, UserCheck, UserPlus, UserRound } from '@lucide/vue'
import FileUpload from '../components/common/FileUpload.vue'
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
      // Optional: force logout here
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
  ['#111827', '#6ea8ff', '#f8fafc'],
  ['#2f163f', '#b18cff', '#fff7ed'],
  ['#10312e', '#54e6c8', '#f7fee7'],
  ['#3a2508', '#ff9d6e', '#fff8db'],
  ['#172554', '#60a5fa', '#eef6ff'],
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
.profile-page {
  padding: 36px 0 78px;
}

.profile-state {
  min-height: 320px;
}

.profile-hero {
  position: relative;
  display: grid;
  gap: 24px;
  padding: 0;
  border-radius: 20px;
  background: var(--tone-panel);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.profile-hero::before {
  content: '';
  display: block;
  width: 100%;
  height: 140px;
  background: 
    radial-gradient(circle at 0% 0%, #315bff 0%, transparent 50%),
    radial-gradient(circle at 100% 100%, #1e3a8a 0%, transparent 50%),
    radial-gradient(circle at 100% 0%, #38bdf8 0%, transparent 50%),
    linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  background-size: cover;
  background-position: center;
}

.profile-hero__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  padding: 0 40px;
  margin-top: -50px;
  position: relative;
  z-index: 2;
  text-align: center;
}

.profile-avatar-frame {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid #ffffff;
  overflow: hidden;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: #f1f5f9;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-placeholder {
  color: #94a3b8;
}

.profile-hero__info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.profile-hero__info h1 {
  margin: 0;
  font-size: clamp(24px, 2.5vw, 28px);
  font-weight: 800;
  line-height: 1.2;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.profile-username {
  margin: 0;
  color: #64748b;
  font-size: 16px;
  font-weight: 500;
}

.profile-bio {
  margin: 4px 0 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.6;
  max-width: 600px;
}

.profile-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 16px;
}

.edit-mode-form {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 480px;
  gap: 12px;
}

.edit-input {
  display: block;
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--tone-line-strong);
  border-radius: 12px;
  background: var(--tone-panel-solid);
  color: var(--tone-ink);
  font: inherit;
  font-size: 15px;
  outline: none;
  transition: all 0.2s ease;
}

.edit-input:focus {
  border-color: #315bff;
  box-shadow: 0 0 0 4px rgba(49, 91, 255, 0.1);
}

.edit-input--title {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
}

.edit-input--textarea {
  resize: vertical;
  min-height: 80px;
  line-height: 1.6;
}

.avatar-upload-wrapper {
  width: 100%;
  margin: 4px 0;
}

.password-edit-section {
  display: flex;
  gap: 12px;
  width: 100%;
}

.password-edit-section .edit-input {
  flex: 1;
}

.profile-stats-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  padding: 24px 40px 40px;
}

.profile-stats-bar button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 90px;
  padding: 16px;
  border: none;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  font: inherit;
  transition: all 0.2s ease;
}

.profile-stats-bar button:hover {
  background: #f1f5f9;
  transform: translateY(-2px);
}

.profile-stats-bar button.is-active {
  background: #eef2ff;
  color: #315bff;
}

.profile-stats-bar button strong {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.profile-stats-bar button.is-active strong {
  color: #315bff;
}

.profile-stats-bar button span {
  font-size: 13px;
  font-weight: 500;
}

.profile-section {
  margin-top: var(--theme-density-spacing, 16px);
}

.article-date {
  color: var(--tone-primary);
  font-size: 12px;
  font-weight: 720;
}

.profile-user-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--theme-density-spacing, 16px);
}

.profile-user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #f1f5f9;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  text-decoration: none;
  color: inherit;
  transition: all 0.2s ease;
}

.profile-user-card:hover {
  border-color: #e2e8f0;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.profile-user-card__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #94a3b8;
}

.profile-user-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-user-card__info {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.profile-user-card__info strong {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-user-card__info span {
  color: #64748b;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.journal-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.journal-card {
  display: grid;
  grid-template-rows: 200px minmax(0, 1fr);
  border-radius: 16px;
  background: #fff;
  text-decoration: none;
  color: inherit;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #f1f5f9;
  overflow: hidden;
  transition: all 0.25s ease;
}

.journal-card:hover {
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  transform: translateY(-4px);
}

.journal-card__visual {
  position: relative;
  display: grid;
  height: 200px;
  place-items: center;
  overflow: hidden;
  background: radial-gradient(circle at 78% 18%, color-mix(in srgb, var(--cover-accent) 42%, transparent), transparent 34%),
              linear-gradient(135deg, color-mix(in srgb, var(--cover-from) 86%, #ffffff), color-mix(in srgb, var(--cover-accent) 74%, #f8fbff));
}

.journal-card__visual::after {
  content: "";
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.12), transparent 42%),
              repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 20px);
  pointer-events: none;
}

.journal-card__visual img {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.journal-card__visual span {
  position: relative;
  z-index: 1;
  color: rgba(248, 251, 255, 0.88);
  font-size: 42px;
  font-weight: 900;
  text-shadow: 0 16px 34px rgba(0, 0, 0, 0.26);
}

.journal-card > div:last-child {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 24px;
}

.journal-card h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.3;
  font-weight: 700;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.article-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.privacy-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: #fffbeb;
  color: #d97706;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.favorite-type-badge {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #315bff;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 6px;
}

.article-date {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}

@media (max-width: 760px) {
  .profile-page {
    padding-top: 24px;
  }

  .profile-hero__main {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 20px;
    margin-top: -60px;
  }

  .profile-hero__info {
    text-align: center;
  }

  .profile-bio {
    margin-left: auto;
    margin-right: auto;
  }

  .profile-actions {
    display: flex;
    justify-content: center;
  }

  .profile-stats-bar {
    justify-content: center;
  }

  .journal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
