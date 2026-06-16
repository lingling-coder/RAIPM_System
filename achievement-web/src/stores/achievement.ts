import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PaperFormDTO } from '@/api/achievement/paper'
import * as paperApi from '@/api/achievement/paper'

export interface DraftItem {
  id: number
  title: string
  updatedTime?: string
}

/**
 * Achievement registration state store.
 * Manages form type switching, form data, draft list, and draft ID tracking.
 */
export const useAchievementStore = defineStore('achievement', () => {
  // ── State ──────────────────────────────────────────────────────────
  const currentType = ref<'paper' | 'patent' | 'copyright'>('paper')
  const formData = ref<PaperFormDTO | null>(null)
  const drafts = ref<DraftItem[]>([])
  const currentDraftId = ref<number | null>(null)

  // ── Actions ────────────────────────────────────────────────────────

  /**
   * Switch achievement type (resets form data per D-01).
   * Returns true if the switch proceeded, false if cancelled.
   */
  function switchType(type: 'paper' | 'patent' | 'copyright'): boolean {
    if (currentType.value !== type) {
      currentType.value = type
      resetForm()
      return true
    }
    return false
  }

  /**
   * Load a draft into the form.
   */
  async function loadDraft(id: number) {
    try {
      const res: any = await paperApi.getDraftById(id)
      if (res?.data) {
        formData.value = res.data as PaperFormDTO
        currentDraftId.value = id
      }
    } catch (e) {
      console.error('Failed to load draft:', e)
    }
  }

  /**
   * Save current form as draft.
   */
  async function saveDraftAction() {
    if (!formData.value) return
    const res: any = await paperApi.saveDraft(formData.value)
    if (res?.data) {
      currentDraftId.value = res.data
    }
  }

  /**
   * Reset form data to empty state.
   */
  function resetForm() {
    formData.value = {
      title: '',
      authors: '',
      journal: '',
      doi: '',
      issn: '',
      volume: undefined,
      issue: undefined,
      pages: '',
      publishYear: new Date().getFullYear(),
      indexStatus: '',
      impactFactor: undefined,
      zone: '',
      abstractText: '',
      isClassified: 0,
      classifiedLevel: '',
      projectRef: '',
    }
    currentDraftId.value = null
  }

  /**
   * Set form data (e.g., from DOI preview confirmation).
   */
  function setFormData(data: Partial<PaperFormDTO>) {
    if (formData.value) {
      formData.value = { ...formData.value, ...data }
    }
  }

  return {
    currentType,
    formData,
    drafts,
    currentDraftId,
    switchType,
    loadDraft,
    saveDraft: saveDraftAction,
    resetForm,
    setFormData,
  }
})
