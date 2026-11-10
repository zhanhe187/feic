<template>
  <div class="page">
    <el-row :gutter="12" class="cards">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <div class="card">
          <div class="label">{{ card.label }}</div>
          <div class="value">{{ card.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <div slot="header">数据表概览</div>
      <el-table :data="data.tables || []" border size="mini" max-height="420">
        <el-table-column prop="name" label="表名" min-width="220" />
        <el-table-column prop="approxRows" label="约行数" width="140" align="right" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: "SysCfg",
  data() {
    return { data: {} };
  },
  computed: {
    cards() {
      return [
        { label: "角色", value: this.data.roles || 0 },
        { label: "菜单", value: this.data.menus || 0 },
        { label: "账号", value: this.data.accounts || 0 },
        { label: "员工", value: this.data.employees || 0 }
      ];
    }
  },
  mounted() {
    this.getRequest("/personnel/summary").then((resp) => {
      if (resp) {
        this.data = resp;
      }
    });
  }
};
</script>

<style scoped>
.page { padding: 8px; }
.cards { margin-bottom: 12px; }
.card { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 14px 16px; }
.card .label { color: #909399; font-size: 13px; }
.card .value { font-size: 24px; font-weight: 600; margin-top: 6px; }
</style>
