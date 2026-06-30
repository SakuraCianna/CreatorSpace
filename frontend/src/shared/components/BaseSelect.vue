<template>
  <div class="base-select" :class="{ 'is-open': isOpen }" ref="selectRef">
    <div class="select-trigger" @click="toggleDropdown">
      <span class="select-label">{{ selectedLabel }}</span>
      <ChevronDown class="select-icon" :size="16" />
    </div>
    
    <div class="select-dropdown" v-show="isOpen">
      <ul class="select-options">
        <li
          v-for="option in options"
          :key="String(option.value)"
          class="select-option"
          :class="{ 'is-selected': option.value === modelValue }"
          @click="selectOption(option)"
        >
          {{ option.label }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ChevronDown } from '@lucide/vue'

export interface SelectOption {
  label: string
  value: string | number | null | undefined
}

const props = defineProps<{
  modelValue: string | number | undefined | null
  options: SelectOption[]
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | null | undefined): void
  (e: 'change', value: string | number | null | undefined): void
}>()

const isOpen = ref(false)
const selectRef = ref<HTMLElement | null>(null)

const selectedLabel = computed(() => {
  const selected = props.options.find(opt => opt.value === props.modelValue)
  return selected ? selected.label : (props.placeholder || '请选择')
})

function toggleDropdown() {
  isOpen.value = !isOpen.value
}

function selectOption(option: SelectOption) {
  emit('update:modelValue', option.value)
  emit('change', option.value)
  isOpen.value = false
}

function handleClickOutside(event: MouseEvent) {
  if (selectRef.value && !selectRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.base-select {
  position: relative;
  display: inline-block;
  min-width: 120px;
  width: 100%;
}

.select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border: 1px solid var(--tone-line-strong);
  border-radius: var(--app-radius-sm, 8px);
  background: color-mix(in srgb, var(--tone-panel-solid) 84%, transparent);
  color: var(--tone-ink);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-time, 180ms) ease;
  min-height: 40px;
}

.select-trigger:hover {
  border-color: color-mix(in srgb, var(--tone-primary) 48%, var(--tone-line-strong));
}

.base-select.is-open .select-trigger {
  border-color: var(--tone-primary);
  box-shadow: 0 0 0 4px rgba(49, 91, 255, 0.08);
}

.select-icon {
  color: var(--tone-muted);
  transition: transform var(--transition-time, 180ms) ease;
}

.base-select.is-open .select-icon {
  transform: rotate(180deg);
}

.select-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  width: 100%;
  max-height: 240px;
  overflow-y: auto;
  z-index: 100;
  background: var(--tone-panel-solid);
  border: 1px solid var(--tone-line-strong);
  border-radius: var(--app-radius-sm, 8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 4px;
}

.select-options {
  list-style: none;
  margin: 0;
  padding: 0;
}

.select-option {
  padding: 8px 12px;
  border-radius: 6px;
  color: var(--tone-ink);
  font-size: 14px;
  cursor: pointer;
  transition: background 150ms ease;
}

.select-option:hover {
  background: rgba(49, 91, 255, 0.08);
}

.select-option.is-selected {
  background: rgba(49, 91, 255, 0.12);
  color: var(--tone-primary);
  font-weight: 600;
}
</style>
