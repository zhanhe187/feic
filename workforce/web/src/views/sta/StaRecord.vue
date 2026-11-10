<template>
  <div class="stat-page">
    <el-tabs v-model="active">
      <el-tab-pane label="考评记录" name="appraise" />
      <el-tab-pane label="奖惩记录" name="ec" />
      <el-tab-pane label="调薪记录" name="adjust" />
      <el-tab-pane label="调动记录" name="remove" />
      <el-tab-pane label="培训记录" name="train" />
    </el-tabs>

    <div class="panel">
      <el-table :data="data[active] || []" size="small" style="width: 100%">
        <el-table-column prop="name" label="员工" width="140" />
        <el-table-column prop="happenDate" label="发生日期" width="180" />
        <el-table-column prop="title" label="事项" min-width="240" show-overflow-tooltip />
        <el-table-column prop="detail" label="说明" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!data[active] || data[active].length === 0" description="该分类暂无记录" :image-size="70" />
    </div>
  </div>
</template>

<script>
export default {
  name: "StaRecord",
  data() {
    return { active: "appraise", data: {} };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.getRequest("/statistics/records").then((resp) => {
        if (resp) {
          this.data = resp;
        }
      });
    }
  }
};
</script>

<style scoped>
.stat-page { padding: 4px 8px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 16px 16px; }
</style>
