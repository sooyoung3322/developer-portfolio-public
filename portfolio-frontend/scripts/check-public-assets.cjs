const fs = require('node:fs')
const path = require('node:path')
const policy = require('../config/private-asset-policy.json')

function privatePath(relative) {
  return relative.split(/[\\/]/).some(part => {
    const name = part.toLowerCase()
    return policy.blockedNames.includes(name)
      || policy.blockedPrefixes.some(prefix => name.startsWith(prefix))
      || policy.blockedSuffixes.some(suffix => name.endsWith(suffix))
  })
}

function assertPublicAssetsSafe(directory) {
  if (fs.lstatSync(directory).isSymbolicLink() || !fs.statSync(directory).isDirectory()) {
    throw new Error('Static directory missing or linked')
  }
  const visit = current => {
    for (const item of fs.readdirSync(current, { withFileTypes: true })) {
      const filename = path.join(current, item.name)
      const relative = path.relative(directory, filename)
      if (privatePath(relative)) throw new Error(`Private static path forbidden: ${relative}`)
      if (item.isSymbolicLink()) throw new Error(`Static symlink forbidden: ${relative}`)
      if (item.isDirectory()) { visit(filename); continue }
      if (!item.isFile()) throw new Error(`Unsupported static file: ${relative}`)
      const bytes = fs.readFileSync(filename)
      // Archives are not current public downloads. This also catches a renamed DOCX.
      if (policy.blockedContentPrefixesHex.some(hex => bytes.subarray(0, hex.length / 2).equals(Buffer.from(hex, 'hex')))
        || policy.blockedTextMarkers.some(marker => bytes.includes(Buffer.from(marker)))) {
        throw new Error(`Private static content forbidden: ${relative}`)
      }
    }
  }
  visit(directory)
}

module.exports = { assertPublicAssetsSafe }
if (require.main === module) {
  assertPublicAssetsSafe(path.resolve(process.argv[2] || 'dist'))
  console.log('Static asset privacy check passed')
}
