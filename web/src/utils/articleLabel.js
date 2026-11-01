const LABEL_KEYS = {
  随笔: 'labels.essay',
  日常: 'labels.daily',
  关于: 'labels.about',
  记录: 'labels.notes',
  故事: 'labels.story'
}

export function articleLabel(label, t) {
  if (!label) return t('labels.essay')
  const key = LABEL_KEYS[label]
  return key ? t(key) : label
}
