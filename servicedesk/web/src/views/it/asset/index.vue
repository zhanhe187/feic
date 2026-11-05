<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="78px">
      <el-form-item label="资产编号" prop="assetNo">
        <el-input
          v-model="queryParams.assetNo"
          placeholder="请输入资产编号"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="资产类型" prop="assetType">
        <el-select v-model="queryParams.assetType" placeholder="请选择类型" clearable style="width: 200px">
          <el-option v-for="item in assetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="资产状态" prop="assetStatus">
        <el-select v-model="queryParams.assetStatus" placeholder="请选择状态" clearable style="width: 200px">
          <el-option v-for="item in assetStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['it:asset:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['it:asset:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['it:asset:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['it:asset:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-upload :headers="upload.headers" :action="upload.url" :show-file-list="false" :on-success="handleImportSuccess">
          <el-button type="info" plain icon="Upload" v-hasPermi="['it:asset:import']">导入</el-button>
        </el-upload>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="assetList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="资产编号" align="center" prop="assetNo" />
      <el-table-column label="类型" align="center" prop="assetType">
        <template #default="scope">{{ formatAssetType(scope.row.assetType) }}</template>
      </el-table-column>
      <el-table-column label="品牌型号" align="center" prop="brandModel" />
      <el-table-column label="使用人" align="center" prop="ownerUser" />
      <el-table-column label="状态" align="center" prop="assetStatus">
        <template #default="scope">
          <el-tag>{{ formatAssetStatus(scope.row.assetStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="领用时间" align="center" prop="receiveTime" width="120" />
      <el-table-column label="归还时间" align="center" prop="returnTime" width="120" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['it:asset:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['it:asset:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="assetRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="资产编号" prop="assetNo">
          <el-input v-model="form.assetNo" placeholder="请输入资产编号" />
        </el-form-item>
        <el-form-item label="资产类型" prop="assetType">
          <el-select v-model="form.assetType" placeholder="请选择资产类型" style="width: 100%">
            <el-option v-for="item in assetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌型号" prop="brandModel">
          <el-input v-model="form.brandModel" placeholder="请输入品牌型号" />
        </el-form-item>
        <el-form-item label="使用人" prop="ownerUser">
          <el-input v-model="form.ownerUser" placeholder="请输入使用人" />
        </el-form-item>
        <el-form-item label="资产状态" prop="assetStatus">
          <el-select v-model="form.assetStatus" placeholder="请选择资产状态" style="width: 100%">
            <el-option v-for="item in assetStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="领用时间" prop="receiveTime">
          <el-date-picker clearable v-model="form.receiveTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择领用时间" />
        </el-form-item>
        <el-form-item label="归还时间" prop="returnTime">
          <el-date-picker clearable v-model="form.returnTime" type="date" value-format="YYYY-MM-DD" placeholder="请选择归还时间" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ItAsset">
import { getToken } from '@/utils/auth'
import { listAsset, getAsset, delAsset, addAsset, updateAsset } from '@/api/it/asset'

const { proxy } = getCurrentInstance()

const assetTypeOptions = [
  { label: '电脑', value: 'computer' },
  { label: '显示器', value: 'monitor' },
  { label: '外设', value: 'peripheral' }
]

const assetStatusOptions = [
  { label: '闲置', value: 'idle' },
  { label: '使用中', value: 'in_use' },
  { label: '维修中', value: 'repairing' },
  { label: '报废', value: 'scrapped' }
]

const loading = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const showSearch = ref(true)
const total = ref(0)
const assetList = ref([])
const title = ref('')
const open = ref(false)

const upload = reactive({
  headers: { Authorization: 'Bearer ' + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + '/it/asset/importData'
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    assetNo: undefined,
    assetType: undefined,
    assetStatus: undefined
  },
  form: {},
  rules: {
    assetNo: [{ required: true, message: '资产编号不能为空', trigger: 'blur' }],
    assetType: [{ required: true, message: '资产类型不能为空', trigger: 'change' }],
    brandModel: [{ required: true, message: '品牌型号不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listAsset(queryParams.value).then(res => {
    assetList.value = res.rows
    total.value = res.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    assetId: undefined,
    assetNo: undefined,
    assetType: undefined,
    brandModel: undefined,
    ownerUser: undefined,
    assetStatus: 'idle',
    receiveTime: undefined,
    returnTime: undefined,
    remark: undefined
  }
  proxy.resetForm('assetRef')
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.assetId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增资产'
}

function handleUpdate(row) {
  reset()
  const assetId = row.assetId || ids.value[0]
  getAsset(assetId).then(res => {
    form.value = res.data
    open.value = true
    title.value = '修改资产'
  })
}

function submitForm() {
  proxy.$refs.assetRef.validate(valid => {
    if (!valid) return
    const request = form.value.assetId ? updateAsset(form.value) : addAsset(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.assetId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const assetIds = row.assetId || ids.value
  proxy.$modal.confirm('确认删除资产编号为 "' + assetIds + '" 的数据项？').then(() => {
    return delAsset(assetIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

function handleExport() {
  proxy.download('it/asset/export', {
    ...queryParams.value
  }, 'it_asset.xlsx')
}

function handleImportSuccess() {
  proxy.$modal.msgSuccess('导入成功')
  getList()
}

function formatAssetType(value) {
  const item = assetTypeOptions.find(option => option.value === value)
  return item ? item.label : value
}

function formatAssetStatus(value) {
  const item = assetStatusOptions.find(option => option.value === value)
  return item ? item.label : value
}

getList()
</script>
