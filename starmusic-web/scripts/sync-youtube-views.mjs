import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../public/assets/api/', import.meta.url));
const files = [];
const videos = new Map();
function youtubeId(video) {
  return video.videoUrl?.match(/(?:embed\/|v=|youtu\.be\/)([\w-]{11})/)?.[1]
    ?? video.cover?.match(/\/vi\/([\w-]{11})\//)?.[1];
}
function visit(value, callback) {
  if (!value || typeof value !== 'object') return;
  if (typeof value.views === 'number' && 'videoUrl' in value) callback(value);
  Object.values(value).forEach((child) => visit(child, callback));
}
async function readFiles(dir) {
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    const name = path.join(dir, entry.name);
    if (entry.isDirectory()) await readFiles(name);
    else if (name.endsWith('.json')) {
      const original = await fs.readFile(name, 'utf8');
      const data = JSON.parse(original);
      files.push({ name, original, data });
      visit(data, (video) => {
        const id = youtubeId(video);
        if (id) videos.set(id, video.title);
      });
    }
  }
}

await readFiles(root);
const counts = new Map();
for (const [id, title] of videos) {
  try {
    const response = await fetch(`https://www.youtube.com/watch?v=${id}`, {
      signal: AbortSignal.timeout(30000)
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const html = await response.text();
    // Read the video's player metadata rather than recommendation counts.
    const start = html.indexOf('var ytInitialPlayerResponse = ');
    if (start < 0) throw new Error('Player metadata unavailable');
    const source = html.slice(start + 'var ytInitialPlayerResponse = '.length);
    let depth = 0, quoted = false, escaped = false, end = -1;
    for (let i = 0; i < source.length; i++) {
      const c = source[i];
      if (quoted) {
        if (escaped) escaped = false;
        else if (c === '\\') escaped = true;
        else if (c === '"') quoted = false;
      } else if (c === '"') quoted = true;
      else if (c === '{') depth++;
      else if (c === '}' && --depth === 0) { end = i + 1; break; }
    }
    if (end < 0) throw new Error('Incomplete metadata');
    const details = JSON.parse(source.slice(0, end)).videoDetails;
    const count = Number(details?.viewCount);
    if (details?.videoId !== id || !/^\d+$/.test(details?.viewCount ?? '') || !Number.isSafeInteger(count)) {
      throw new Error('Valid view count unavailable');
    }
    counts.set(id, count);
    console.log(`${title}: ${count}`);
  } catch (error) {
    console.error(`${title}: retained previous count (${error.message})`);
    process.exitCode = 1;
  }
}

let updated = 0;
for (const file of files) {
  const before = JSON.stringify(file.data);
  visit(file.data, (video) => {
    const count = counts.get(youtubeId(video));
    if (count !== undefined) video.views = count;
  });
  if (path.relative(root, file.name) === path.join('videos', 'ranking.json')) {
    file.data.sort((a, b) => b.views - a.views);
  }
  if (Array.isArray(file.data.ranking)) file.data.ranking.sort((a, b) => b.views - a.views);
  if (JSON.stringify(file.data) !== before) {
    const pretty = file.original.trim().includes('\n');
    await fs.writeFile(file.name, JSON.stringify(file.data, null, pretty ? 2 : undefined) + '\n');
    updated++;
  }
}
console.log(`Synced ${counts.size}/${videos.size} videos; updated ${updated} files.`);
