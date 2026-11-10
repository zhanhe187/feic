<template>
  <div class="stat-page">
    <div class="panel">
      <div class="panel-title">
        员工积分排行（按奖惩累计得分）
        <span class="avg">参与评分员工平均分：{{ data.avg || 0 }}</span>
      </div>
      <el-table :data="data.rows" size="small" style="width: 100%">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="workID" label="工号" width="120" />
        <el-table-column prop="times" label="奖惩次数" width="100" />
        <el-table-column label="累计积分" min-width="220">
          <template slot-scope="scope">
            <div class="bar-row">
              <div class="bar-track">
                <div class="bar-fill" :class="{ negative: scope.row.point < 0 }" :style="{ width: pct(scope.row.point) }"></div>
              </div>
              <span class="bar-cnt">{{ scope.row.point }}</span>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!data.rows || data.rows.length === 0" description="暂无奖惩记录" :image-size="70" />
    </div>
  </div>
</template>

<script>
export default {
  name: "StaScore",
  data() {
    return { data: { rows: [], avg: 0 } };
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.getRequest("/statistics/score").then((resp) => {
        if (resp) {
          this.data = resp;
        }
      });
    },
    pct(point) {
      const max = Math.max(...(this.data.rows || []).map((r) => Math.abs(r.point || 0)), 1);
      return Math.round((Math.abs(point || 0) / max) * 100) + "%";
    }
  }
};
</script>

<style scoped>
.stat-page { padding: 4px 8px; }
.panel { background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px 16px 16px; }
.panel-title { font-weight: 600; margin-bottom: 10px; color: #303133; }
.avg { font-weight: 400; color: #909399; font-size: 12px; margin-left: 10px; }
.bar-row { display: flex; align-items: center; gap: 8px; }
.bar-track { flex: 1; height: 10px; background: #f2f6fc; border-radius: 5px; overflow: hidden; }
.bar-fill { height: 100%; background: #67c23a; border-radius: 5px; }
.bar-fill.negative { background: #f56c6c; }
.bar-cnt { width: 44px; text-align: right; font-size: 12px; color: #909399; }
</style>
