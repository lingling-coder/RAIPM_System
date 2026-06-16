import { describe, it, expect } from 'vitest'

describe('AchievementRegister.vue', () => {
  it('module can be imported', async () => {
    const module = await import('@/views/achievement/AchievementRegister.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('PaperForm component can be imported', async () => {
    const module = await import('@/components/achievement/PaperForm.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('DoiAutoComplete component can be imported', async () => {
    const module = await import('@/components/achievement/DoiAutoComplete.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('DoiPreviewDialog component can be imported', async () => {
    const module = await import('@/components/achievement/DoiPreviewDialog.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('AttachmentUploader component can be imported', async () => {
    const module = await import('@/components/achievement/AttachmentUploader.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('AchievementList can be imported', async () => {
    const module = await import('@/views/achievement/AchievementList.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('AchievementDetail can be imported', async () => {
    const module = await import('@/views/achievement/AchievementDetail.vue')
    expect(module).toBeDefined()
    expect(module.default).toBeDefined()
  })

  it('paper API module can be imported', async () => {
    const module = await import('@/api/achievement/paper')
    expect(module).toBeDefined()
    expect(module.submit).toBeDefined()
    expect(module.saveDraft).toBeDefined()
    expect(module.lookupDoi).toBeDefined()
    expect(module.getPage).toBeDefined()
  })

  it('attachment API module can be imported', async () => {
    const module = await import('@/api/attachment')
    expect(module).toBeDefined()
    expect(module.upload).toBeDefined()
    expect(module.download).toBeDefined()
  })
})
