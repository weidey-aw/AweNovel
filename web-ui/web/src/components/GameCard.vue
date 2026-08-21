<script setup lang="ts">
import { computed, ref } from 'vue'
import { resolveAssetUrl } from '@gal/shared'
import type { Game } from '@gal/shared'

const props = defineProps<{ game: Game }>()

const imgFailed = ref(false)
const coverUrl = computed(() => resolveAssetUrl(props.game.cover))
</script>

<template>
  <router-link :to="`/game/${game.gameId}`" class="game-card">
    <div class="cover">
      <div v-if="!coverUrl || imgFailed" class="cover-fallback">🎮</div>
      <img
        v-else
        :src="coverUrl"
        :alt="game.title"
        loading="lazy"
        @error="imgFailed = true"
      />
      <span v-if="game.brandName" class="cover-brand">{{ game.brandName }}</span>
      <span v-if="Number(game.ratingAvg) > 0" class="cover-score">
        ★ {{ Number(game.ratingAvg).toFixed(1) }}
      </span>
    </div>
    <div class="info">
      <div class="title" :title="game.titleCn || game.title">{{ game.titleCn || game.title }}</div>
      <div class="meta">
        <span class="date">{{ game.releaseDate ? game.releaseDate.slice(0, 10) : '未知发售日' }}</span>
        <span v-if="game.ratingCount !== undefined" class="count">{{ game.ratingCount }} 人评分</span>
      </div>
      <div v-if="game.tags?.length" class="tags">
        <span v-for="t in game.tags.slice(0, 3)" :key="t.tagId" class="tag">{{ t.name }}</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.game-card {
  display: block;
  border-radius: 12px;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border-glow);
  box-shadow: var(--shadow-card);
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
}

.game-card:hover {
  transform: translateY(-6px);
  border-color: rgba(167, 139, 250, 0.55);
  box-shadow: 0 14px 34px rgba(139, 92, 246, 0.25);
}

.cover {
  position: relative;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: linear-gradient(160deg, #23233a, #14141f);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.game-card:hover .cover img {
  transform: scale(1.06);
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
  opacity: 0.5;
}

.cover-brand {
  position: absolute;
  left: 8px;
  top: 8px;
  background: rgba(8, 8, 16, 0.72);
  backdrop-filter: blur(6px);
  color: #e6e0ff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cover-score {
  position: absolute;
  right: 8px;
  top: 8px;
  background: var(--grad-main);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
}

.info {
  padding: 12px 14px 14px;
}

.title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  color: var(--text-dim);
  font-size: 12px;
}

.tags {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  color: var(--accent);
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.25);
  padding: 2px 8px;
  border-radius: 999px;
}
</style>
