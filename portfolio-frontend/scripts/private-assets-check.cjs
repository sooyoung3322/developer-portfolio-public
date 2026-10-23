const assert = require('node:assert/strict')
const fs = require('node:fs')
const os = require('node:os')
const path = require('node:path')
const { assertPublicAssetsSafe } = require('./check-public-assets.cjs')

const temporaryRoot = fs.realpathSync(os.tmpdir())
const temporary = fs.mkdtempSync(path.join(temporaryRoot, 'portfolio-static-privacy-'))
try {
  for (const filename of ['resume.docx', 'downloads/RESUME.DOCX', 'files/copy.docx', '.env.production', 'secret.pem', 'secret.key', 'dump.sql.gz', 'backup.sql', 'backup.dump', 'id_rsa']) {
    const file = path.join(temporary, filename)
    fs.mkdirSync(path.dirname(file), { recursive: true })
    fs.writeFileSync(file, 'synthetic private fixture')
    assert.throws(() => assertPublicAssetsSafe(temporary), /Private static path/)
    fs.unlinkSync(file)
  }
  for (const content of [Buffer.from('504b0304', 'hex'), Buffer.from('1f8b', 'hex'), Buffer.from('-----BEGIN PRIVATE KEY-----')]) {
    const file = path.join(temporary, 'renamed-image.jpg')
    fs.writeFileSync(file, content)
    assert.throws(() => assertPublicAssetsSafe(temporary), /Private static content/)
    fs.unlinkSync(file)
  }
  for (const filename of ['image.jpg', 'portfolio.md', 'guide.pdf', 'asset.js']) fs.writeFileSync(path.join(temporary, filename), 'public fixture')
  assert.doesNotThrow(() => assertPublicAssetsSafe(temporary))
  assertPublicAssetsSafe(path.resolve(__dirname, '../public'))
  const dist = path.resolve(__dirname, '../dist')
  if (fs.existsSync(dist)) assertPublicAssetsSafe(dist)
  const scripts = require('../package.json').scripts
  assert.match(scripts.build, /check-public-assets\.cjs public/)
  assert.match(scripts.build, /vite build && node scripts\/check-public-assets\.cjs dist/)
  console.log('Static privacy checks passed: private paths, renamed archives/keys, normal downloads and build guards')
} finally {
  assert.equal(path.dirname(fs.realpathSync(temporary)), temporaryRoot)
  fs.rmSync(temporary, { recursive: true, force: true })
}
