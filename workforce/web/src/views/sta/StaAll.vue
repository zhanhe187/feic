<template>
  <div class="stat-page">
    <el-row :gutter="12">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <div class="card">
          <div class="card-label">{{ card.label }}</div>
          <div class="card-value">{{ card.value }}</div>
          <div class="card-hint">{{ card.hint }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top: 16px">
      <el-col :span="12">
        <div class="panel">
          <div class="panel-title">部门人数 Top 10</div>
          <div class="bar-row" v-for="item in data.departments" :key="item.name">
            <span class="bar-name">{{ item.name }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: pct(item.cnt, maxOf(data.departments)) }"></div></div>
            <span class="bar-cnt">{{ item.cnt }}</span>
          </div>
          <el-empty v-if="!data.departments || data.departments.length === 0" description="暂无数据" :image-size="60" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="panel">
          <div class="panel-title">职称分布 Top 10</div>
          <div class="bar-row" v-for="item in data.jobLevels" :key="item.name">
            <span class="bar-name">{{ item.name }}</span>
            <div class="bar-track"><div class="bar-fill blue" :style="{ width: pct(item.cnt, maxOf(data.jobLevels)) }"></div></div>
            <span class="bar-cnt">{{ item.cnt }}</span>
          </div>
          <el-empty v-if="!data.jobLevels || data.jobLevels.length === 0" description="暂无数据" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: "StaAll",
  data() {
    return { data: {} };
  },
  computed: {
    cards() {
      return [
        { label: "员工总数", value: this.data.total || 0, hint: "在职 " + (this.data.onJob || 0) + " / 试用 " + (this.data.probation || 0) },
        { label: "本月新入职", value: this.data.newThisMonth || 0, hint: "按入职日期统计" },
        { label: "7 天内待转正", value: this.data.pendingProbation || 0, hint: "生命周期提醒候选" },
        { label: "7 天内合同到期", value: this.data.pendingContract || 0, hint: "含 30 天冷静期过滤" }
      ];
    }
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.getRequest("/statistics/overview").then((resp) => {
        if (resp) {
          this.data = resp;
        }
      });
    },
    maxOf(list) {
      if (!list || list.length === 0) return 1;
      return Math.max(...list.map((i) => i.cnt || 0), 1);
    },
    pct(value, max) {
      return Math.round(((value || 0) / max) * 100) + "%";
    }
  }
};
</script>

<style scoped>
.stat-page { padding: 4px 8px; }
.card { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 14px 16px; }
.card-label { color: #909399; font-size: 13px; }
.card-value { font-size: 28px; font-weight: 600; color: #303133; margin: 6px 0 2px; }
.card-hint { color: #c0c4cc; font-size: 12px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 16px 16px; }
.panel-title { font-weight: 600; margin-bottom: 10px; color: #303133; }
.bar-row { display: flex; align-items: center; gap: 8px; margin: 6px 0; }
.bar-name { width: 110px; font-size: 13px; color: #606266; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.bar-track { flex: 1; height: 10px; background: #f2f6fc; border-radius: 5px; overflow: hidden; }
.bar-fill { height: 100%; background: #67c23a; border-radius: 5px; }
.bar-fill.blue { background: #409eff; }
.bar-cnt { width: 40px; text-align: right; font-size: 12px; color: #909399; }
</style>
