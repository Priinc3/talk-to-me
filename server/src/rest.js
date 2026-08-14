import { Router } from 'express'
import { ai, getSystemPrompt } from './vertex.js'
import { config } from './config.js'

const router = Router()

const PARSE_SCHEMA = `Return ONLY valid JSON with this exact shape:
{
  "summaryText": "brief 1-line summary",
  "spokenConfirmation": "short natural phrase to speak back",
  "todos": [{ "text": "string", "dueDate": "ISO date or null" }],
  "calendarBlocks": [{ "title": "string", "startTimeMillis": 0, "durationMinutes": 30, "location": null }],
  "reminders": [{ "message": "string", "remindAtMillis": 0 }],
  "alarms": [{ "triggerTimeMillis": 0, "spokenMessage": "string" }],
  "meetingNotes": [{ "title": "string", "transcript": "string", "summary": "string" }]
}
Rules:
- Convert relative times ("in 30 minutes", "tomorrow at 3pm") to epoch millis.
- For a meeting, also add a reminder 5 minutes before and a "prepare" todo.
- No markdown, no code fences, no commentary — JSON only.`

router.get('/health', (_req, res) => {
  res.json({ ok: true, service: 'talk-to-me-backend', time: new Date().toISOString() })
})

router.get('/config', (_req, res) => {
  res.json({
    vertex: { project: config.vertex.project, location: config.vertex.location },
    models: config.models,
  })
})

router.post('/parse', async (req, res) => {
  const transcript = req.body?.transcript
  if (!transcript || typeof transcript !== 'string') {
    return res.status(400).json({ error: 'transcript (string) is required' })
  }
  const now = Date.now()
  try {
    const prompt = `${getSystemPrompt()}\n\nCurrent time is ${new Date(now).toISOString()} (epoch ${now}).\nUser said: "${transcript}"\n\n${PARSE_SCHEMA}`
    const result = await ai.models.generateContent({
      model: config.models.text,
      contents: prompt,
      config: { temperature: 0.2 },
    })
    const text = result.candidates?.[0]?.content?.parts
      ?.map((p) => p.text || '')
      .join('') || ''
    const cleaned = text.replace(/```(json)?/g, '').trim()
    let parsed
    try {
      parsed = JSON.parse(cleaned)
    } catch {
      const start = cleaned.indexOf('{')
      const end = cleaned.lastIndexOf('}')
      parsed = JSON.parse(cleaned.slice(start, end + 1))
    }
    parsed.transcript = transcript
    res.json(parsed)
  } catch (err) {
    console.error('[parse] error:', err.message)
    res.status(502).json({ error: err.message })
  }
})

export default router
