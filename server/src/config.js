import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.join(__dirname, '..')

function loadJson(file) {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    return null
  }
}

const keyFile = process.env.VERTEX_KEY_FILE || path.join(ROOT, 'vertex-ai-key.json')
const credentials = process.env.VERTEX_SA_JSON
  ? JSON.parse(process.env.VERTEX_SA_JSON)
  : loadJson(keyFile)

const userConfig = loadJson(process.env.CONFIG_FILE || path.join(ROOT, 'config.json')) || {}

export const config = {
  port: Number(process.env.PORT || 3000),
  vertex: {
    project: process.env.GCP_PROJECT_ID || userConfig.vertex?.project || credentials?.project_id,
    location: process.env.GCP_LOCATION || userConfig.vertex?.location || 'us-central1',
    credentials,
  },
  models: {
    live: userConfig.models?.live || 'gemini-2.0-flash-live-preview-04-09',
    text: userConfig.models?.text || 'gemini-2.5-flash',
  },
  systemPrompt:
    process.env.SYSTEM_PROMPT ||
    userConfig.systemPrompt ||
    'You are Talk to Me, a voice productivity assistant. Parse the user request into structured actions: todos, calendar blocks, reminders, voice alarms, and meeting notes.',
}
