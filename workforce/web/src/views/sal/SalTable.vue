<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.name" placeholder="按姓名搜索" clearable style="width: 200px" @clear="load" />
        <el-date-picker v-model="query.month" type="month" value-format="yyyy-MM" placeholder="按发放月份筛选"
                        style="width: 180px" />
        <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        <span class="total">共 {{ total }} 条</span>
      </div>

      <el-table :data="rows" v-loading="loading" border size="mini" show-summary
                :summary-method="summary">
        <el-table-column prop="empName" label="员工" width="110" />
        <el-table-column prop="workID" label="工号" width="110" />
        <el-table-column prop="depName" label="部门" width="140" />
        <el-table-column prop="sobName" label="工资账套" width="140" />
        <el-table-column prop="basicSalary" label="基本工资" width="110" />
        <el-table-column prop="bonus" label="奖金" width="100" />
        <el-table-column prop="lunchSalary" label="午餐补助" width="110" />
        <el-table-column prop="trafficSalary" label="交通补助" width="110" />
        <el-table-column prop="allSalary" label="应发合计" width="110" />
        <el-table-column label="发放月份" width="120">
          <template slot-scope="scope">{{ month(scope.row.createDate) }}</template>
        </el-table-column>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :total="total" :current-page.sync="query.page" :page-size="query.size"
                     @current-change="load" />
    </el-card>
  </div>
</template>

<script>
export default {
  name: "SalTable",
  data() {
    return {
      loading: false,
      rows: [],
      total: 0,
      query: { page: 1, size: 10, name: "", month: "" }
    };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.getRequest("/personnel/salary/list?page=" + this.query.page + "&size=" + this.query.size +
        "&name=" + encodeURIComponent(this.query.name || "") + "&month=" + (this.query.month || ""))
        .then((resp) => {
          this.loading = false;
          if (resp) {
            this.rows = resp.data || [];
            this.total = resp.total || 0;
          }
        })
        .catch(() => { this.loading = false; });
    },
    summary(param) {
      const sums = [];
      param.columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = "合计";
          return;
        }
        const numeric = ["basicSalary", "bonus", "lunchSalary", "trafficSalary", "allSalary"];
        if (numeric.indexOf(column.property) >= 0) {
          const total = this.rows.reduce((acc, row) => acc + (Number(row[column.property]) || 0), 0);
          sums[index] = total;
        } else {
          sums[index] = "";
        }
      });
      return sums;
    },
    month(value) {
      return value ? String(value).substring(0, 7) : "-";
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
