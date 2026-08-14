import { GoogleGenAI } from '@google/genai'
import { config } from './config.js'

if (!config.vertex.project || !config.vertex.credentials) {
  console.warn('[vertex] Missing project or credentials. AI calls will fail.')
}

export const ai = new GoogleGenAI({
  vertexai: true,
  project: config.vertex.project,
  location: config.vertex.location,
  googleAuthOptions: config.vertex.credentials
    ? { credentials: config.vertex.credentials, scopes: ['https://www.googleapis.com/auth/cloud-platform'] }
    : undefined,
})

export function getSystemPrompt() {
  return config.systemPrompt.replace('{date}', new Date().toLocaleDateString('en-US'))
}
