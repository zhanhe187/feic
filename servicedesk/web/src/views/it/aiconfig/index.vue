<template>
  <div class="app-container">
    <el-alert :type="runtime.mode === 'llm' ? 'success' : 'info'" :closable="false" show-icon style="margin-bottom: 12px">
      <template #title>
        当前模式：<b>{{ runtime.text }}</b>
        <span v-if="runtime.mode === 'llm'">，模型 {{ runtime.model }}</span>
        <span v-else>，识别结果来自关键词规则</span>
      </template>
    </el-alert>

    <el-form :model="form" label-width="140px" v-loading="loading">
      <el-form-item v-for="item in items" :key="item.key" :label="item.label">
        <div class="row">
          <el-input
            v-if="item.key === 'ai.provider'"
            v-model="form[item.key]"
            style="max-width: 240px"
            placeholder="rule / openai"
          />
          <el-switch
            v-else-if="item.key === 'ai.enabled'"
            v-model="form[item.key]"
            active-value="true"
            inactive-value="false"
            active-text="开启"
            inactive-text="关闭"
          />
          <template v-else-if="item.key === 'ai.api_key'">
            <el-input
              v-model="form[item.key]"
              style="max-width: 420px"
              type="password"
              show-password
              :placeholder="item.value === '******' ? '已配置，留空表示不修改' : '粘贴你的 API Key'"
            />
            <el-tag :type="item.value === '******' ? 'success' : 'info'" size="small">
              {{ item.value === '******' ? '已配置（' + (item.keyLength || 0) + ' 个字符）' : '未配置' }}
            </el-tag>
          </template>
          <el-input
            v-else
            v-model="form[item.key]"
            style="max-width: 420px"
            :placeholder="item.hint"
          />
          <span class="hint">{{ item.hint }}</span>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        <el-button type="success" plain :loading="testing" @click="test">测试连接</el-button>
        <el-button @click="load">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="testResult"
      :type="testResult.type"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    >
      <template #title>{{ testResult.title }}</template>
      <div class="detail">
        <div v-for="(v, k) in testResult.detail" :key="k">{{ k }}：{{ v }}</div>
        <div v-if="testResult.costMs !== undefined">耗时：{{ testResult.costMs }} ms</div>
        <div v-if="testResult.reply">模型返回：{{ testResult.reply }}</div>
      </div>
    </el-alert>

    <el-divider />
    <div class="tips">
      <p><b>判断是否接上</b>：点「测试连接」——提示「接入成功」才是真的走模型；提示「当前未启用大模型」说明提供方或 Key 没填。</p>
      <p><b>provider</b> 选 <code>openai</code> 且填了 Key 才会调用模型，其余情况走规则引擎，功能照常可用。</p>
      <p><b>降级</b>：超时、5xx、坏 JSON、额度用尽、熔断打开都会自动回落规则引擎，并在工单上标记来源。</p>
      <p><b>告警</b>：降级会向管理员推送一条站内消息，同一场景每天只推一次。</p>
    </div>
  </div>
</template>

<script setup name="ItAiConfig">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const items = ref([])
const form = reactive({})
const runtime = reactive({ mode: 'rule', text: '当前走规则引擎', model: '-' })
const testResult = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await request({ url: '/it/ai/config', method: 'get' })
    items.value = res.data || []
    Object.keys(form).forEach((k) => delete form[k])
    items.value.forEach((it) => {
      form[it.key] = it.key === 'ai.api_key' ? '' : it.value
    })
    Object.assign(runtime, res.runtime || {})
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const payload = { 'ai.enabled': form['ai.enabled'] === 'true' ? 'true' : 'false' }
    items.value.forEach((it) => {
      const v = form[it.key]
      if (it.key === 'ai.api_key') {
        // 留空表示不修改；只有真输入了才提交
        if (v !== undefined && v !== null && String(v).trim() !== '') {
          payload[it.key] = String(v).trim()
        }
        return
      }
      if (it.key === 'ai.enabled') {
        return
      }
      payload[it.key] = v === undefined || v === null ? '' : String(v).trim()
    })
    const res = await request({ url: '/it/ai/config', method: 'put', data: payload })
    Object.assign(runtime, res.runtime || {})
    ElMessage.success(`保存成功，更新 ${res.updated || 0} 项，Key 长度 ${res.keyLength || 0}，已即时生效`)
    await load()
  } finally {
    saving.value = false
  }
}

async function test() {
  testing.value = true
  testResult.value = null
  try {
    const res = await request({ url: '/it/ai/config/test', method: 'post', timeout: 30000 })
    const detail = res.detail || {}
    if (res.mode === 'llm') {
      testResult.value = {
        type: 'success',
        title: res.msg || '接入成功',
        detail,
        costMs: res.costMs,
        reply: res.reply
      }
    } else if (res.mode === 'rule') {
      testResult.value = { type: 'warning', title: res.msg || '当前未启用大模型', detail }
    } else {
      testResult.value = { type: 'error', title: res.msg || '接入失败', detail, costMs: res.costMs }
    }
  } catch (e) {
    testResult.value = { type: 'error', title: '测试请求失败，看后端日志', detail: {} }
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.detail {
  font-size: 12px;
  color: #606266;
  line-height: 1.7;
}
.tips p {
  margin: 4px 0;
  color: #606266;
  font-size: 13px;
}
code {
  background: #f2f6fc;
  padding: 1px 5px;
  border-radius: 3px;
}
</style>
