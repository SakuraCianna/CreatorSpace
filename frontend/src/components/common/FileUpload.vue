<template>
  <div class="file-upload" :class="{ 'is-dragging': isDragging, 'is-uploading': isUploading }"
       @dragover.prevent="isDragging = true"
       @dragleave.prevent="isDragging = false"
       @drop.prevent="onDrop">
    
    <input type="file" ref="fileInput" class="hidden-input" @change="onFileSelected" :accept="accept" />
    
    <div class="upload-content" @click="triggerSelect">
      <LoaderCircle v-if="isUploading" class="spin icon text-primary" :size="32" />
      <UploadCloud v-else-if="!modelValue" class="icon text-muted" :size="32" />
      <CheckCircle2 v-else class="icon text-success" :size="32" />
      
      <div class="upload-text">
        <span v-if="isUploading">正在上传...</span>
        <span v-else-if="modelValue" class="text-success-text">上传成功，点击重新选择</span>
        <span v-else>点击或拖拽文件至此上传</span>
      </div>
      <div v-if="hint && !isUploading" class="upload-hint">{{ hint }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UploadCloud, LoaderCircle, CheckCircle2 } from '@lucide/vue'
import { uploadFile } from '../../services/file'
import { useSessionStore } from '../../shared/sessionStore'

const props = defineProps<{
  modelValue?: string
  module: string
  accept?: string
  hint?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'success', url: string): void
  (e: 'error', message: string): void
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)
const isUploading = ref(false)
const sessionStore = useSessionStore()
const isAdmin = sessionStore.isAdmin

function triggerSelect() {
  if (!isUploading.value) {
    fileInput.value?.click()
  }
}

async function handleFile(file: File) {
  if (!file) return
  isUploading.value = true
  try {
    const res = await uploadFile(file, props.module, isAdmin.value)
    emit('update:modelValue', res.publicUrl)
    emit('success', res.publicUrl)
  } catch (err: any) {
    emit('error', err.message || '上传失败')
  } finally {
    isUploading.value = false
    isDragging.value = false
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    const file = target.files[0]
    if (file) {
      handleFile(file)
    }
  }
}

function onDrop(event: DragEvent) {
  isDragging.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0) {
    const file = files[0]
    if (file) {
      handleFile(file)
    }
  }
}
</script>

<style scoped>
.file-upload {
  border: 2px dashed #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafafa;
}

.file-upload:hover {
  border-color: #6366f1;
  background: #f5f3ff;
}

.file-upload.is-dragging {
  border-color: #6366f1;
  background: #e0e7ff;
}

.file-upload.is-uploading {
  cursor: not-allowed;
  opacity: 0.8;
  border-color: #6366f1;
  background: #f5f3ff;
}

.hidden-input {
  display: none;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.icon {
  color: #9ca3af;
}

.text-muted {
  color: #9ca3af;
}

.text-success {
  color: #10b981;
}

.text-primary {
  color: #6366f1;
}

.text-success-text {
  color: #059669;
}

.upload-text {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.upload-hint {
  font-size: 12px;
  color: #6b7280;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
