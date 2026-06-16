import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PaperFormDTO } from '@/api/achievement/paper'
import type { PatentFormDTO } from '@/api/achievement/patent'
import type { CopyrightFormDTO } from '@/api/achievement/copyright'
import * as paperApi from '@/api/achievement/paper'
import * as patentApi from '@/api/achievement/patent'
import * as copyrightApi from '@/api/achievement/copyright'

export type AchievementFormData = PaperFormDTO | PatentFormDTO | CopyrightFormDTO

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
  const formData = ref<AchievementFormData | null>(null)
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
      const apiMap: Record<string, any> = {
        paper: paperApi.getById,
        patent: patentApi.getById,
        copyright: copyrightApi.getById,
      }
      const getApi = apiMap[currentType.value]
      if (!getApi) return

      const res: any = await getApi(id)
      if (res?.data) {
        formData.value = res.data as AchievementFormData
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

    const apiMap: Record<string, any> = {
      paper: paperApi.saveDraft,
      patent: patentApi.saveDraft,
      copyright: copyrightApi.saveDraft,
    }
    const saveApi = apiMap[currentType.value]
    if (!saveApi) return

    const res: any = await saveApi(formData.value)
    if (res?.data) {
      currentDraftId.value = res.data
    }
  }

  /**
   * Reset form data to empty state based on current type.
   */
  function resetForm() {
    if (currentType.value === 'paper') {
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
      } as PaperFormDTO
    } else if (currentType.value === 'patent') {
      formData.value = {
        patentName: '',
        inventors: '',
        applicationNo: '',
        authorizationNo: '',
        applicationDate: '',
        authorizationDate: '',
        patentType: '',
        country: '中国',
        nextFeeDate: '',
        legalStatus: '',
        isClassified: 0,
        classifiedLevel: '',
        projectRef: '',
      } as PatentFormDTO
    } else if (currentType.value === 'copyright') {
      formData.value = {
        name: '',
        copyrightHolder: '',
        registrationNo: '',
        registrationDate: '',
        softwareVersion: '',
        softwareCategory: '',
        isClassified: 0,
        classifiedLevel: '',
        projectRef: '',
      } as CopyrightFormDTO
    }
    currentDraftId.value = null
  }

  /**
   * Set form data (e.g., from DOI preview confirmation).
   */
  function setFormData(data: Partial<AchievementFormData>) {
    if (formData.value) {
      formData.value = { ...formData.value, ...data } as AchievementFormData
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
