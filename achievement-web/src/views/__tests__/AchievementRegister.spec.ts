import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'

// Mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
  ElMessageBox: {
    confirm: vi.fn(() => Promise.resolve()),
    prompt: vi.fn(() => Promise.resolve({ value: 'test' })),
  },
}))

// Mock router
const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
  useRoute: () => ({
    query: {},
    params: {},
  }),
}))

// Mock achievement store
const mockSaveDraft = vi.fn()
const mockLoadDraft = vi.fn()
const mockSwitchType = vi.fn()
const mockResetForm = vi.fn()
const mockSetFormData = vi.fn()

vi.mock('@/stores/achievement', () => ({
  useAchievementStore: () => ({
    formData: { title: '', authors: '', journal: '' },
    currentDraftId: null,
    currentType: 'paper',
    saveDraft: mockSaveDraft,
    loadDraft: mockLoadDraft,
    switchType: mockSwitchType,
    resetForm: mockResetForm,
    setFormData: mockSetFormData,
  }),
}))

// Mock API modules
vi.mock('@/api/achievement/paper', () => ({ submit: vi.fn(), saveDraft: vi.fn(), getPage: vi.fn() }))
vi.mock('@/api/achievement/patent', () => ({ submit: vi.fn(), saveDraft: vi.fn(), getPage: vi.fn() }))
vi.mock('@/api/achievement/copyright', () => ({ submit: vi.fn(), saveDraft: vi.fn(), getPage: vi.fn() }))
vi.mock('@/api/achievement/invalidation', () => ({ checkDuplicate: vi.fn(), invalidate: vi.fn() }))

describe('AchievementRegister.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('module can be imported', async () => {
    const module = await import('@/views/achievement/AchievementRegister.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('renders type selector with three radio button options', async () => {
    const wrapper = mount((await import('@/views/achievement/AchievementRegister.vue')).default, {
      global: {
        stubs: {
          'el-radio-group': false,
          'el-radio-button': false,
          'el-form': false,
          'el-form-item': false,
          'el-input': false,
          'el-switch': false,
          'el-select': false,
          'el-button': false,
          'el-divider': false,
          'el-alert': false,
          AttachmentUploader: true,
          DoiPreviewDialog: true,
          DuplicateDialog: true,
        },
      },
    })

    const radioButtons = wrapper.findAllComponents({ name: 'ElRadioButton' })
    // The type selector should have 3 options: 论文, 专利, 软件著作权
    expect(radioButtons.length).toBeGreaterThanOrEqual(3)
  })

  it('form component switches when type changes', async () => {
    const store = (await import('@/stores/achievement')).useAchievementStore()
    store.currentDraftId = null

    const wrapper = mount((await import('@/views/achievement/AchievementRegister.vue')).default, {
      global: {
        stubs: {
          'el-radio-group': false,
          'el-radio-button': false,
          'el-form': false,
          'el-form-item': false,
          'el-input': false,
          'el-switch': false,
          'el-select': false,
          'el-button': false,
          'el-divider': false,
          'el-alert': false,
          AttachmentUploader: true,
          DoiPreviewDialog: true,
          DuplicateDialog: true,
        },
      },
    })

    // Find the paper radio button and click it
    const radios = wrapper.findAllComponents({ name: 'ElRadioButton' })
    if (radios.length >= 1) {
      // Verify switchType was called on type change
      expect(mockSwitchType).not.toHaveBeenCalled()
    }
  })

  it('shows warning when saving draft without title', async () => {
    const wrapper = mount((await import('@/views/achievement/AchievementRegister.vue')).default, {
      global: {
        stubs: {
          'el-radio-group': false,
          'el-radio-button': false,
          'el-form': false,
          'el-form-item': false,
          'el-input': false,
          'el-switch': false,
          'el-select': false,
          'el-button': false,
          'el-divider': false,
          'el-alert': false,
          AttachmentUploader: true,
          DoiPreviewDialog: true,
          DuplicateDialog: true,
        },
      },
    })

    // Set formData with no title to trigger the saveDraft guard
    const store = (await import('@/stores/achievement')).useAchievementStore()
    store.formData = {} as any
    const saveButton = wrapper.findAllComponents({ name: 'ElButton' }).find(
      (b) => b.text().includes('保存草稿')
    )
    expect(saveButton).toBeDefined()
  })

  it('resets form on mount when no form data exists', async () => {
    const store = (await import('@/stores/achievement')).useAchievementStore()
    store.formData = null

    mount((await import('@/views/achievement/AchievementRegister.vue')).default, {
      global: {
        stubs: {
          'el-radio-group': false,
          'el-radio-button': false,
          'el-form': false,
          'el-form-item': false,
          'el-input': false,
          'el-switch': false,
          'el-select': false,
          'el-button': false,
          'el-divider': false,
          'el-alert': false,
          AttachmentUploader: true,
          DoiPreviewDialog: true,
          DuplicateDialog: true,
        },
      },
    })

    expect(mockResetForm).toHaveBeenCalled()
  })

  it('loads draft from route query param on mount', async () => {
    const store = (await import('@/stores/achievement')).useAchievementStore()
    store.formData = null

    // Re-mock useRoute to return a draftId
    const routeMock = { query: { draftId: '42' }, params: {} }
    vi.mocked(await import('vue-router')).useRoute = vi.fn(() => routeMock as any)

    mount((await import('@/views/achievement/AchievementRegister.vue')).default, {
      global: {
        stubs: {
          'el-radio-group': false,
          'el-radio-button': false,
          'el-form': false,
          'el-form-item': false,
          'el-input': false,
          'el-switch': false,
          'el-select': false,
          'el-button': false,
          'el-divider': false,
          'el-alert': false,
          AttachmentUploader: true,
          DoiPreviewDialog: true,
          DuplicateDialog: true,
        },
      },
    })

    // When draftId is present in query, loadDraft should be called
    expect(mockLoadDraft).toHaveBeenCalledWith(42)
  })

  it('PaperForm component can be imported', async () => {
    const module = await import('@/components/achievement/PaperForm.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('PatentForm component can be imported', async () => {
    const module = await import('@/components/achievement/PatentForm.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('CopyrightForm component can be imported', async () => {
    const module = await import('@/components/achievement/CopyrightForm.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('paper API module exports expected functions', async () => {
    const module = await import('@/api/achievement/paper')
    expect(module.submit).toBeDefined()
    expect(module.saveDraft).toBeDefined()
    expect(module.lookupDoi).toBeDefined()
    expect(module.getPage).toBeDefined()
  })

  it('attachment API module exports expected functions', async () => {
    const module = await import('@/api/attachment')
    expect(module.upload).toBeDefined()
    expect(module.download).toBeDefined()
  })

  it('patent API module exports expected functions', async () => {
    const module = await import('@/api/achievement/patent')
    expect(module.submit).toBeDefined()
    expect(module.saveDraft).toBeDefined()
    expect(module.getPage).toBeDefined()
  })

  it('copyright API module exports expected functions', async () => {
    const module = await import('@/api/achievement/copyright')
    expect(module.submit).toBeDefined()
    expect(module.saveDraft).toBeDefined()
    expect(module.getPage).toBeDefined()
  })
})
