<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-radio-group v-model="query.readFlag" size="small" @change="load">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="0">未读</el-radio-button>
          <el-radio-button label="1">已读</el-radio-button>
        </el-radio-group>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Check" size="small" @click="readAll">全部已读</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Refresh" size="small" @click="load">刷新</el-button>
      </el-col>
      <el-col :span="6" class="unread">
        未读 <b>{{ unread }}</b> 条
      </el-col>
    </el-row>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column label="" width="46" align="center">
        <template #default="{ row }">
          <el-badge v-if="row.readFlag === '0'" is-dot type="danger" />
        </template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="tagType(row.msgType)">{{ typeText(row.msgType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="{ unreadTitle: row.readFlag === '0' }">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="内容" min-width="320" show-overflow-tooltip>
        <template #default="{ row }">{{ row.content }}</template>
      </el-table-column>
      <el-table-column label="时间" width="170" prop="createTime" />
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button v-if="row.readFlag === '0'" link type="primary" size="small" @click="markRead(row)">标为已读</el-button>
          <el-button link type="primary" size="small" @click="go(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && list.length === 0" description="还没有消息" />
  </div>
</template>

<script setup name="ItMessage">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const unread = ref(0)
const query = reactive({ readFlag: '' })

const TYPES = {
  AI_ALERT: { text: 'AI 告警', type: 'danger' },
  TICKET: { text: '新工单', type: 'warning' },
  ASSIGN: { text: '派单', type: 'primary' },
  RESULT: { text: '处理结果', type: 'success' },
  SYSTEM: { text: '系统', type: 'info' }
}

function typeText(t) {
  return (TYPES[t] || TYPES.SYSTEM).text
}
function tagType(t) {
  return (TYPES[t] || TYPES.SYSTEM).type
}

async function load() {
  loading.value = true
  try {
    const res = await request({
      url: '/it/message/list',
      method: 'get',
      params: { readFlag: query.readFlag, limit: 50 }
    })
    list.value = res.data || []
    const u = await request({ url: '/it/message/unread', method: 'get' })
    unread.value = u.data || 0
  } finally {
    loading.value = false
  }
}

async function markRead(row) {
  await request({ url: `/it/message/read/${row.messageId}`, method: 'put' })
  await load()
}

async function readAll() {
  await request({ url: '/it/message/readAll', method: 'put' })
  await load()
}

function go(row) {
  if (row.link) {
    router.push(row.link)
  }
}

onMounted(load)
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
  align-items: center;
}
.unread {
  color: #909399;
  font-size: 13px;
  line-height: 32px;
}
.unreadTitle {
  font-weight: 600;
}
</style>
