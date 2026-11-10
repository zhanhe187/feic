<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker v-model="month" type="month" value-format="yyyy-MM" placeholder="选择月份"
                        style="width: 180px" @change="load" />
        <span class="hint">统计该月发生的调薪记录，用于月末核对薪资变动</span>
      </div>

      <el-row :gutter="12" class="cards">
        <el-col :span="8">
          <div class="card"><div class="label">调薪人次</div><div class="value">{{ (data.rows || []).length }}</div></div>
        </el-col>
        <el-col :span="8">
          <div class="card"><div class="label">合计增资</div><div class="value">{{ data.total || 0 }}</div></div>
        </el-col>
        <el-col :span="8">
          <div class="card"><div class="label">统计月份</div><div class="value">{{ data.month }}</div></div>
        </el-col>
      </el-row>

      <el-table :data="data.rows" v-loading="loading" border size="mini" style="width: 100%">
        <el-table-column prop="empName" label="员工" width="120" />
        <el-table-column label="调薪日期" width="130">
          <template slot-scope="scope">{{ date(scope.row.asDate) }}</template>
        </el-table-column>
        <el-table-column prop="beforeSalary" label="调整前" width="120" />
        <el-table-column prop="afterSalary" label="调整后" width="120" />
        <el-table-column label="增减" width="100">
          <template slot-scope="scope">
            <span :style="{ color: scope.row.delta >= 0 ? '#67c23a' : '#f56c6c' }">{{ scope.row.delta }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && (!data.rows || data.rows.length === 0)" description="该月没有调薪记录" :image-size="70" />
    </el-card>
  </div>
</template>

<script>
export default {
  name: "SalMonth",
  data() {
    const now = new Date();
    const m = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0");
    return { month: m, loading: false, data: { rows: [], total: 0, month: m } };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.getRequest("/personnel/monthAdjust?month=" + (this.month || ""))
        .then((resp) => {
          this.loading = false;
          if (resp) {
            this.data = resp;
          }
        })
        .catch(() => { this.loading = false; });
    },
    date(value) {
      return value ? String(value).substring(0, 10) : "-";
    }
  }
};
</script>

<style scoped>
.page { padding: 8px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.hint { color: #909399; font-size: 12px; }
.cards { margin-bottom: 12px; }
.card { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 14px 16px; }
.card .label { color: #909399; font-size: 13px; }
.card .value { font-size: 24px; font-weight: 600; margin-top: 6px; }
</style>
