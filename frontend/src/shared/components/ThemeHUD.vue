<template>
<!-- 预览模式下方常驻主题 HUD 指示栏与 JSON 面板 -->
  <!-- 将 HUD 状态栏与配置窗口渲染挂载到 body 根元素下 -->
  <Teleport to="body">
    <!-- 主题预览底部指示栏的淡入淡出动画过渡 -->
    <Transition name="hud-fade">
      <!-- 仅在 previewTheme 缓存配置存在时渲染展示 -->
      <div v-if="previewTheme" class="theme-hud" role="status" aria-label="主题预览提示">
        <div class="theme-hud__indicator">
          <!-- 展示当前主题的主配色小圆点 -->
          <span class="theme-hud__dot" :style="{ backgroundColor: previewTheme.primaryColor }" />
          <span>正在预览主题：<strong>{{ previewTheme.displayName || previewTheme.themeName }}</strong></span>
          <!-- 动态条件渲染展示排版间距与动效中文标签 -->
          <span v-if="densityLabel" class="theme-hud__badge">{{ densityLabel }}</span>
          <span v-if="motionLabel" class="theme-hud__badge">{{ motionLabel }}</span>
        </div>

        <div class="theme-hud__actions">
          <!-- 开启查看 JSON 树的弹窗对话框 -->
          <button class="hud-btn" type="button" @click="showJsonModal = true" title="查看主题 JSON 变量">
            <Code :size="14" />
            JSON
          </button>
          <!-- 触发还原全站系统默认主题配置 -->
          <button class="hud-btn hud-btn--accent" type="button" @click="restoreDefault">
            <RotateCcw :size="14" />
            还原默认
          </button>
        </div>
      </div>
    </Transition>

    <!-- 主题 JSON 变量预览弹窗对话框 -->
    <Transition name="modal-fade">
      <!-- 蒙层区域点击隐藏弹窗 -->
      <div v-if="showJsonModal && previewTheme" class="theme-hud-modal" @click.self="showJsonModal = false">
        <div class="theme-hud-modal__content">
          <div class="theme-hud-modal__header">
            <h3>
              <Palette :size="16" />
              主题变量 JSON 深度预览
            </h3>
            <!-- 头部右上角关闭弹窗按钮 -->
            <button class="modal-close" type="button" @click="showJsonModal = false" aria-label="关闭">
              <X :size="18" />
            </button>
          </div>
          
          <div class="theme-hud-modal__body">
            <!-- 树状缩进格式展示代码块 -->
            <pre class="theme-hud-modal__code"><code>{{ formattedJson }}</code></pre>
          </div>

          <div class="theme-hud-modal__footer">
            <!-- 复制操作的即时反馈提示文本 -->
            <span class="toast-tip" :class="{ 'is-show': showCopiedTip }">已复制到剪贴板!</span>
            <!-- 复制完整 JSON 变量对象到剪贴板 -->
            <button class="hud-btn" type="button" @click="copyJson">
              <Copy :size="14" />
              {{ copyBtnText }}
            </button>
            <!-- 关闭弹窗对话框 -->
            <button class="hud-btn hud-btn--accent" type="button" @click="showJsonModal = false">
              关闭窗口
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Code, Copy, Palette, RotateCcw, X } from '@lucide/vue'

import { fetchCurrentTheme } from '../../services/content'
import { applyThemeConfig } from '../theme'

// 预览中的主题变量
const previewTheme = ref<any | null>(null)
// 变量预览弹窗可见状态
const showJsonModal = ref(false)
// 复制成功提示标识
const showCopiedTip = ref(false)
// 复制按钮交互文字
const copyBtnText = ref('复制配置')

// 动态计算并返回当前布局密度所对应的中文标签
// 计算布局密度的中文标签
const densityLabel = computed(() => {
  const density = previewTheme.value?.config?.density
  if (density === 'compact') return '紧凑'
  if (density === 'spacious') return '宽松'
  if (density === 'comfortable') return '舒适'
  return ''
})

// 动态计算并返回当前动效强度所对应的中文标签
// 计算动效程度的中文标签
const motionLabel = computed(() => {
  const motion = previewTheme.value?.config?.motion
  if (motion === 'static') return '无动效'
  if (motion === 'subtle') return '微动效'
  if (motion === 'dynamic') return '灵动'
  return ''
})

// 将预览主题序列化为缩进格式的 JSON 字符串
const formattedJson = computed(() => {
  if (!previewTheme.value) return ''
  return JSON.stringify(previewTheme.value, null, 2)
})

// 检查本地缓存中的预览主题并同步更新组件状态
function checkPreviewTheme() {
  const raw = window.localStorage.getItem('creatorspace_preview_theme')
  if (raw) {
    try {
      previewTheme.value = JSON.parse(raw)
    } catch {
      previewTheme.value = null
    }
  } else {
    previewTheme.value = null
  }
}

// 恢复系统默认主题, 清空本地预览缓存并触发更新事件
async function restoreDefault() {
  window.localStorage.removeItem('creatorspace_preview_theme')
  window.dispatchEvent(new CustomEvent('creatorspace-theme-preview-change'))
  try {
    const theme = await fetchCurrentTheme()
    applyThemeConfig(theme)
  } catch {
    applyThemeConfig(null)
  }
}

// 将 JSON 主题树复制到剪贴板并开启成功状态反馈
function copyJson() {
  if (!formattedJson.value) return
  navigator.clipboard.writeText(formattedJson.value)
    .then(() => {
      showCopiedTip.value = true
      copyBtnText.value = '已复制'
      setTimeout(() => {
        showCopiedTip.value = false
        copyBtnText.value = '复制配置'
      }, 2000)
    })
}

// 挂载组件时初始化预览信息并注册跨组件缓存同步监听器
onMounted(() => {
  checkPreviewTheme()
  window.addEventListener('creatorspace-theme-preview-change', checkPreviewTheme)
})

// 卸载组件前注销监听以防止内存泄漏
onBeforeUnmount(() => {
  window.removeEventListener('creatorspace-theme-preview-change', checkPreviewTheme)
})
</script>

<style scoped>
.theme-hud {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: fit-content;
  max-width: 90vw;
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(11, 15, 26, 0.88);
  backdrop-filter: blur(18px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.36), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  color: #f3f5ff;
  font-size: 13px;
  font-weight: 500;
}

.theme-hud__indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.theme-hud__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 0 8px currentColor;
}

.theme-hud__badge {
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: #9cf2e4;
  font-size: 11px;
}

.theme-hud__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hud-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: #f3f5ff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 180ms ease;
}

.hud-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.2);
}

.hud-btn--accent {
  background: #0b57d0;
  border-color: transparent;
  color: #ffffff;
}

.hud-btn--accent:hover {
  background: #1b67e0;
}

.theme-hud-modal {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(4, 6, 12, 0.62);
  backdrop-filter: blur(12px);
}

.theme-hud-modal__content {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 16px;
  width: min(600px, 100%);
  max-height: 80vh;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: #0f121e;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.54);
}

.theme-hud-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding-bottom: 12px;
}

.theme-hud-modal__header h3 {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: #f3f5ff;
  font-size: 16px;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #8b93a3;
  cursor: pointer;
  transition: all 180ms ease;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #f3f5ff;
}

.theme-hud-modal__body {
  overflow-y: auto;
}

.theme-hud-modal__code {
  margin: 0;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  background: #070911;
  color: #54e6c8;
  font-family: ui-monospace, 'Space Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.theme-hud-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 12px;
}

.toast-tip {
  margin-right: auto;
  color: #54e6c8;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.toast-tip.is-show {
  opacity: 1;
}

.hud-fade-enter-active,
.hud-fade-leave-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.hud-fade-enter-from,
.hud-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 20px);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
