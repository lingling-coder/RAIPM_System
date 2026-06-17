<template>
  <el-dialog
    v-model="visible"
    title="紧急提醒"
    :modal="false"
    :close-on-click-modal="true"
    :show-close="true"
    width="480px"
    top="15vh"
    modal-class="urgency-modal"
  >
    <div class="urgency-body">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
      >
        <template #title>
          截止日期: {{ currentTask?.deadline }}
          <span v-if="currentTask?.daysUntilDeadline !== undefined" class="deadline-hint">
            （{{ getCountdownText(currentTask.daysUntilDeadline) }}）
          </span>
        </template>
      </el-alert>
      <p class="urgency-content">{{ currentTask?.content }}</p>
    </div>
    <template #footer>
      <el-button type="primary" :loading="dismissing" @click="handleDismiss">
        我知道了
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useReminderStore } from '@/stores/reminder'

const store = useReminderStore()
const visible = ref(false)
const dismissing = ref(false)

/** Show one task at a time — the first in the high-urgency list */
const currentTask = computed(() => store.highUrgencyTasks[0] || null)

/** Auto-show dialog when high-urgency tasks are available */
watch(() => store.highUrgencyTasks.length, (len) => {
  if (len > 0 && currentTask.value) {
    visible.value = true
  }
})

/** Refresh high-urgency tasks on route change (D-20: shows on page switch) */
const router = useRouter()
watch(() => router.currentRoute.value, () => {
  store.fetchHighUrgencyTasks()
})

/** Fetch on mount */
onMounted(() => {
  store.fetchHighUrgencyTasks()
})

/** Dismiss the current task and close the dialog */
async function handleDismiss() {
  if (!currentTask.value) return
  dismissing.value = true
  try {
    await store.dismissTask(currentTask.value.id)
    visible.value = false
  } catch {
    visible.value = false
  } finally {
    dismissing.value = false
  }
}

/** Countdown text helper */
function getCountdownText(days: number): string {
  return days >= 0 ? `${days}天后截止` : `已逾期${Math.abs(days)}天`
}
</script>

<style scoped>
.urgency-body {
  padding: 4px 0;
}
.urgency-content {
  margin: 16px 0 4px;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}
.deadline-hint {
  font-size: 12px;
  color: #f56c6c;
  margin-left: 8px;
}
</style>

<!--
  Non-blocking modal workaround (RESEARCH.md Pitfall 6, UI-SPEC Interaction Contract 4):
  Applying pointer-events: none to the modal overlay allows users to interact with
  page elements behind the dialog. The dialog itself remains interactive through
  Element Plus's default DOM structure (dialog panel has pointer-events: auto).
  Tested across Chrome, Edge, and 360 browser per D-20.
-->
<style>
.urgency-modal {
  pointer-events: none;
}
.urgency-modal .el-overlay-dialog {
  pointer-events: none;
}
.urgency-modal .el-dialog {
  pointer-events: auto;
}
</style>
