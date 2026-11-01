<template>
  <div>
    <div class="admin-page-head">
      <div>
        <h2>{{ t('admin.users.title') }}</h2>
        <p v-if="userStore.isSuperAdmin">{{ t('admin.users.subtitleSuper') }}</p>
        <p v-else>{{ t('admin.users.subtitleAdmin') }}</p>
      </div>
      <span class="badge">{{ userStore.isSuperAdmin ? t('admin.shell.roleSuper') : t('admin.shell.roleAdmin') }}</span>
    </div>

    <div class="admin-panel">
      <div class="admin-toolbar user-toolbar">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="t('admin.users.searchPlaceholder')"
          class="user-search"
          @keyup.enter="search"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-if="userStore.isSuperAdmin"
          v-model="roleFilter"
          clearable
          :placeholder="t('admin.users.allRoles')"
          class="user-role-filter"
          @change="search"
        >
          <el-option :label="t('admin.users.roles.super')" value="ROLE_SUPER_ADMIN" />
          <el-option :label="t('admin.users.roles.admin')" value="ROLE_ADMIN" />
          <el-option :label="t('admin.users.roles.user')" value="ROLE_USER" />
        </el-select>
        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>
          {{ t('admin.common.search') }}
        </el-button>
        <el-button @click="resetFilters">{{ t('admin.common.reset') }}</el-button>
        <span class="user-count">{{ t('admin.users.count', { n: total }) }}</span>
      </div>

      <el-table v-loading="loading" :data="users" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column :label="t('admin.users.account')" min-width="190">
          <template #default="{ row }">
            <div class="admin-product-name">{{ row.username }}</div>
            <div class="admin-product-desc">{{ row.nickname || t('admin.users.noNickname') }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="email" :label="t('admin.users.email')" min-width="210" />
        <el-table-column :label="t('admin.users.role')" width="130">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="plain" size="small">
              {{ roleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.users.registeredAt')" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime, true) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.common.actions')" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit(row)" text type="primary" @click="openEdit(row)">{{ t('admin.common.edit') }}</el-button>
            <el-button v-if="canReset(row)" text type="warning" @click="openReset(row)">{{ t('admin.users.resetPassword') }}</el-button>
            <el-button v-if="canChangeRole(row)" text type="warning" @click="toggleRole(row)">
              {{ row.role === 'ROLE_ADMIN' ? t('admin.users.demote') : t('admin.users.promote') }}
            </el-button>
            <el-button v-if="canDelete(row)" text type="danger" @click="remove(row)">{{ t('admin.common.delete') }}</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="user-empty">{{ t('admin.users.empty') }}</span>
        </template>
      </el-table>

      <div v-if="total > pageSize" class="user-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          @current-change="goPage"
        />
      </div>
    </div>

    <el-dialog v-model="editVisible" :title="t('admin.users.editTitle')" width="min(520px, 92vw)" destroy-on-close>
      <div class="user-form-grid">
        <label class="user-form-field span-2">
          <span>{{ t('admin.users.username') }}</span>
          <el-input :model-value="editing.username" disabled />
        </label>
        <label class="user-form-field">
          <span>{{ t('admin.users.nickname') }}</span>
          <el-input v-model="editing.nickname" maxlength="32" show-word-limit />
        </label>
        <label class="user-form-field">
          <span>{{ t('admin.users.email') }}</span>
          <el-input v-model="editing.email" maxlength="128" />
        </label>
        <label class="user-form-field">
          <span>{{ t('admin.users.phone') }}</span>
          <el-input v-model="editing.phone" maxlength="11" />
        </label>
        <label class="user-form-field">
          <span>{{ t('admin.users.address') }}</span>
          <el-input v-model="editing.address" maxlength="255" />
        </label>
      </div>
      <template #footer>
        <el-button @click="editVisible = false">{{ t('admin.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">
          <el-icon><Check /></el-icon>
          {{ t('admin.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" :title="t('admin.users.resetTitle')" width="min(460px, 92vw)" destroy-on-close>
      <p class="user-reset-hint">{{ t('admin.users.resetHint', { name: resetTarget.username }) }}</p>
      <div class="user-form-grid">
        <label class="user-form-field span-2">
          <span>{{ t('admin.users.newPassword') }}</span>
          <el-input v-model="resetForm.password" type="password" show-password maxlength="72" />
        </label>
        <label class="user-form-field span-2">
          <span>{{ t('admin.users.confirmPassword') }}</span>
          <el-input v-model="resetForm.confirm" type="password" show-password maxlength="72" />
        </label>
      </div>
      <template #footer>
        <el-button @click="resetVisible = false">{{ t('admin.common.cancel') }}</el-button>
        <el-button type="primary" :loading="resetting" @click="saveReset">
          {{ t('admin.users.resetPassword') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteManagedUser,
  listManagedUsers,
  resetManagedUserPassword,
  updateManagedUser,
  updateManagedUserRole
} from '@/api/users'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const userStore = useUserStore()
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')
const roleFilter = ref('')
const loading = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const resetVisible = ref(false)
const resetting = ref(false)
const resetTarget = reactive({ id: null, username: '' })
const resetForm = reactive({ password: '', confirm: '' })

const editing = reactive({
  id: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  address: ''
})

function roleText(role) {
  if (role === 'ROLE_SUPER_ADMIN') return t('admin.users.roles.super')
  if (role === 'ROLE_ADMIN') return t('admin.users.roles.admin')
  return t('admin.users.roles.user')
}

function roleTagType(role) {
  if (role === 'ROLE_SUPER_ADMIN') return 'danger'
  if (role === 'ROLE_ADMIN') return 'warning'
  return 'info'
}

function isSelf(row) {
  return row.id === userStore.user?.id
}

function canEdit(row) {
  return isSelf(row) || row.role !== 'ROLE_SUPER_ADMIN'
}

function canChangeRole(row) {
  return userStore.isSuperAdmin && !isSelf(row) && row.role !== 'ROLE_SUPER_ADMIN'
}

function canReset(row) {
  return !isSelf(row) && row.role !== 'ROLE_SUPER_ADMIN'
}

function canDelete(row) {
  return !isSelf(row) && row.role !== 'ROLE_SUPER_ADMIN' && userStore.isAdmin
}

async function load() {
  loading.value = true
  try {
    const data = await listManagedUsers({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value.trim(),
      role: userStore.isSuperAdmin ? roleFilter.value : ''
    })
    users.value = data?.records || []
    total.value = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

function resetFilters() {
  keyword.value = ''
  roleFilter.value = ''
  search()
}

function goPage(page) {
  pageNum.value = page
  load()
}

function openEdit(row) {
  Object.assign(editing, {
    id: row.id,
    username: row.username,
    nickname: row.nickname || '',
    email: row.email || '',
    phone: row.phone || '',
    address: row.address || ''
  })
  editVisible.value = true
}

async function saveEdit() {
  const nickname = editing.nickname.trim()
  const email = editing.email.trim()
  if (!nickname) {
    ElMessage.warning(t('admin.users.nicknameRequired'))
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning(t('admin.users.emailInvalid'))
    return
  }

  saving.value = true
  try {
    await updateManagedUser(editing.id, {
      nickname,
      email,
      phone: editing.phone.trim(),
      address: editing.address.trim()
    })
    ElMessage.success(t('admin.users.updated'))
    editVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function openReset(row) {
  resetTarget.id = row.id
  resetTarget.username = row.username
  resetForm.password = ''
  resetForm.confirm = ''
  resetVisible.value = true
}

async function saveReset() {
  if (resetForm.password.length < 6) {
    ElMessage.warning(t('admin.users.passwordShort'))
    return
  }
  if (resetForm.password !== resetForm.confirm) {
    ElMessage.warning(t('admin.users.passwordMismatch'))
    return
  }
  resetting.value = true
  try {
    await resetManagedUserPassword(resetTarget.id, resetForm.password)
    ElMessage.success(t('admin.users.resetSuccess', { name: resetTarget.username }))
    resetVisible.value = false
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    resetting.value = false
  }
}

async function toggleRole(row) {
  const nextRole = row.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN'
  const message = nextRole === 'ROLE_ADMIN'
    ? t('admin.users.promoteConfirm', { name: row.username })
    : t('admin.users.demoteConfirm', { name: row.username })
  try {
    await ElMessageBox.confirm(
      message,
      t('admin.users.roleChangeTitle'),
      { type: 'warning', confirmButtonText: t('admin.common.confirm'), cancelButtonText: t('admin.common.cancel') }
    )
    await updateManagedUserRole(row.id, nextRole)
    ElMessage.success(t('admin.users.roleChanged'))
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || t('admin.common.operationFailed'))
    }
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      t('admin.users.deleteConfirm', { name: row.username }),
      t('admin.users.deleteTitle'),
      { type: 'warning', confirmButtonText: t('admin.common.delete'), cancelButtonText: t('admin.common.cancel') }
    )
    await deleteManagedUser(row.id)
    ElMessage.success(t('admin.users.deleted'))
    if (users.value.length === 1 && pageNum.value > 1) pageNum.value -= 1
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || t('admin.common.operationFailed'))
    }
  }
}

onMounted(load)
</script>

<style scoped>
.user-toolbar {
  align-items: center;
}

.user-search {
  width: 280px;
}

.user-role-filter {
  width: 150px;
}

.user-count {
  margin-left: auto;
  color: #9a8f80;
  font-size: 12px;
}

.user-reset-hint {
  margin: 0 0 16px;
  color: #7d7264;
  font-size: 12px;
  line-height: 1.7;
}

.user-empty {
  color: #9a8f80;
  font-size: 13px;
}

.user-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 18px;
}

.user-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.user-form-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #6f6558;
  font-size: 12px;
}

.user-form-field.span-2 {
  grid-column: span 2;
}

@media (max-width: 760px) {
  .user-search,
  .user-role-filter {
    width: 100%;
  }

  .user-count {
    width: 100%;
    margin-left: 0;
  }

  .user-form-grid {
    grid-template-columns: 1fr;
  }

  .user-form-field.span-2 {
    grid-column: span 1;
  }
}
</style>
