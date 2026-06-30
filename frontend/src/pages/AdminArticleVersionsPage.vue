<template>
  <section class="cms-page article-versions-page">
    <header class="cms-header">
      <div>
        <h2>文章版本</h2>
        <p>{{ article?.title ?? '正在读取文章版本记录' }}</p>
      </div>
      <div class="header-actions">
        <RouterLink class="button button-tonal" :to="{ name: 'admin-articles' }">
          <ArrowLeft :size="16" />
          返回文章
        </RouterLink>
        <button class="button button-tonal" type="button" :disabled="loading" @click="reload">
          <RefreshCw :size="16" />
          刷新
        </button>
      </div>
    </header>

    <section class="cms-grid version-layout">
      <div class="cms-panel">
        <div class="panel-title">
          <h3>版本列表</h3>
          <span>{{ versions.length }} 条</span>
        </div>
        <div class="list-stack">
          <button
            v-for="version in versions"
            :key="version.id"
            class="version-row"
            :class="{ active: selectedVersion?.id === version.id }"
            type="button"
            @click="selectVersion(version.id)"
          >
            <strong>版本 {{ version.versionNo }}</strong>
            <span>{{ formatDateTimeToSecond(version.createdAt) }}</span>
            <small>{{ version.title }}</small>
          </button>
          <p v-if="versions.length === 0 && !loading" class="empty-hint">当前文章还没有版本记录。</p>
        </div>
      </div>

      <div class="cms-panel version-detail">
        <div class="panel-title">
          <h3>{{ selectedVersion ? `版本 ${selectedVersion.versionNo}` : '版本详情' }}</h3>
          <button
            v-if="selectedVersion"
            class="button button-filled"
            type="button"
            :disabled="restoring"
            @click="restoreSelectedVersion"
          >
            <RotateCcw :size="16" />
            恢复此版本
          </button>
        </div>

        <div v-if="selectedVersion" class="detail-stack">
          <label>
            标题
            <input :value="selectedVersion.title" readonly />
          </label>
          <label>
            摘要
            <textarea :value="selectedVersion.summary ?? ''" rows="4" readonly />
          </label>
          <label>
            Markdown 正文
            <pre>{{ selectedVersion.contentMarkdown }}</pre>
          </label>
        </div>
        <p v-else class="empty-hint">请选择一个版本查看内容。</p>
      </div>
    </section>

    <p v-if="notice" class="inline-notice">{{ notice }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft, RefreshCw, RotateCcw } from '@lucide/vue'

import {
  fetchAdminArticle,
  fetchAdminArticleVersion,
  fetchAdminArticleVersions,
  restoreAdminArticleVersion,
} from '@/services/content'
import { toUserMessage } from '@/services/http'
import { formatDateTimeToSecond } from '@/shared/datetime'
import type { ArticleSummary, ArticleVersionSummary } from '@/shared/domain'

const route = useRoute()
const articleId = computed(() => Number(route.params.id))

const article = ref<ArticleSummary | null>(null)
const versions = ref<ArticleVersionSummary[]>([])
const selectedVersion = ref<ArticleVersionSummary | null>(null)
const loading = ref(false)
const restoring = ref(false)
const notice = ref('')

onMounted(reload)

async function reload() {
  if (!Number.isFinite(articleId.value)) {
    notice.value = '文章 ID 无效'
    return
  }
  loading.value = true
  notice.value = ''
  try {
    const [articleDetail, versionList] = await Promise.all([
      fetchAdminArticle(articleId.value),
      fetchAdminArticleVersions(articleId.value),
    ])
    article.value = articleDetail
    versions.value = versionList
    if (versionList.length === 0) {
      selectedVersion.value = null
      return
    }
    const selectedId = selectedVersion.value?.id ?? versionList[0].id
    await selectVersion(selectedId)
  } catch (error) {
    notice.value = toUserMessage(error, '版本记录加载失败')
  } finally {
    loading.value = false
  }
}

async function selectVersion(versionId: number) {
  notice.value = ''
  try {
    selectedVersion.value = await fetchAdminArticleVersion(articleId.value, versionId)
  } catch (error) {
    notice.value = toUserMessage(error, '版本详情读取失败')
  }
}

async function restoreSelectedVersion() {
  if (!selectedVersion.value) {
    return
  }
  if (!window.confirm(`确认恢复到版本 ${selectedVersion.value.versionNo} 吗？`)) {
    return
  }
  restoring.value = true
  notice.value = ''
  try {
    const restoredVersionNo = selectedVersion.value.versionNo
    article.value = await restoreAdminArticleVersion(articleId.value, selectedVersion.value.id)
    selectedVersion.value = null
    await reload()
    notice.value = `已恢复到版本 ${restoredVersionNo}`
  } catch (error) {
    notice.value = toUserMessage(error, '版本恢复失败')
  } finally {
    restoring.value = false
  }
}
</script>

<style scoped>
.article-versions-page {
  display: grid;
  gap: 18px;
}

.cms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
}

.cms-header p {
  margin: 8px 0 0;
  color: var(--admin-muted);
  font-size: 14px;
}

.header-actions {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}

.cms-grid {
  display: grid;
  align-items: start;
  gap: 14px;
}

.version-layout {
  grid-template-columns: minmax(260px, 0.72fr) minmax(0, 1.28fr);
}

.version-row {
  display: grid;
  width: 100%;
  gap: 5px;
  padding: 12px;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  background: #fff;
  color: #243044;
  text-align: left;
  cursor: pointer;
}

.version-row.active {
  border-color: var(--admin-primary);
  background: rgba(49, 91, 255, 0.08);
}

.version-row span,
.version-row small {
  color: var(--admin-muted);
}

.version-detail {
  min-width: 0;
}

.detail-stack {
  display: grid;
  gap: 14px;
}

.detail-stack pre {
  min-height: 360px;
  max-height: 620px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  border: 1px solid var(--admin-line);
  border-radius: 8px;
  padding: 14px;
  background: #f8fafc;
  color: #243044;
  line-height: 1.7;
}

.empty-hint {
  color: var(--admin-muted);
  font-size: 13px;
}

.inline-notice {
  margin: 0;
  color: var(--admin-primary-strong);
  font-size: 14px;
  font-weight: 760;
}

@media (max-width: 1020px) {
  .cms-header,
  .version-layout {
    grid-template-columns: 1fr;
  }

  .cms-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
