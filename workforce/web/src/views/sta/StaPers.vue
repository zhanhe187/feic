<template>
  <div class="stat-page">
    <el-row :gutter="12">
      <el-col :span="12" v-for="block in blocks" :key="block.title">
        <div class="panel">
          <div class="panel-title">{{ block.title }}</div>
          <el-table :data="block.rows" size="small" style="width: 100%">
            <el-table-column prop="name" :label="block.label" />
            <el-table-column prop="cnt" label="人数" width="90" align="right" />
            <el-table-column label="占比" width="90" align="right">
              <template slot-scope="scope">{{ ratio(scope.row.cnt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!block.rows || block.rows.length === 0" description="暂无数据" :image-size="50" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "StaPers",
  data() {
    return { data: { departments: [], jobLevels: [], politics: [], degrees: [] } };
  },
  computed: {
    total() {
      return (this.data.departments || []).reduce((sum, item) => sum + (item.cnt || 0), 0);
    },
    blocks() {
      return [
        { title: "按部门分布", label: "部门", rows: this.data.departments },
        { title: "按职称分布", label: "职称", rows: this.data.jobLevels },
        { title: "按政治面貌分布", label: "政治面貌", rows: this.data.politics },
        { title: "按学历分布", label: "学历", rows: this.data.degrees }
      ];
    }
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.getRequest("/statistics/personnel").then((resp) => {
        if (resp) {
          this.data = resp;
        }
      });
    },
    ratio(cnt) {
      if (!this.total) return "0%";
      return Math.round(((cnt || 0) / this.total) * 100) + "%";
    }
  }
};
</script>

<style scoped>
.stat-page { padding: 4px 8px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 16px 16px; margin-bottom: 12px; }
.panel-title { font-weight: 600; margin-bottom: 10px; color: #303133; }
</style>
