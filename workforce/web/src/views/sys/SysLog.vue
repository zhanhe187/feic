<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <span class="hint">日志来源：生命周期动作表（转正 / 续签 / 不续签的完整留痕）</span>
        <el-button icon="el-icon-refresh" size="mini" @click="load">刷新</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border size="mini" style="width: 100%">
        <el-table-column prop="empName" label="员工" width="120" />
        <el-table-column label="动作" width="140">
          <template slot-scope="scope">
            <el-tag size="mini" :type="tagType(scope.row.actionType)">{{ label(scope.row.actionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更前到期日" width="140">
          <template slot-scope="scope">{{ date(scope.row.beforeEnd) }}</template>
        </el-table-column>
        <el-table-column label="变更后到期日" width="140">
          <template slot-scope="scope">{{ date(scope.row.afterEnd) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="130" />
        <el-table-column label="时间" width="170">
          <template slot-scope="scope">{{ time(scope.row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无动作记录" :image-size="70" />
    </el-card>
  </div>
</template>

<script>
export default {
  name: "SysLog",
  data() {
    return { loading: false, rows: [] };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      this.getRequest("/personnel/audit/list?limit=100")
        .then((resp) => {
          this.loading = false;
          this.rows = (resp && resp.data) || [];
        })
        .catch(() => { this.loading = false; });
    },
    label(type) {
      const map = { REGULARIZE: "转正", RENEW: "续签", NOT_RENEW: "不续签" };
      return map[type] || type;
    },
    tagType(type) {
      if (type === "NOT_RENEW") return "danger";
      if (type === "RENEW") return "warning";
      return "success";
    },
    date(value) {
      return value ? String(value).substring(0, 10) : "-";
    },
    time(value) {
      return value ? String(value).substring(0, 19).replace("T", " ") : "-";
    }
  }
};
</script>

<style scoped>
.page { padding: 8px; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.hint { color: #909399; font-size: 12px; margin-right: auto; }
</style>
