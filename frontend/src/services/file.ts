import { requestJson } from './http'

export interface FileResource {
  id: number
  fileName: string
  originalName: string
  relativePath: string
  publicUrl: string
  fileType: string
  fileSize: number
  storageType: string
  module: string
  createdAt: string
}

export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export async function uploadFile(file: File, module: string, isAdmin = false): Promise<FileResource> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('module', module)

  const endpoint = isAdmin ? '/api/admin/files/upload' : '/api/creator/files/upload'
  
  const response = await requestJson<ApiEnvelope<FileResource>>(endpoint, {
    method: 'POST',
    body: formData,
  })

  return response.data
}
