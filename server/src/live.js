import { WebSocketServer } from 'ws'
import { ai, getSystemPrompt } from './vertex.js'
import { config } from './config.js'

function relayBinaryToAudio(data) {
  return new Blob([data], { type: 'audio/pcm' })
}

function liveConfig() {
  return {
    responseModalities: ['AUDIO'],
    systemInstruction: getSystemPrompt(),
    outputAudioTranscription: { languageCodes: ['en-US'] },
  }
}

export function setupLive(wss) {
  wss.on('connection', (ws) => {
    let session = null
    let closed = false

    const send = (obj) => {
      if (!closed && ws.readyState === ws.OPEN) ws.send(JSON.stringify(obj))
    }

    const forwardServerMessage = (msg) => {
      const serverContent = msg.serverContent
      if (serverContent) {
        // Audio chunks from the model
        for (const part of serverContent.modelTurn?.parts || []) {
          if (part.inlineData?.data) {
            send({ type: 'audio', data: part.inlineData.data })
          }
        }
        if (serverContent.interimInputTranscription?.text) {
          send({ type: 'interim_transcript', text: serverContent.interimInputTranscription.text })
        }
        if (serverContent.inputTranscription?.text) {
          send({ type: 'transcript', text: serverContent.inputTranscription.text })
        }
        if (serverContent.outputTranscription?.text) {
          send({ type: 'output_transcript', text: serverContent.outputTranscription.text })
        }
        if (serverContent.turnComplete) send({ type: 'turn_complete' })
        if (serverContent.interrupted) send({ type: 'interrupted' })
      }
      if (msg.setupComplete) {
        send({ type: 'setup_complete' })
      }
      if (msg.toolCall) {
        send({ type: 'tool_call', toolCall: msg.toolCall })
      }
    }

    const openSession = async () => {
      try {
        session = await ai.live.connect({
          model: config.models.live,
          config: liveConfig(),
          callbacks: {
            onmessage: forwardServerMessage,
            onerror: (e) => {
              console.error('[live] session error:', e?.message, e?.error)
              send({ type: 'error', message: String(e?.message || e) })
            },
            onclose: (e) => {
              console.error('[live] session closed:', e?.code, e?.reason)
              if (!closed) send({ type: 'closed', code: e?.code, reason: String(e?.reason) })
            },
          },
        })
        send({ type: 'connected', model: config.models.live })
      } catch (err) {
        console.error('[live] connect failed:', err.message)
        send({ type: 'error', message: err.message })
      }
    }

    ws.on('message', async (raw, isBinary) => {
      if (isBinary) {
        if (session) session.sendRealtimeInput({ audio: relayBinaryToAudio(raw) })
        return
      }
      let msg
      try {
        msg = JSON.parse(raw.toString())
      } catch {
        return
      }
      switch (msg.type) {
        case 'start':
          if (!session) await openSession()
          break
        case 'audio':
          if (session && msg.data) {
            session.sendRealtimeInput({
              audio: relayBinaryToAudio(Buffer.from(msg.data, 'base64')),
            })
          }
          break
        case 'audio_stream_end':
          if (session) session.sendRealtimeInput({ audioStreamEnd: true })
          break
        case 'text':
          if (session && msg.data) {
            session.sendClientContent({
              turns: [{ role: 'user', parts: [{ text: msg.data }] }],
              turnComplete: true,
            })
          }
          break
        case 'interrupt':
          // ponytail: no client-side barge-in yet; SDK sends activityStart/End automatically
          break
        case 'close':
          session?.close()
          closed = true
          ws.close()
          break
      }
    })

    ws.on('close', () => {
      closed = true
      session?.close()
    })

    ws.on('error', () => {
      closed = true
      session?.close()
    })
  })
}

export function attachLive(server) {
  const wss = new WebSocketServer({ server, path: '/live' })
  setupLive(wss)
  return wss
}
