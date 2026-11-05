<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="工单编号" prop="ticketNo">
        <el-input
          v-model="queryParams.ticketNo"
          placeholder="请输入工单编号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="影响面" prop="impactScope">
        <el-select v-model="queryParams.impactScope" placeholder="请选择影响面" clearable style="width: 160px">
          <el-option v-for="item in impactOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" prop="ticketType">
        <el-input
          v-model="queryParams.ticketType"
          placeholder="请输入类型"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['it:ticket:add']">创建工单</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Bell" @click="handleScan" v-hasPermi="['it:ticket:handle']">扫描超时</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ticketList">
      <el-table-column label="工单编号" align="center" prop="ticketNo" width="150" />
      <el-table-column label="标题" align="center" prop="title" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="申请人" align="center" prop="applicant" width="100" />
      <el-table-column label="类型" align="center" prop="ticketType" width="100" />
      <el-table-column label="影响面" align="center" prop="impactScope" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.impactScope" :type="impactTagType(scope.row.impactScope)" size="small">
            {{ formatImpact(scope.row.impactScope) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="优先级" align="center" prop="priority" width="90">
        <template #default="scope">{{ formatPriority(scope.row.priority) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag>{{ formatStatus(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理人" align="center" prop="handler" width="100" />
      <el-table-column label="来源" align="center" prop="aiSource" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.aiSource" :type="scope.row.aiSource === 'llm' ? 'success' : 'info'" size="small">
            {{ scope.row.aiSource === 'llm' ? 'AI·大模型' : 'AI·规则' }}
          </el-tag>
          <span v-else>手工</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
          <el-button link type="primary" icon="User" @click="openAction(scope.row, 'assign')" v-hasPermi="['it:ticket:handle']">分配</el-button>
          <el-button link type="primary" icon="Check" @click="openAction(scope.row, 'process')" v-hasPermi="['it:ticket:handle']">完成</el-button>
          <el-button link type="primary" icon="Close" @click="openAction(scope.row, 'reject')" v-hasPermi="['it:ticket:handle']">驳回</el-button>
          <el-button link type="primary" icon="CircleClose" @click="openAction(scope.row, 'close')" v-hasPermi="['it:ticket:close']">关闭</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 创建工单：先点「确定」让 AI 按描述补全结构化信息，确认后再提交 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="ticketRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="一句话说明问题，如：笔记本开不了机" />
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="用口语描述即可，例如：我这台 ThinkPad T14 今天早上按电源键没反应，重启好几次都不行，现在没法干活了"
          />
          <div class="tip-line">
            <span>点「确定」会先按描述自动识别设备/现象/影响面（可修改），再确认提交；不识别也可以直接提交。</span>
            <el-button link type="primary" :loading="aiState.loading" @click="runIntake">立即识别</el-button>
          </div>
        </el-form-item>

        <el-alert
          v-if="aiState.notice"
          :title="aiState.notice"
          :type="aiState.degraded ? 'warning' : 'info'"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
        />

        <template v-if="aiState.reviewed">
          <el-divider content-position="left">AI 已识别（可修改，留空则不保存）</el-divider>
          <el-form-item label="设备/系统">
            <el-input v-model="form.deviceInfo" placeholder="如 ThinkPad T14 / VPN / 邮箱" />
          </el-form-item>
          <el-form-item label="现象">
            <el-input v-model="form.symptom" placeholder="报错原文或现象" />
          </el-form-item>
          <el-form-item label="影响面">
            <el-select v-model="form.impactScope" style="width: 100%" placeholder="请选择影响面">
              <el-option v-for="item in impactOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="已尝试">
            <el-input v-model="form.triedActions" placeholder="如：重启、换线、重装" />
          </el-form-item>
          <el-form-item label="识别来源">
            <el-tag :type="form.aiSource === 'llm' ? 'success' : 'info'" size="small">
              {{ form.aiSource === 'llm' ? 'AI·大模型' : 'AI·规则引擎' }}
            </el-tag>
            <span v-if="aiState.rejected.length" class="tip-line" style="margin-left: 8px">
              已丢弃无原文依据的字段：{{ aiState.rejected.join(', ') }}
            </span>
          </el-form-item>
        </template>

        <el-form-item label="类型" prop="ticketType">
          <el-input v-model="form.ticketType" placeholder="如：设备故障 / 账号权限 / 网络问题 / 软件安装" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%" @change="priorityTouched = true">
            <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="aiState.loading" @click="submitForm">
            {{ aiState.reviewed ? '确认提交' : '确定' }}
          </el-button>
          <el-button v-if="aiState.reviewed" @click="aiState.reviewed = false">重新识别</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="actionTitle" v-model="actionOpen" width="560px" append-to-body>
      <el-form :model="actionForm" label-width="90px">
        <el-form-item v-if="actionType === 'assign'" label="处理人">
          <el-input v-model="actionForm.handler" placeholder="请输入处理人" />
        </el-form-item>
        <template v-else>
          <el-form-item label="处理说明">
            <el-input v-model="actionForm.remark" type="textarea" :rows="4" placeholder="请输入处理说明" />
          </el-form-item>
          <el-form-item label="历史参考">
            <el-button link type="primary" icon="Search" :loading="historyLoading" @click="toggleHistory">
              {{ historyOpen ? '收起历史同类工单' : '参考历史同类工单' }}
            </el-button>
          </el-form-item>
          <div v-if="historyOpen" class="history-box">
            <el-empty v-if="!historyLoading && historyList.length === 0" description="没有找到相似历史工单" :image-size="60" />
            <div v-for="item in historyList" :key="item.ticketId" class="history-item">
              <div class="history-title">
                <el-tag size="small" type="info">{{ item.ticketNo }}</el-tag>
                <span class="history-name">{{ item.title }}</span>
                <span class="history-score">相关度 {{ item.relevance }}%</span>
              </div>
              <div class="history-note">{{ item.processNote }}</div>
              <el-button link type="primary" size="small" @click="quoteHistory(item)">引用到处理说明</el-button>
            </div>
          </div>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitAction">确定</el-button>
          <el-button @click="actionOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="工单详情" v-model="detailOpen" width="820px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="工单编号">{{ detail.ticketNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ formatStatus(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicant }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.handler }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="设备/系统">{{ detail.deviceInfo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="影响面">{{ detail.impactScope ? formatImpact(detail.impactScope) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="现象" :span="2">{{ detail.symptom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="已尝试" :span="2">{{ detail.triedActions || '-' }}</el-descriptions-item>
        <el-descriptions-item label="信息来源" :span="2">
          <el-tag v-if="detail.aiSource" :type="detail.aiSource === 'llm' ? 'success' : 'info'" size="small">
            {{ detail.aiSource === 'llm' ? 'AI·大模型识别' : 'AI·规则引擎识别' }}
          </el-tag>
          <span v-else>人工填写</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">AI 调用记录</el-divider>
      <el-table :data="aiLogRows" size="small">
        <el-table-column label="场景" width="110"><template #default="scope">{{ formatScene(scope.row.scene) }}</template></el-table-column>
        <el-table-column label="来源" prop="provider" width="80" />
        <el-table-column label="模型" prop="model" width="140" />
        <el-table-column label="降级" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.degraded === '1' ? 'warning' : 'success'" size="small">
              {{ scope.row.degraded === '1' ? '已降级' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" prop="costMs" width="90" />
        <el-table-column label="时间" prop="createTime" width="170" />
      </el-table>

      <el-divider content-position="left">处理记录</el-divider>
      <el-table :data="records">
        <el-table-column label="处理人" prop="processor" width="110" />
        <el-table-column label="动作" prop="action" width="100" />
        <el-table-column label="结果" prop="processResult" width="110" />
        <el-table-column label="说明" prop="processNote" />
        <el-table-column label="时间" prop="processTime" width="170" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="ItTicket">
import { listTicket, getTicket, addTicket, assignTicket, processTicket, rejectTicket, closeTicket, scanOverdue } from '@/api/it/ticket'
import { aiIntake, aiSimilar, aiLogs } from '@/api/it/ai'

const { proxy } = getCurrentInstance()

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已完成', value: 'completed' },
  { label: '已驳回', value: 'rejected' },
  { label: '已关闭', value: 'closed' }
]

const priorityOptions = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '紧急', value: 'urgent' }
]

const impactOptions = [
  { label: '仅本人', value: 'single' },
  { label: '多人/区域', value: 'multi' },
  { label: '业务中断', value: 'blocking' },
  { label: '未知', value: 'unknown' }
]

const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const ticketList = ref([])
const open = ref(false)
const detailOpen = ref(false)
const actionOpen = ref(false)
const title = ref('')
const actionTitle = ref('')
const actionType = ref('')
const actionRow = ref(null)
const detail = ref(null)
const records = ref([])
const aiLogRows = ref([])

const historyOpen = ref(false)
const historyLoading = ref(false)
const historyList = ref([])

// AI 识别状态：reviewed=false 表示还没跑过识别，点「确定」时先识别再让用户确认
const aiState = reactive({ loading: false, reviewed: false, degraded: false, notice: '', rejected: [] })
// 用户是否手动改过优先级：改过就不再被 AI 建议覆盖
const priorityTouched = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    ticketNo: undefined,
    status: undefined,
    ticketType: undefined,
    impactScope: undefined
  },
  form: {},
  actionForm: {},
  rules: {
    title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
    ticketType: [{ required: true, message: '类型不能为空', trigger: 'blur' }],
    priority: [{ required: true, message: '优先级不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, actionForm, rules } = toRefs(data)

function getList() {
  loading.value = true
  listTicket(queryParams.value).then(res => {
    ticketList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function reset() {
  form.value = {
    title: undefined,
    ticketType: undefined,
    priority: 'medium',
    description: undefined,
    deviceInfo: undefined,
    symptom: undefined,
    impactScope: undefined,
    triedActions: undefined,
    aiSource: undefined
  }
  aiState.loading = false
  aiState.reviewed = false
  aiState.degraded = false
  aiState.notice = ''
  aiState.rejected = []
  proxy.resetForm('ticketRef')
}

function cancel() {
  open.value = false
  reset()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '创建工单'
}

/**
 * 调用 AI 把自由描述解析成结构化槽位。
 * 设计要点：识别结果只是「建议」，全部可改可清空；识别失败也照样能提交（degraded 时提示已降级）。
 */
function runIntake() {
  const description = (form.value.description || '').trim()
  const titleText = (form.value.title || '').trim()
  if (!description && !titleText) {
    proxy.$modal.msgWarning('请先填写标题或问题描述')
    return Promise.resolve(false)
  }
  aiState.loading = true
  return aiIntake({ title: titleText, description: description })
    .then(res => {
      const d = res.data || {}
      form.value.deviceInfo = d.deviceInfo || undefined
      form.value.symptom = d.symptom || undefined
      form.value.impactScope = d.impactScope || undefined
      form.value.triedActions = d.triedActions || undefined
      form.value.aiSource = d.source || undefined
      // 类型只在用户没填时预填，避免覆盖人工判断
      if (!form.value.ticketType && d.suggestedType) {
        form.value.ticketType = d.suggestedType
      }
      // 优先级由后端规则（影响面）推导，用户手动改过之后就不再覆盖
      if (!priorityTouched.value && d.suggestedPriority) {
        form.value.priority = d.suggestedPriority
      }
      aiState.reviewed = true
      aiState.degraded = !!d.degraded
      aiState.notice = d.notice || ''
      aiState.rejected = d.rejected || []
      aiState.loading = false
      return true
    })
    .catch(() => {
      // 识别失败不阻塞建单：这里静默降级为手工填写
      aiState.loading = false
      proxy.$modal.msgWarning('AI 识别失败，可继续手工填写后提交')
      return false
    })
}

function submitForm() {
  // 第一步：只需要标题就能触发识别（类型/优先级此时可能还是空的，正好由识别结果补上）
  if (!form.value.title) {
    proxy.$refs.ticketRef.validateField('title')
    return
  }
  if (!aiState.reviewed) {
    runIntake().then(okRun => {
      if (okRun) {
        proxy.$modal.msgSuccess('已按描述识别工单信息，请确认后点击「确认提交」')
      } else {
        // 识别失败：退回手工填写，仍然要求必填项齐全后再提交
        proxy.$refs.ticketRef.validate(valid => { if (valid) doCreate() })
      }
    })
    return
  }
  proxy.$refs.ticketRef.validate(valid => {
    if (!valid) return
    doCreate()
  })
}

function doCreate() {
  addTicket(form.value).then(() => {
    proxy.$modal.msgSuccess('创建成功')
    open.value = false
    reset()
    getList()
  })
}

function handleDetail(row) {
  getTicket(row.ticketId).then(res => {
    detail.value = res.data
    records.value = res.records || []
    detailOpen.value = true
    aiLogs(row.ticketId).then(logRes => {
      aiLogRows.value = logRes.data || []
    }).catch(() => { aiLogRows.value = [] })
  })
}

function openAction(row, type) {
  actionRow.value = row
  actionType.value = type
  actionForm.value = {}
  historyOpen.value = false
  historyList.value = []
  actionTitle.value = {
    assign: '分配工单',
    process: '完成工单',
    reject: '驳回工单',
    close: '关闭工单'
  }[type]
  actionOpen.value = true
}

function toggleHistory() {
  if (historyOpen.value) {
    historyOpen.value = false
    return
  }
  historyOpen.value = true
  if (historyList.value.length > 0) return
  historyLoading.value = true
  aiSimilar({ ticketId: actionRow.value.ticketId, topK: 3 })
    .then(res => {
      historyList.value = res.data || []
      historyLoading.value = false
    })
    .catch(() => {
      historyLoading.value = false
      historyList.value = []
    })
}

function quoteHistory(item) {
  const current = actionForm.value.remark ? actionForm.value.remark + '\n' : ''
  actionForm.value.remark = current + '参考工单 ' + item.ticketNo + '：' + (item.processNote || '')
}

function submitAction() {
  const id = actionRow.value.ticketId
  const actions = {
    assign: () => assignTicket(id, actionForm.value),
    process: () => processTicket(id, actionForm.value),
    reject: () => rejectTicket(id, actionForm.value),
    close: () => closeTicket(id, actionForm.value)
  }
  actions[actionType.value]().then(() => {
    proxy.$modal.msgSuccess('操作成功')
    actionOpen.value = false
    getList()
  })
}

function handleScan() {
  scanOverdue().then(() => {
    proxy.$modal.msgSuccess('扫描完成')
    getList()
  })
}

function formatScene(value) {
  const map = { intake: '识别一次', intake_commit: '工单采纳' }
  return map[value] || value
}

function formatStatus(value) {
  const item = statusOptions.find(option => option.value === value)
  return item ? item.label : value
}

function formatPriority(value) {
  const item = priorityOptions.find(option => option.value === value)
  return item ? item.label : value
}

function formatImpact(value) {
  const item = impactOptions.find(option => option.value === value)
  return item ? item.label : value
}

function impactTagType(value) {
  if (value === 'blocking') return 'danger'
  if (value === 'multi') return 'warning'
  return 'info'
}

getList()
</script>

<style scoped>
.tip-line {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.history-box {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 12px;
  background: #fafafa;
  margin-bottom: 8px;
}

.history-item {
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
}

.history-item:last-child {
  border-bottom: none;
}

.history-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-name {
  font-weight: 600;
  color: #303133;
}

.history-score {
  margin-left: auto;
  color: #67c23a;
  font-size: 12px;
}

.history-note {
  color: #606266;
  font-size: 13px;
  margin: 4px 0;
}
</style>
