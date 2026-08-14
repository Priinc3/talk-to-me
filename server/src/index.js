import express from 'express'
import http from 'node:http'
import { config } from './config.js'
import rest from './rest.js'
import { attachLive } from './live.js'

const app = express()
app.use(express.json({ limit: '2mb' }))
app.use('/', rest)

// ponytail: single shared ws connection on /live; multiple clients get separate sessions per socket
const server = http.createServer(app)
attachLive(server)

server.listen(config.port, () => {
  console.log(`talk-to-me backend listening on :${config.port}`)
  console.log(`  vertex: ${config.vertex.project} / ${config.vertex.location}`)
})
