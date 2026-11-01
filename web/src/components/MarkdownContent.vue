<template>
  <article class="markdown-body" v-html="html"></article>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/format'

const props = defineProps({
  content: { type: String, default: '' }
})

const html = computed(() => renderMarkdown(props.content).replace(/<img /g, '<img loading="lazy" '))
</script>

<style scoped>
.markdown-body {
  color: var(--ink-soft);
  font-size: 15px;
  line-height: 1.95;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 1.8em 0 0.65em;
  color: var(--ink);
  font-family: var(--font-display);
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: -0.01em;
}

.markdown-body :deep(h1) {
  font-size: 30px;
}

.markdown-body :deep(h2) {
  font-size: 25px;
}

.markdown-body :deep(h3) {
  font-size: 20px;
}

.markdown-body :deep(h4) {
  font-size: 17px;
}

.markdown-body :deep(p) {
  margin: 0.9em 0;
}

.markdown-body :deep(a) {
  color: var(--accent);
  border-bottom: 1px solid rgba(181, 80, 46, 0.35);
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  margin: 1.4em auto;
  border-radius: var(--radius-md);
}

.markdown-body :deep(blockquote) {
  margin: 1.4em 0;
  padding: 12px 18px;
  border-left: 3px solid var(--gold);
  background: var(--paper);
  color: var(--muted);
}

.markdown-body :deep(blockquote p) {
  margin: 0;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--paper-deep);
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  overflow-x: auto;
  padding: 16px;
  border-radius: 10px;
  background: #23201c;
  color: #f4efe6;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.4em;
}

.markdown-body :deep(li) {
  margin: 0.4em 0;
}

.markdown-body :deep(hr) {
  margin: 2em 0;
  border: 0;
  border-top: 1px solid var(--line);
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1.4em 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 9px 12px;
  border: 1px solid var(--line);
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--paper);
}
</style>
