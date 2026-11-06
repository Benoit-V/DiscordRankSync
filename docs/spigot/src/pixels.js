// Small pixel-art helpers drawn as SVG rects (original art, no game textures).

function seeded(seed) {
  let s = seed;
  return () => {
    s = (s * 1103515245 + 12345) & 0x7fffffff;
    return s / 0x7fffffff;
  };
}

// 8x8 side of a grass block: grass on top with a ragged edge, dirt below.
function grassBlock(size, seed = 7) {
  const rnd = seeded(seed);
  const grass = ["#5fb34a", "#6cc254", "#53a040", "#79cf5f"];
  const dirt = ["#8a5a35", "#7a4e2d", "#996640", "#6d4427"];
  const edge = [2, 3, 2, 2, 3, 2, 3, 2];
  const p = size / 8;
  let rects = "";
  for (let y = 0; y < 8; y++) {
    for (let x = 0; x < 8; x++) {
      const palette = y < edge[x] ? grass : dirt;
      const c = palette[Math.floor(rnd() * palette.length)];
      rects += `<rect x="${x * p}" y="${y * p}" width="${p + .5}" height="${p + .5}" fill="${c}"/>`;
    }
  }
  return `<svg class="pixel-art" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">${rects}</svg>`;
}

// 8x8 generic player face (hair, skin, eyes) - not a real game skin.
function playerHead(size, hair = "#3b2a1e", skin = "#e0a985", eyes = "#4a6fe3") {
  const map = [
    "HHHHHHHH",
    "HHHHHHHH",
    "HSSSSSSH",
    "SSSSSSSS",
    "SWESSEWS",
    "SSSNNSSS",
    "SSMMMMSS",
    "SSSSSSSS",
  ];
  const colors = { H: hair, S: skin, W: "#ffffff", E: eyes, N: "#c98a68", M: "#9c5a45" };
  const p = size / 8;
  let rects = "";
  map.forEach((row, y) => [...row].forEach((k, x) => {
    rects += `<rect x="${x * p}" y="${y * p}" width="${p + .5}" height="${p + .5}" fill="${colors[k]}"/>`;
  }));
  return `<svg class="pixel-art" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">${rects}</svg>`;
}

// Generic chat-bubble glyph (not the Discord logo).
function chatGlyph(size, color = "#ffffff", eyes = "#5865f2") {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none">
    <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v8a2.5 2.5 0 0 1-2.5 2.5H11l-4.5 4v-4h0A2.5 2.5 0 0 1 4 13.5z" fill="${color}"/>
    <circle cx="9" cy="9.5" r="1.4" fill="${eyes}"/><circle cx="15" cy="9.5" r="1.4" fill="${eyes}"/>
  </svg>`;
}

function syncGlyph(size, color = "#ffffff") {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
    <path d="M20 11a8 8 0 0 0-14.3-4.9L4 8"/><path d="M4 3v5h5"/>
    <path d="M4 13a8 8 0 0 0 14.3 4.9L20 16"/><path d="M20 21v-5h-5"/>
  </svg>`;
}

function render(id, html) {
  document.getElementById(id).innerHTML = html;
}
