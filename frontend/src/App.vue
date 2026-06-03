<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

type DayEntry = {
  date: string
  day: string
  hours: string
  project: string
}

const apiBase = 'http://localhost:8080'
const today = new Date()
const form = reactive({
  year: today.getFullYear(),
  month: today.getMonth() + 1,
  company: 'OG Consultancy Services GmbH',
  consultantFirstName: 'ANTONIO',
  consultantLastName: 'GRGIC',
  defaultProject: 'MOVAS',
  approverFirstName: '',
  approverLastName: '',
})

const entries = ref<DayEntry[]>([])
const status = ref('')
const isGenerating = ref(false)

const monthLabel = computed(() =>
  new Intl.DateTimeFormat('de-DE', { month: 'long', year: 'numeric' }).format(new Date(form.year, form.month - 1, 1)),
)

const totalHours = computed(() =>
  entries.value.reduce((sum, entry) => sum + parseGermanNumber(entry.hours), 0),
)

function rebuildEntries() {
  const previous = new Map(entries.value.map((entry) => [entry.date, entry]))
  const days = new Date(form.year, form.month, 0).getDate()
  const next: DayEntry[] = []

  for (let day = 1; day <= days; day++) {
    const date = new Date(form.year, form.month - 1, day)
    const iso = formatIsoDate(date)
    const existing = previous.get(iso)
    next.push({
      date: iso,
      day: new Intl.DateTimeFormat('de-DE', { weekday: 'short' }).format(date).replace('.', ''),
      hours: existing?.hours ?? '',
      project: existing?.project ?? '',
    })
  }

  entries.value = next
}

function fillWeekdays() {
  for (const entry of entries.value) {
    const date = new Date(`${entry.date}T00:00:00`)
    const weekday = date.getDay()
    if (weekday !== 0 && weekday !== 6) {
      entry.hours = '8,00'
      entry.project = form.defaultProject
    }
  }
}

function clearMonth() {
  for (const entry of entries.value) {
    entry.hours = ''
    entry.project = ''
  }
}

async function downloadPdf() {
  isGenerating.value = true
  status.value = ''

  try {
    const response = await fetch(`${apiBase}/api/timesheets/pdf`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        ...form,
        entries: entries.value.map((entry) => ({
          date: entry.date,
          hours: parseGermanNumber(entry.hours),
          project: entry.project || form.defaultProject,
        })),
      }),
    })

    if (!response.ok) {
      throw new Error(`PDF konnte nicht erstellt werden (${response.status})`)
    }

    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `timesheet-${form.year}-${String(form.month).padStart(2, '0')}.pdf`
    anchor.click()
    URL.revokeObjectURL(url)
    status.value = 'PDF wurde erstellt.'
  } catch (error) {
    status.value = error instanceof Error ? error.message : 'PDF konnte nicht erstellt werden.'
  } finally {
    isGenerating.value = false
  }
}

function parseGermanNumber(value: string) {
  const normalized = value.trim().replace(',', '.')
  if (!normalized) return 0
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatIsoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDisplayDate(iso: string) {
  const [year, month, day] = iso.split('-')
  return `${day}.${month}.${year}`
}

function isWeekend(iso: string) {
  const day = new Date(`${iso}T00:00:00`).getDay()
  return day === 0 || day === 6
}

watch(() => [form.year, form.month], rebuildEntries, { immediate: true })
</script>

<template>
  <main class="app-shell">
    <section class="toolbar">
      <div>
        <p class="eyebrow">Worktime Tracker</p>
        <h1>{{ monthLabel }}</h1>
      </div>
      <div class="actions">
        <button type="button" class="secondary" @click="fillWeekdays">Werktage füllen</button>
        <button type="button" class="secondary" @click="clearMonth">Leeren</button>
        <button type="button" :disabled="isGenerating" @click="downloadPdf">
          {{ isGenerating ? 'Erstelle PDF...' : 'PDF herunterladen' }}
        </button>
      </div>
    </section>

    <section class="settings">
      <label>
        Jahr
        <input v-model.number="form.year" type="number" min="2000" max="2100" />
      </label>
      <label>
        Monat
        <select v-model.number="form.month">
          <option v-for="month in 12" :key="month" :value="month">{{ month }}</option>
        </select>
      </label>
      <label>
        Firma
        <input v-model="form.company" type="text" />
      </label>
      <label>
        Projekt
        <input v-model="form.defaultProject" type="text" />
      </label>
      <label>
        Vorname
        <input v-model="form.consultantFirstName" type="text" />
      </label>
      <label>
        Nachname
        <input v-model="form.consultantLastName" type="text" />
      </label>
      <label>
        Genehmiger Vorname
        <input v-model="form.approverFirstName" type="text" />
      </label>
      <label>
        Genehmiger Nachname
        <input v-model="form.approverLastName" type="text" />
      </label>
    </section>

    <section class="sheet">
      <div class="sheet-header">
        <strong>Monatsstunden</strong>
        <span>{{ totalHours.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }} h</span>
      </div>
      <div class="table">
        <div class="table-row table-head">
          <span>Datum</span>
          <span>Tag</span>
          <span>Stunden</span>
          <span>Projekt / Auftraggeber</span>
        </div>
        <div v-for="entry in entries" :key="entry.date" class="table-row" :class="{ weekend: isWeekend(entry.date) }">
          <span>{{ formatDisplayDate(entry.date) }}</span>
          <span>{{ entry.day }}</span>
          <input v-model="entry.hours" inputmode="decimal" placeholder="0,00" />
          <input v-model="entry.project" :placeholder="form.defaultProject" />
        </div>
      </div>
    </section>

    <p class="status" role="status">{{ status }}</p>
  </main>
</template>

