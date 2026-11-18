<template>
  <div>
    <div class="admin-page-head">
      <div>
        <h2>{{ t('admin.calendar.title') }}</h2>
        <p>{{ t('admin.calendar.subtitle') }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        <span>{{ t('admin.calendar.add') }}</span>
      </el-button>
    </div>

    <div v-if="statusLoading" v-loading="true" class="admin-panel calendar-state"></div>

    <div v-else-if="statusError" class="admin-panel calendar-state">
      <el-alert :title="statusError" type="error" show-icon :closable="false" />
      <el-button class="calendar-state-action" @click="init">{{ t('admin.calendar.retry') }}</el-button>
    </div>

    <div v-else-if="!sources.length" class="admin-panel calendar-connect">
      <div class="calendar-connect-head">
        <span class="calendar-connect-mark">
          <el-icon><Calendar /></el-icon>
        </span>
        <div>
          <h3>{{ t('admin.calendar.setupTitle') }}</h3>
          <p>{{ t('admin.calendar.setupIntro') }}</p>
        </div>
      </div>

      <ol class="calendar-steps">
        <li>{{ t('admin.calendar.step1') }}</li>
        <li>{{ t('admin.calendar.step2') }}</li>
        <li>{{ t('admin.calendar.step3') }}</li>
      </ol>

      <div class="calendar-connect-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>{{ t('admin.calendar.addFirst') }}</span>
        </el-button>
      </div>
      <p class="calendar-note">{{ t('admin.calendar.securityNote') }}</p>
    </div>

    <template v-else>
      <div class="admin-panel calendar-toolbar">
        <div class="calendar-status">
          <span class="calendar-dot"></span>
          <div>
            <b>{{ t('admin.calendar.connected', { n: sources.length }) }}</b>
            <small v-if="fetchedLabel">{{ t('admin.calendar.lastFetched', { time: fetchedLabel }) }}</small>
          </div>
        </div>
        <div class="calendar-actions">
          <el-button :loading="loadingFeed" @click="reloadFeed">
            <el-icon><Refresh /></el-icon>
            <span>{{ t('admin.calendar.refresh') }}</span>
          </el-button>
        </div>
      </div>

      <div class="calendar-source-grid">
        <div v-for="source in sources" :key="source.id" class="admin-panel calendar-source-card">
          <span class="calendar-source-bar" :style="{ backgroundColor: source.color }"></span>
          <div class="calendar-source-body">
            <div class="calendar-source-title">
              <b>{{ source.name }}</b>
              <span class="calendar-source-tag" :class="{ 'is-private': !source.visible }">
                {{ source.visible ? t('admin.calendar.publicTag') : t('admin.calendar.privateTag') }}
              </span>
            </div>
            <small>{{ t('admin.calendar.configUpdated', { time: sourceTime(source) }) }}</small>
          </div>
          <div class="calendar-source-actions">
            <el-switch
              :model-value="source.visible"
              size="small"
              :aria-label="t('admin.calendar.visibleLabel')"
              @change="toggleVisible(source, $event)"
            />
            <el-button text size="small" :aria-label="t('admin.common.edit')" @click="openEdit(source)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button text size="small" type="danger" :aria-label="t('common.delete')" @click="remove(source)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <el-alert
        v-if="feedError"
        class="calendar-alert"
        :title="feedError"
        :type="feedErrorType"
        show-icon
        :closable="false"
      >
        <template #default>
          <span v-if="feedDetail">{{ feedDetail }}</span>
        </template>
      </el-alert>

      <div v-loading="loadingFeed" class="calendar-grid">
        <div class="admin-panel calendar-panel">
          <el-calendar v-model="selectedDate">
            <template #header>
              <div class="calendar-head">
                <div class="calendar-month">
                  <b>{{ monthTitle }}</b>
                  <small>{{ t('admin.calendar.monthHint') }}</small>
                </div>
                <el-button-group>
                  <el-button @click="shiftMonth(-1)">
                    <el-icon><ArrowLeft /></el-icon>
                  </el-button>
                  <el-button @click="goToday">{{ t('admin.calendar.today') }}</el-button>
                  <el-button @click="shiftMonth(1)">
                    <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </el-button-group>
              </div>
            </template>

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
                    :title="`${event.sourceName} · ${event.title || t('admin.calendar.untitled')}`"
                  >
                    {{ eventCellLabel(event) }}
                  </span>
                  <span v-if="cellMore(data.day)" class="calendar-cell-more">+{{ cellMore(data.day) }}</span>
                </div>
              </div>
            </template>
          </el-calendar>
        </div>

        <div class="admin-panel calendar-agenda">
          <div class="calendar-agenda-head">
            <div>
              <b>{{ selectedDayTitle }}</b>
              <small>{{ t('admin.calendar.eventsCount', { n: selectedDayEvents.length }) }}</small>
            </div>
            <el-button text size="small" @click="goToday">{{ t('admin.calendar.today') }}</el-button>
          </div>

          <div v-if="selectedDayEvents.length" class="calendar-agenda-list">
            <div v-for="event in selectedDayEvents" :key="event.key" class="calendar-agenda-item">
              <span class="calendar-agenda-time">{{ formatEventTime(event) }}</span>
              <div class="calendar-agenda-body">
                <b>{{ event.title || t('admin.calendar.untitled') }}</b>
                <small v-if="event.location">{{ event.location }}</small>
                <span class="calendar-agenda-source">
                  <i :style="{ backgroundColor: event.color }"></i>
                  {{ event.sourceName }}
                </span>
              </div>
            </div>
          </div>
          <p v-else class="calendar-agenda-empty">{{ t('admin.calendar.emptyDay') }}</p>

          <p class="calendar-note">{{ t('admin.calendar.feedHint') }}</p>
        </div>
      </div>
    </template>

    <el-dialog v-model="dialog" :title="editingId ? t('admin.calendar.editTitle') : t('admin.calendar.addTitle')" width="560px">
      <div class="calendar-form">
        <label class="calendar-field">
          <span>{{ t('admin.calendar.nameLabel') }}</span>
          <el-input
            v-model="form.name"
            maxlength="48"
            show-word-limit
            :placeholder="t('admin.calendar.namePlaceholder')"
          />
        </label>

        <label class="calendar-field">
          <span>{{ t('admin.calendar.colorLabel') }}</span>
          <div class="calendar-color-row">
            <el-color-picker v-model="form.color" :predefine="colorPalette" />
            <span class="calendar-color-value">{{ form.color }}</span>
          </div>
        </label>

        <label class="calendar-field">
          <span>{{ t('admin.calendar.urlLabel') }}</span>
          <el-input
            v-model="form.icsUrl"
            type="password"
            show-password
            maxlength="1024"
            :placeholder="t('admin.calendar.urlPlaceholder')"
            @keyup.enter="save"
          />
        </label>
        <p v-if="editingId" class="calendar-note">{{ t('admin.calendar.urlKeepHint') }}</p>

        <div class="calendar-visibility">
          <div>
            <b>{{ t('admin.calendar.visibleLabel') }}</b>
            <small>{{ t('admin.calendar.visibleHint') }}</small>
          </div>
          <el-switch v-model="form.visible" />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ saving ? t('admin.calendar.saving') : t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCalendarSource,
  deleteCalendarSource,
  getCalendarFeed,
  getCalendarSources,
  updateCalendarSource
} from '@/api/calendar'
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

const colorPalette = [
  '#2f4a3a',
  '#b5502e',
  '#3e5a7a',
  '#6b4a63',
  '#8f6b2f',
  '#4f6b5e',
  '#7a4a3a',
  '#5c5a4a'
]

const statusLoading = ref(true)
const statusError = ref('')
const loadingFeed = ref(false)
const saving = ref(false)
const feedError = ref('')
const feedErrorType = ref('error')
const feedDetail = ref('')
const fetchedAt = ref('')
const sources = ref([])
const selectedDate = ref(new Date())
const monthEvents = ref([])
const dialog = ref(false)
const editingId = ref(null)

const form = reactive({
  name: '',
  color: colorPalette[0],
  icsUrl: '',
  visible: true
})

const expanders = shallowRef([])
const failedNames = ref([])

let loadedMonth = ''
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

const eventsByDay = computed(() => {
  const map = {}
  for (const event of monthEvents.value) {
    for (const day of event.days) {
      if (!map[day]) map[day] = []
      map[day].push(event)
    }
  }
  return map
})

const selectedDayEvents = computed(() => eventsByDay.value[dateKey(selectedDate.value)] || [])

async function init() {
  statusLoading.value = true
  statusError.value = ''
  try {
    const list = await getCalendarSources()
    sources.value = Array.isArray(list) ? list : []
    if (sources.value.length) {
      await loadFeed(false)
    }
  } catch (error) {
    statusError.value = error.message || t('admin.calendar.statusFailed')
  } finally {
    statusLoading.value = false
  }
}

async function loadFeed(refresh) {
  if (!sources.value.length) {
    expanders.value = []
    monthEvents.value = []
    fetchedAt.value = ''
    return
  }
  loadingFeed.value = true
  feedError.value = ''
  feedDetail.value = ''
  try {
    const data = await getCalendarFeed(refresh)
    const calendars = Array.isArray(data?.calendars) ? data.calendars : []
    const failed = Array.isArray(data?.failed) ? data.failed : []
    const nextExpanders = []
    const nextFailed = [...failed]
    let latest = ''

    for (const calendar of calendars) {
      try {
        if (!calendar.ics) throw new Error('empty calendar')
        nextExpanders.push({
          source: {
            id: calendar.id,
            name: calendar.name,
            color: calendar.color || '#2f4a3a'
          },
          expander: createCalendarExpander(calendar.ics)
        })
        if (calendar.fetchedAt && calendar.fetchedAt > latest) latest = calendar.fetchedAt
      } catch {
        nextFailed.push({ id: calendar.id, name: calendar.name })
      }
    }

    expanders.value = nextExpanders
    failedNames.value = nextFailed
    fetchedAt.value = latest
    if (failedNames.value.length) {
      feedErrorType.value = 'warning'
      feedError.value = t('admin.calendar.partialFailure', { n: failedNames.value.length })
      feedDetail.value = failedNames.value
        .map(item => item.name || String(item.id))
        .filter(Boolean)
        .join('、')
    }
    loadedMonth = ''
    monthCache.clear()
    monthEvents.value = []
    loadMonth()
  } catch (error) {
    feedErrorType.value = 'error'
    feedError.value = t('admin.calendar.loadFailed')
    feedDetail.value = error.message || ''
  } finally {
    loadingFeed.value = false
  }
}

function loadMonth() {
  if (!expanders.value.length) {
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
  const parseFailures = []
  for (const item of expanders.value) {
    try {
      const events = expandCalendarRange(item.expander, range.start, range.end)
      groups.push(attachCalendarSource(events, item.source))
    } catch {
      parseFailures.push(item.source)
    }
  }
  const merged = mergeCalendarEvents(groups)
  monthCache.set(key, merged)
  monthEvents.value = merged
  if (parseFailures.length) {
    feedErrorType.value = 'warning'
    feedError.value = t('admin.calendar.partialFailure', { n: parseFailures.length })
    feedDetail.value = parseFailures.map(item => item.name).join('、')
  }
}

function reloadFeed() {
  loadFeed(true)
}

function shiftMonth(offset) {
  const date = selectedDate.value
  selectedDate.value = new Date(date.getFullYear(), date.getMonth() + offset, 1)
}

function goToday() {
  selectedDate.value = new Date()
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
  const title = event.title || t('admin.calendar.untitled')
  return event.allDay ? title : `${formatCalendarTime(event.startDate, localeTag.value)} ${title}`
}

function formatEventTime(event) {
  return formatCalendarEventTime(event, localeTag.value, t('admin.calendar.allDay'))
}

function sourceTime(source) {
  if (!source.updateTime) return '—'
  const date = new Date(source.updateTime)
  if (Number.isNaN(date.getTime())) return '—'
  return new Intl.DateTimeFormat(localeTag.value, {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  }).format(date)
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.color = colorPalette[0]
  form.icsUrl = ''
  form.visible = true
}

function openCreate() {
  resetForm()
  dialog.value = true
}

function openEdit(source) {
  editingId.value = source.id
  form.name = source.name || ''
  form.color = source.color || colorPalette[0]
  form.icsUrl = ''
  form.visible = Boolean(source.visible)
  dialog.value = true
}

async function save() {
  const name = form.name.trim()
  const url = form.icsUrl.trim()
  if (!name) {
    ElMessage.warning(t('admin.calendar.nameRequired'))
    return
  }
  if (!editingId.value && !url) {
    ElMessage.warning(t('admin.calendar.sourceRequired'))
    return
  }
  if (url && !/^https:\/\/calendar\.google\.com\/calendar\/ical\//i.test(url)) {
    ElMessage.warning(t('admin.calendar.sourceInvalid'))
    return
  }

  const payload = {
    name,
    color: form.color || colorPalette[0],
    visible: form.visible
  }
  if (url) payload.icsUrl = url

  saving.value = true
  try {
    if (editingId.value) {
      const saved = await updateCalendarSource(editingId.value, payload)
      sources.value = sources.value.map(item => (item.id === saved.id ? saved : item))
      ElMessage.success(t('admin.calendar.updated'))
    } else {
      const saved = await createCalendarSource(payload)
      sources.value = [...sources.value, saved]
      ElMessage.success(t('admin.calendar.created'))
    }
    dialog.value = false
    await loadFeed(true)
  } catch (error) {
    ElMessage.error(error.message || t('admin.calendar.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function toggleVisible(source, visible) {
  try {
    const saved = await updateCalendarSource(source.id, {
      name: source.name,
      color: source.color,
      visible
    })
    sources.value = sources.value.map(item => (item.id === saved.id ? saved : item))
    ElMessage.success(t('admin.calendar.updated'))
  } catch (error) {
    sources.value = [...sources.value]
    ElMessage.error(error.message || t('admin.common.operationFailed'))
  }
}

async function remove(source) {
  try {
    await ElMessageBox.confirm(
      t('admin.calendar.deleteConfirm', { name: source.name }),
      t('admin.calendar.deleteTitle'),
      {
        type: 'warning',
        confirmButtonText: t('admin.common.confirm'),
        cancelButtonText: t('common.cancel')
      }
    )
  } catch {
    return
  }

  try {
    await deleteCalendarSource(source.id)
    sources.value = sources.value.filter(item => item.id !== source.id)
    ElMessage.success(t('admin.calendar.deleted'))
    if (sources.value.length) {
      await loadFeed(false)
    } else {
      expanders.value = []
      monthEvents.value = []
      fetchedAt.value = ''
      feedError.value = ''
      feedDetail.value = ''
    }
  } catch (error) {
    ElMessage.error(error.message || t('admin.common.operationFailed'))
  }
}

watch(selectedDate, loadMonth)

onMounted(init)
</script>

<style scoped>
.calendar-state {
  min-height: 180px;
}

.calendar-state-action {
  margin-top: 16px;
}

.calendar-connect {
  max-width: 760px;
}

.calendar-connect-head {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.calendar-connect-mark {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #23201c;
  color: #f4efe6;
  font-size: 20px;
}

.calendar-connect-head h3 {
  margin: 2px 0 0;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 500;
}

.calendar-connect-head p {
  margin: 6px 0 0;
  color: #8d8274;
  font-size: 13px;
  line-height: 1.7;
}

.calendar-steps {
  margin: 20px 0;
  padding-left: 20px;
  color: #6f6558;
  font-size: 13px;
  line-height: 2;
}

.calendar-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #6f6558;
  font-size: 12px;
}

.calendar-connect-actions {
  margin-top: 16px;
}

.calendar-note {
  margin: 16px 0 0;
  color: #9a8f80;
  font-size: 11px;
  line-height: 1.7;
}

.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 18px;
  margin-bottom: 14px;
}

.calendar-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calendar-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2f7a4f;
  box-shadow: 0 0 0 4px rgba(47, 122, 79, 0.12);
}

.calendar-status b {
  display: block;
  font-size: 13px;
  font-weight: 600;
}

.calendar-status small {
  display: block;
  margin-top: 3px;
  color: #9a8f80;
  font-size: 11px;
}

.calendar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.calendar-source-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.calendar-source-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 12px 14px 18px;
  overflow: hidden;
}

.calendar-source-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.calendar-source-body {
  min-width: 0;
  flex: 1;
}

.calendar-source-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.calendar-source-title b {
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.calendar-source-tag {
  flex: 0 0 auto;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(47, 122, 79, 0.1);
  color: #2f7a4f;
  font-size: 9px;
  letter-spacing: 0.04em;
}

.calendar-source-tag.is-private {
  background: rgba(154, 143, 128, 0.15);
  color: #8d8274;
}

.calendar-source-body small {
  display: block;
  margin-top: 4px;
  color: #9a8f80;
  font-size: 10px;
}

.calendar-source-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 0 0 auto;
}

.calendar-alert {
  margin-bottom: 18px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 18px;
  align-items: start;
}

.calendar-panel {
  padding: 8px 14px 14px;
}

.calendar-panel :deep(.el-calendar) {
  background: transparent;
}

.calendar-panel :deep(.el-calendar__header) {
  padding: 12px 4px 16px;
  border-bottom: 1px solid #eee7dd;
}

.calendar-panel :deep(.el-calendar__body) {
  padding: 8px 0 0;
}

.calendar-panel :deep(.el-calendar-table td) {
  border-color: #eee7dd;
}

.calendar-panel :deep(.el-calendar-table .el-calendar-day) {
  height: 98px;
  padding: 6px 7px;
}

.calendar-panel :deep(.el-calendar-table thead th) {
  padding: 8px 0;
  color: #9a8f80;
  font-weight: 500;
}

.calendar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.calendar-month b {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 500;
}

.calendar-month small {
  display: block;
  margin-top: 3px;
  color: #9a8f80;
  font-size: 11px;
}

.calendar-cell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  color: #6f6558;
}

.calendar-cell.is-outside {
  opacity: 0.42;
}

.calendar-cell-day {
  font-size: 12px;
  line-height: 1.4;
}

.calendar-cell.is-today .calendar-cell-day {
  color: #b5502e;
  font-weight: 700;
}

.calendar-cell-events {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 4px;
  overflow: hidden;
}

.calendar-cell-event {
  display: block;
  padding: 2px 4px;
  border-left: 3px solid transparent;
  border-radius: 4px;
  color: #3d4f43;
  font-size: 10px;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-cell-more {
  padding-left: 4px;
  color: #9a8f80;
  font-size: 10px;
}

.calendar-agenda {
  padding: 18px;
}

.calendar-agenda-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #eee7dd;
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
  color: #9a8f80;
  font-size: 11px;
}

.calendar-agenda-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 0 4px;
}

.calendar-agenda-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.calendar-agenda-time {
  flex: 0 0 94px;
  padding-top: 1px;
  color: #9a8f80;
  font-size: 11px;
  line-height: 1.5;
}

.calendar-agenda-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.calendar-agenda-body b {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
  word-break: break-word;
}

.calendar-agenda-body small {
  color: #9a8f80;
  font-size: 11px;
  line-height: 1.5;
  word-break: break-word;
}

.calendar-agenda-source {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #9a8f80;
  font-size: 10px;
}

.calendar-agenda-source i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.calendar-agenda-empty {
  margin: 22px 0 8px;
  color: #9a8f80;
  font-size: 12px;
}

.calendar-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.calendar-color-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.calendar-color-value {
  color: #9a8f80;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
}

.calendar-visibility {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 14px;
  border-top: 1px solid #eee7dd;
}

.calendar-visibility b {
  display: block;
  color: #3f3a33;
  font-size: 12px;
  font-weight: 600;
}

.calendar-visibility small {
  display: block;
  margin-top: 3px;
  color: #9a8f80;
  font-size: 11px;
}

@media (max-width: 1080px) {
  .calendar-grid {
    grid-template-columns: 1fr;
  }

  .calendar-agenda-time {
    flex-basis: 84px;
  }
}
</style>
