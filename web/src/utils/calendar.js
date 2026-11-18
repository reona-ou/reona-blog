import IcalExpander from 'ical-expander'

const DAY_MS = 24 * 60 * 60 * 1000
const MAX_DAY_SPAN = 366

export function createCalendarExpander(ics) {
  return new IcalExpander({ ics, maxIterations: 5000 })
}

export function expandCalendarRange(expander, start, end) {
  const result = expander.between(start, end)
  const records = [
    ...result.events.map(event => normalizeRecord(event, false)),
    ...result.occurrences.map(occurrence => normalizeRecord(occurrence, true))
  ]
  const seen = new Set()
  return records
    .filter(Boolean)
    .filter(record => {
      if (seen.has(record.id)) return false
      seen.add(record.id)
      return true
    })
    .sort((a, b) => {
      if (a.allDay !== b.allDay) return a.allDay ? -1 : 1
      return a.startMs - b.startMs || a.title.localeCompare(b.title)
    })
}

function normalizeRecord(record, isOccurrence) {
  const item = isOccurrence ? record.item : record
  const start = isOccurrence ? record.startDate : item.startDate
  if (!item || !start) return null

  const end = (isOccurrence ? record.endDate : item.endDate) || start
  const allDay = Boolean(start.isDate)
  const startDate = toLocalDate(start, allDay)
  const endDate = toLocalDate(end, allDay)
  const endMs = Math.max(endDate.getTime(), startDate.getTime() + (allDay ? DAY_MS : 0))
  const safeEnd = new Date(endMs)
  const title = String(item.summary || '').replace(/\s+/g, ' ').trim()
  const location = String(item.location || '').replace(/\s+/g, ' ').trim()
  const uid = item.uid || ''
  const id = `${uid}|${startDate.getTime()}|${title}`

  return {
    id,
    title,
    location,
    allDay,
    startDate,
    endDate: safeEnd,
    startMs: startDate.getTime(),
    endMs,
    days: buildDayKeys(startDate, safeEnd, allDay)
  }
}

function toLocalDate(time, allDay) {
  if (!allDay) return time.toJSDate()
  return new Date(time.year, time.month - 1, time.day)
}

function buildDayKeys(startDate, endDate, allDay) {
  const keys = []
  const cursor = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate())
  const lastDay = allDay
    ? new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate() - 1)
    : new Date(endDate.getTime() - 1)
  const last = new Date(lastDay.getFullYear(), lastDay.getMonth(), lastDay.getDate())
  if (last < cursor) return [dateKey(cursor)]

  while (cursor <= last && keys.length < MAX_DAY_SPAN) {
    keys.push(dateKey(cursor))
    cursor.setDate(cursor.getDate() + 1)
  }
  return keys
}

export function attachCalendarSource(events, source) {
  return events.map(event => ({
    ...event,
    key: `${source.id}|${event.id}`,
    sourceId: source.id,
    sourceName: source.name,
    color: source.color
  }))
}

export function mergeCalendarEvents(groups) {
  return groups
    .flat()
    .sort((a, b) => {
      if (a.allDay !== b.allDay) return a.allDay ? -1 : 1
      return a.startMs - b.startMs || a.title.localeCompare(b.title)
    })
}

export function withAlpha(hex, alpha) {
  const value = String(hex || '').replace('#', '')
  if (!/^[0-9a-f]{6}$/i.test(value)) return `rgba(47, 74, 58, ${alpha})`
  const r = parseInt(value.slice(0, 2), 16)
  const g = parseInt(value.slice(2, 4), 16)
  const b = parseInt(value.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

export function dateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function formatCalendarTime(date, localeTag) {
  return new Intl.DateTimeFormat(localeTag, {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23'
  }).format(date)
}

export function formatCalendarShortDate(date, localeTag) {
  return new Intl.DateTimeFormat(localeTag, {
    month: 'numeric',
    day: 'numeric'
  }).format(date)
}

export function formatCalendarEventTime(event, localeTag, allDayLabel) {
  if (event.allDay) {
    const lastDay = new Date(
      event.endDate.getFullYear(),
      event.endDate.getMonth(),
      event.endDate.getDate() - 1
    )
    if (dateKey(event.startDate) === dateKey(lastDay)) {
      return allDayLabel
    }
    return `${formatCalendarShortDate(event.startDate, localeTag)} – ${formatCalendarShortDate(lastDay, localeTag)} · ${allDayLabel}`
  }

  if (event.endMs <= event.startMs) {
    return formatCalendarTime(event.startDate, localeTag)
  }

  if (dateKey(event.startDate) === dateKey(event.endDate)) {
    return `${formatCalendarTime(event.startDate, localeTag)} – ${formatCalendarTime(event.endDate, localeTag)}`
  }

  return `${formatCalendarShortDate(event.startDate, localeTag)} ${formatCalendarTime(event.startDate, localeTag)} – ${formatCalendarShortDate(event.endDate, localeTag)} ${formatCalendarTime(event.endDate, localeTag)}`
}

export function calendarMonthRange(date) {
  const start = new Date(date.getFullYear(), date.getMonth(), 1)
  start.setDate(start.getDate() - 7)
  const end = new Date(date.getFullYear(), date.getMonth() + 1, 0)
  end.setDate(end.getDate() + 8)
  return { start, end }
}
