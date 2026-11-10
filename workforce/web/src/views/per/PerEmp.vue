<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.name" placeholder="按姓名搜索" clearable style="width: 220px"
                  @keyup.enter.native="load" @clear="load" />
        <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        <span class="total">共 {{ total }} 人</span>
      </div>

      <el-table :data="rows" v-loading="loading" border size="mini">
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="workID" label="工号" width="110" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="departmentName" label="部门" width="140" />
        <el-table-column prop="positionName" label="职位" width="130" />
        <el-table-column prop="jobLevelName" label="职称" width="130" />
        <el-table-column prop="workState" label="在职状态" width="100" />
        <el-table-column label="入职日期" width="120">
          <template slot-scope="scope">{{ date(scope.row.beginDate) }}</template>
        </el-table-column>
        <el-table-column label="转正日期" width="120">
          <template slot-scope="scope">{{ date(scope.row.conversionTime) }}</template>
        </el-table-column>
        <el-table-column label="合同到期" width="120">
          <template slot-scope="scope">{{ date(scope.row.endContract) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" min-width="130" />
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="detail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :total="total" :current-page.sync="query.page" :page-size="query.size"
                     @current-change="load" />
    </el-card>

    <el-dialog title="员工档案" :visible.sync="dialogVisible" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">{{ current.name }}</el-descriptions-item>
        <el-descriptions-item label="工号">{{ current.workID }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ current.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ current.positionName }}</el-descriptions-item>
        <el-descriptions-item label="职称">{{ current.jobLevelName }}</el-descriptions-item>
        <el-descriptions-item label="在职状态">{{ current.workState }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">{{ date(current.beginDate) }}</el-descriptions-item>
        <el-descriptions-item label="转正日期">{{ date(current.conversionTime) }}</el-descriptions-item>
        <el-descriptions-item label="合同起始">{{ date(current.beginContract) }}</el-descriptions-item>
        <el-descriptions-item label="合同到期">{{ date(current.endContract) }}</el-descriptions-item>
        <el-descriptions-item label="合同年限">{{ current.contractTerm }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ current.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ current.email }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ current.address }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "PerEmp",
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      query: { page: 1, size: 10, name: "" },
      dialogVisible: false,
      current: {}
    };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.getRequest("/employee/basic/?page=" + this.query.page + "&size=" + this.query.size +
        "&name=" + encodeURIComponent(this.query.name || ""))
        .then((resp) => {
          this.loading = false;
          if (resp) {
            this.rows = resp.data || [];
            this.total = resp.total || 0;
          }
        })
        .catch(() => { this.loading = false; });
    },
    detail(row) {
      this.current = row;
      this.dialogVisible = true;
    },
    date(value) {
      return value ? String(value).substring(0, 10) : "-";
    }
  }
};
</script>

<style scoped>
.page { padding: 8px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.total { margin-left: auto; color: #909399; font-size: 12px; }
.pager { margin-top: 12px; text-align: right; }
</style>
