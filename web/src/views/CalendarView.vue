<template>
  <div class="calendar-page">
    <section class="container calendar-hero">
      <p class="eyebrow">{{ t('calendar.eyebrow') }}</p>
      <div class="calendar-hero-row">
        <div>
          <h1 class="display">{{ t('calendar.title') }}</h1>
          <p class="calendar-subtitle">{{ t('calendar.subtitle') }}</p>
        </div>
        <div v-if="sources.length" class="calendar-meta">
          <span class="calendar-meta-count">{{ t('calendar.sourcesCount', { n: sources.length }) }}</span>
          <small v-if="fetchedLabel">{{ t('calendar.lastFetched', { time: fetchedLabel }) }}</small>
        </div>
      </div>
    </section>

    <section class="container calendar-body">
      <div v-if="loading" class="calendar-loading">
        <div class="calendar-skeleton"></div>
        <div class="calendar-skeleton"></div>
        <div class="calendar-skeleton"></div>
      </div>

      <div v-else-if="error" class="empty-state calendar-error">
        <strong>{{ t('calendar.loadFailed') }}</strong>
        <p>{{ error }}</p>
        <el-button @click="load(false)">{{ t('calendar.retry') }}</el-button>
      </div>

      <template v-else>
        <div v-if="failed.length && sources.length" class="calendar-notice">
          <el-icon><Warning /></el-icon>
          <span>{{ t('calendar.partialFailure', { n: failed.length }) }}</span>
        </div>

        <div v-if="!sources.length && failed.length" class="empty-state calendar-error">
          <strong>{{ t('calendar.loadFailed') }}</strong>
          <p>{{ t('calendar.partialFailure', { n: failed.length }) }}</p>
          <el-button :loading="refreshing" @click="load(true)">{{ t('calendar.retry') }}</el-button>
        </div>

        <div v-else-if="!sources.length" class="empty-state calendar-empty">
          <strong>{{ t('calendar.emptyTitle') }}</strong>
          <p>{{ t('calendar.emptyNote') }}</p>
        </div>

        <div v-else class="calendar-layout">
          <div class="calendar-main">
            <div class="calendar-toolbar">
              <div class="calendar-month">
                <b>{{ monthTitle }}</b>
                <small>{{ t('calendar.monthHint') }}</small>
              </div>
              <div class="calendar-toolbar-actions">
                <el-button-group>
                  <el-button @click="shiftMonth(-1)">
                    <el-icon><ArrowLeft /></el-icon>
                  </el-button>
                  <el-button @click="goToday">{{ t('calendar.today') }}</el-button>
                  <el-button @click="shiftMonth(1)">
                    <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </el-button-group>
                <el-button text size="small" :loading="refreshing" @click="load(true)">
                  <el-icon><Refresh /></el-icon>
                  <span>{{ t('calendar.refresh') }}</span>
                </el-button>
              </div>
            </div>

            <div class="calendar-filters">
              <button
                v-for="source in sources"
                :key="source.id"
                class="calendar-filter"
                :class="{ 'is-off': !activeSourceIds.has(source.id) }"
                type="button"
                @click="toggleSource(source.id)"
              >
                <span class="calendar-filter-dot" :style="{ backgroundColor: source.color }"></span>
                <span>{{ source.name }}</span>
              </button>
            </div>

            <el-calendar v-model="selectedDate" class="calendar-grid">
              <template #date-cell="{ data }">
                <div
                  class="calendar-cell"
                  :class="{
                    'is-today': isToday(data.day),
                    'is-outside': data.type !== 'current-month'
                  }"
                >
                  <span class="calendar-cell-day">{{ Number(data.day.slice(-2)) }}</span>
                  <div class="calendar-cell-events">
                    <span
                      v-for="event in cellEvents(data.day)"
                      :key="event.key"
                      class="calendar-cell-event"
                      :style="eventStyle(event)"
                      :title="event.title || t('calendar.untitled')"
                    >
                      {{ eventCellLabel(event) }}
                    </span>
                    <span v-if="cellMore(data.day)" class="calendar-cell-more">+{{ cellMore(data.day) }}</span>
                  </div>
                </div>
              </template>
            </el-calendar>
          </div>

          <aside class="calendar-agenda">
            <div class="calendar-agenda-head">
              <div>
                <b>{{ selectedDayTitle }}</b>
                <small>{{ t('calendar.eventsCount', { n: selectedDayEvents.length }) }}</small>
              </div>
              <el-button text size="small" @click="goToday">{{ t('calendar.today') }}</el-button>
            </div>

            <div v-if="selectedDayEvents.length" class="calendar-agenda-list">
              <div v-for="event in selectedDayEvents" :key="event.key" class="calendar-agenda-item">
                <span class="calendar-agenda-time">{{ formatEventTime(event) }}</span>
                <div class="calendar-agenda-body">
                  <b>{{ event.title || t('calendar.untitled') }}</b>
                  <small v-if="event.location">{{ event.location }}</small>
                  <span class="calendar-agenda-source">
                    <i :style="{ backgroundColor: event.color }"></i>
                    {{ event.sourceName }}
                  </span>
                </div>
              </div>
            </div>
            <p v-else class="calendar-agenda-empty">{{ t('calendar.emptyDay') }}</p>

            <p class="calendar-note">{{ t('calendar.feedNote') }}</p>
          </aside>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getPublicCalendarFeed } from '@/api/calendar'
import {
  attachCalendarSource,
  calendarMonthRange,
  createCalendarExpander,
  dateKey,
  expandCalendarRange,
  formatCalendarEventTime,
  formatCalendarTime,
  mergeCalendarEvents,
  withAlpha
} from '@/utils/calendar'

const { t, locale } = useI18n()

const loading = ref(true)
const refreshing = ref(false)
const error = ref('')
const sources = ref([])
const failed = ref([])
const fetchedAt = ref('')
const parsedSources = shallowRef([])
const activeSourceIds = ref(new Set())
const selectedDate = ref(new Date())
const monthEvents = ref([])

let loadedMonth = ''
let loadedOnce = false
const monthCache = new Map()

const localeTag = computed(() => {
  if (locale.value === 'zh') return 'zh-CN'
  if (locale.value === 'ja') return 'ja-JP'
  return 'en-US'
})

const monthTitle = computed(() => (
  new Intl.DateTimeFormat(localeTag.value, { year: 'numeric', month: 'long' }).format(selectedDate.value)
))

const selectedDayTitle = computed(() => (
  new Intl.DateTimeFormat(localeTag.value, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short'
  }).format(selectedDate.value)
))

const fetchedLabel = computed(() => {
  if (!fetchedAt.value) return ''
  const date = new Date(fetchedAt.value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(localeTag.value, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date)
})

const visibleEvents = computed(() => (
  monthEvents.value.filter(event => activeSourceIds.value.has(event.sourceId))
))

const eventsByDay = computed(() => {
  const map = {}
  for (const event of visibleEvents.value) {
    for (const day of event.days) {
      if (!map[day]) map[day] = []
      map[day].push(event)
    }
  }
  return map
})

const selectedDayEvents = computed(() => eventsByDay.value[dateKey(selectedDate.value)] || [])

async function load(refresh) {
  if (refresh) {
    refreshing.value = true
  } else {
    loading.value = true
  }
  error.value = ''
  try {
    const data = await getPublicCalendarFeed(refresh)
    const calendars = Array.isArray(data?.calendars) ? data.calendars : []
    const failures = Array.isArray(data?.failed) ? data.failed : []
    const nextParsed = []
    const nextSources = []
    const parseFailures = []
    let latest = ''

    for (const calendar of calendars) {
      const source = {
        id: calendar.id,
        name: calendar.name,
        color: calendar.color || '#2f4a3a'
      }
      try {
        if (!calendar.ics) throw new Error('empty calendar')
        const expander = createCalendarExpander(calendar.ics)
        nextParsed.push({ source, expander })
        nextSources.push(source)
        if (calendar.fetchedAt && calendar.fetchedAt > latest) latest = calendar.fetchedAt
      } catch {
        parseFailures.push({ id: source.id, name: source.name })
      }
    }

    const previous = activeSourceIds.value
    sources.value = nextSources
    parsedSources.value = nextParsed
    failed.value = [...failures, ...parseFailures]
    fetchedAt.value = latest
    activeSourceIds.value = loadedOnce
      ? new Set(nextSources.map(source => source.id).filter(id => previous.has(id)))
      : new Set(nextSources.map(source => source.id))
    loadedOnce = true
    loadedMonth = ''
    monthCache.clear()
    monthEvents.value = []
    loadMonth()
  } catch (loadError) {
    error.value = loadError.message || t('calendar.loadFailed')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function loadMonth() {
  if (!parsedSources.value.length) {
    monthEvents.value = []
    return
  }
  const date = selectedDate.value
  const key = `${date.getFullYear()}-${date.getMonth()}`
  if (loadedMonth === key) return
  loadedMonth = key

  if (monthCache.has(key)) {
    monthEvents.value = monthCache.get(key)
    return
  }

  const range = calendarMonthRange(date)
  const groups = []
  for (const item of parsedSources.value) {
    try {
      const events = expandCalendarRange(item.expander, range.start, range.end)
      groups.push(attachCalendarSource(events, item.source))
    } catch {
      // Skip one broken source instead of hiding the whole calendar.
    }
  }
  const merged = mergeCalendarEvents(groups)
  monthCache.set(key, merged)
  monthEvents.value = merged
}

function shiftMonth(offset) {
  const date = selectedDate.value
  selectedDate.value = new Date(date.getFullYear(), date.getMonth() + offset, 1)
}

function goToday() {
  selectedDate.value = new Date()
}

function toggleSource(id) {
  const next = new Set(activeSourceIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  activeSourceIds.value = next
}

function isToday(day) {
  return day === dateKey(new Date())
}

function cellEvents(day) {
  return (eventsByDay.value[day] || []).slice(0, 3)
}

function cellMore(day) {
  return Math.max(0, (eventsByDay.value[day] || []).length - 3)
}

function eventStyle(event) {
  return {
    backgroundColor: withAlpha(event.color, 0.12),
    borderLeftColor: event.color
  }
}

function eventCellLabel(event) {
  const title = event.title || t('calendar.untitled')
  return event.allDay ? title : `${formatCalendarTime(event.startDate, localeTag.value)} ${title}`
}

function formatEventTime(event) {
  return formatCalendarEventTime(event, localeTag.value, t('calendar.allDay'))
}

watch(selectedDate, loadMonth)

onMounted(() => load(false))
</script>

<style scoped>
.calendar-page {
  padding: 54px 0 84px;
}

.calendar-hero {
  padding-bottom: 30px;
  border-bottom: 1px solid var(--line-strong);
}

.calendar-hero-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-top: 14px;
}

.calendar-hero h1 {
  margin: 0;
  font-size: clamp(38px, 5vw, 58px);
  line-height: 1.1;
}

.calendar-subtitle {
  max-width: 640px;
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.8;
}

.calendar-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
  flex: 0 0 auto;
  padding-bottom: 4px;
  color: var(--muted);
  font-size: 11px;
}

.calendar-meta-count {
  color: var(--ink-soft);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.calendar-body {
  padding-top: 26px;
}

.calendar-loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.calendar-skeleton {
  height: 180px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--paper-deep), rgba(255, 253, 248, 0.5), var(--paper-deep));
  background-size: 200% 100%;
  opacity: 0.7;
  animation: calendar-shimmer 1.4s infinite linear;
}

@keyframes calendar-shimmer {
  to {
    background-position: -200% 0;
  }
}

.calendar-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  border: 1px solid rgba(181, 80, 46, 0.26);
  border-radius: var(--radius-sm);
  background: rgba(181, 80, 46, 0.06);
  color: var(--accent-dark);
  font-size: 12px;
}

.calendar-error,
.calendar-empty {
  padding: 52px 24px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
}

.calendar-error p,
.calendar-empty p {
  margin: 8px 0 18px;
}

.calendar-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 22px;
  align-items: start;
}

.calendar-main,
.calendar-agenda {
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  background: var(--surface);
  box-shadow: var(--shadow-card);
}

.calendar-main {
  min-width: 0;
  padding: 20px 22px 16px;
}

.calendar-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.calendar-month b {
  display: block;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 500;
  line-height: 1.2;
}

.calendar-month small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.calendar-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.calendar-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 0 4px;
}

.calendar-filter {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 11px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--ink-soft);
  font-size: 11px;
  cursor: pointer;
  transition: opacity 0.18s ease, border-color 0.18s ease;
}

.calendar-filter:hover {
  border-color: var(--line-strong);
}

.calendar-filter.is-off {
  opacity: 0.38;
}

.calendar-filter-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.calendar-grid {
  background: transparent;
}

.calendar-grid :deep(.el-calendar__header) {
  display: none;
}

.calendar-grid :deep(.el-calendar__body) {
  padding: 10px 0 0;
}

.calendar-grid :deep(.el-calendar-table td) {
  border-color: var(--line);
}

.calendar-grid :deep(.el-calendar-table thead th) {
  padding: 8px 0;
  color: var(--muted);
  font-size: 11px;
  font-weight: 500;
}

.calendar-grid :deep(.el-calendar-table .el-calendar-day) {
  height: 106px;
  padding: 7px 8px;
  transition: background 0.18s ease;
}

.calendar-grid :deep(.el-calendar-table .el-calendar-day:hover) {
  background: rgba(35, 32, 28, 0.035);
}

.calendar-cell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}

.calendar-cell.is-outside {
  opacity: 0.38;
}

.calendar-cell-day {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.4;
}

.calendar-cell.is-today .calendar-cell-day {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  margin: -3px 0 0 -4px;
  border-radius: 50%;
  background: var(--ink);
  color: var(--paper);
  font-weight: 600;
}

.calendar-cell-events {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 5px;
  overflow: hidden;
}

.calendar-cell-event {
  display: block;
  padding: 2px 5px;
  border-left: 3px solid transparent;
  border-radius: 3px;
  color: var(--ink-soft);
  font-size: 10px;
  line-height: 1.45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-cell-more {
  padding-left: 6px;
  color: var(--muted);
  font-size: 10px;
}

.calendar-agenda {
  padding: 20px;
}

.calendar-agenda-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
}

.calendar-agenda-head b {
  display: block;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 500;
}

.calendar-agenda-head small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 11px;
}

.calendar-agenda-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 0 4px;
}

.calendar-agenda-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.calendar-agenda-time {
  flex: 0 0 94px;
  padding-top: 1px;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
}

.calendar-agenda-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.calendar-agenda-body b {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.calendar-agenda-body small {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.5;
  word-break: break-word;
}

.calendar-agenda-source {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--muted);
  font-size: 10px;
}

.calendar-agenda-source i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.calendar-agenda-empty {
  margin: 24px 0 8px;
  color: var(--muted);
  font-size: 12px;
}

.calendar-note {
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 10px;
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .calendar-layout {
    grid-template-columns: 1fr;
  }

  .calendar-agenda-time {
    flex-basis: 84px;
  }
}

@media (max-width: 720px) {
  .calendar-page {
    padding-top: 34px;
  }

  .calendar-hero-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .calendar-meta {
    align-items: flex-start;
  }

  .calendar-main {
    padding: 16px 12px 12px;
  }

  .calendar-grid :deep(.el-calendar-table .el-calendar-day) {
    height: 82px;
    padding: 5px 4px;
  }

  .calendar-cell-event {
    font-size: 9px;
    padding: 1px 3px;
  }

  .calendar-toolbar {
    gap: 12px;
  }

  .calendar-toolbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .calendar-agenda {
    padding: 16px;
  }
}
</style>
